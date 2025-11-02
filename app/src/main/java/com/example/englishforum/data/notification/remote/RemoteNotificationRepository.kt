package com.example.englishforum.data.notification.remote

import com.example.englishforum.BuildConfig
import com.example.englishforum.core.model.notification.ForumNotification
import com.example.englishforum.core.model.notification.ForumNotificationTarget
import com.example.englishforum.core.network.sse.SseClient
import com.example.englishforum.core.network.sse.SseConnection
import com.example.englishforum.core.network.sse.SseListener
import com.example.englishforum.core.network.sse.SseMessage
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.bearerToken
import com.example.englishforum.data.post.remote.PostDetailApi
import com.example.englishforum.data.notification.NotificationRealtimeEvent
import com.example.englishforum.data.notification.NotificationRepository
import com.example.englishforum.data.notification.remote.model.NotificationResponse
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class RemoteNotificationRepository(
    private val notificationApi: NotificationApi,
    private val postDetailApi: PostDetailApi,
    private val userSessionRepository: UserSessionRepository,
    private val sseClient: SseClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NotificationRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutableNotifications = MutableStateFlow<List<ForumNotification>>(emptyList())
    override val notificationsStream: Flow<List<ForumNotification>> = mutableNotifications.asStateFlow()

    private val realtimeEventsFlow = MutableSharedFlow<NotificationRealtimeEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val realtimeEvents: Flow<NotificationRealtimeEvent> = realtimeEventsFlow.asSharedFlow()

    private var lastSessionUserId: String? = null
    private var lastSessionAccessToken: String? = null
    private var activeSession: UserSession? = null
    private var notificationConnection: SseConnection? = null
    private val reconnectScheduled = AtomicBoolean(false)
    private val refreshMutex = Mutex()

    init {
        scope.launch { observeSessionChanges() }
    }

    private suspend fun fetchPostIdForComment(session: UserSession, commentId: Int): String? {
        val response = withContext(ioDispatcher) {
            runCatching {
                postDetailApi.getCommentDetail(
                    bearer = session.bearerToken(),
                    commentId = commentId
                )
            }
        }

        val postId = response.getOrNull()?.postId
        return postId?.takeIf { it > 0 }?.toString()
    }

    override suspend fun markNotificationAsRead(notificationId: String) {
        val targetId = notificationId.toIntOrNull() ?: return
        val session = currentSessionOrNull() ?: return

        val result = withContext(ioDispatcher) {
            runCatching {
                notificationApi.markAsRead(
                    bearer = session.bearerToken(),
                    notificationId = targetId
                )
            }
        }

        if (result.isSuccess) {
            mutableNotifications.update { current ->
                current.map { notification ->
                    if (notification.id == notificationId && !notification.isRead) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }
            }
        }
    }

    override suspend fun markAllAsRead() {
        val session = currentSessionOrNull() ?: return
        val unreadNotifications = mutableNotifications.value.filterNot { it.isRead }
        if (unreadNotifications.isEmpty()) return

        withContext(ioDispatcher) {
            unreadNotifications.forEach { notification ->
                val id = notification.id.toIntOrNull() ?: return@forEach
                val result = runCatching {
                    notificationApi.markAsRead(
                        bearer = session.bearerToken(),
                        notificationId = id
                    )
                }
                if (result.isSuccess) {
                    mutableNotifications.update { current ->
                        current.map { item ->
                            if (item.id == notification.id && !item.isRead) {
                                item.copy(isRead = true)
                            } else {
                                item
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun refresh(): Result<Unit> {
        val session = currentSessionOrNull()
            ?: return Result.failure(IllegalStateException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."))
        
        return runCatching {
            fetchNotifications(session, UpdateReason.ManualRefresh)
        }
    }

    private suspend fun observeSessionChanges() {
        userSessionRepository.sessionFlow.collectLatest { session ->
            if (session == null) {
                activeSession = null
                lastSessionUserId = null
                lastSessionAccessToken = null
                mutableNotifications.value = emptyList()
                stopNotificationStream()
            } else {
                val isNewUser = session.userId != lastSessionUserId
                val tokenChanged = session.accessToken != lastSessionAccessToken
                activeSession = session

                if (isNewUser || tokenChanged || mutableNotifications.value.isEmpty()) {
                    fetchNotifications(session, UpdateReason.Initial)
                }

                lastSessionUserId = session.userId
                lastSessionAccessToken = session.accessToken

                if (notificationConnection == null || isNewUser || tokenChanged) {
                    restartNotificationStream(session)
                }
            }
        }
    }

    private suspend fun fetchNotifications(
        session: UserSession,
        reason: UpdateReason
    ) {
        val response = withContext(ioDispatcher) {
            runCatching {
                notificationApi.getNotifications(bearer = session.bearerToken())
            }
        }

        if (response.isFailure) return

        val list = response.getOrNull().orEmpty()
        val now = Instant.now()
        val normalized = mutableListOf<ForumNotification>()
        for (item in list) {
            val normalizedItem = item.toDomain(now, session)
            if (normalizedItem != null) {
                normalized.add(normalizedItem)
            }
        }

        refreshMutex.withLock {
            val previous = mutableNotifications.value
            mutableNotifications.value = normalized
            if (reason == UpdateReason.Realtime) {
                val previousIds = previous.mapTo(mutableSetOf()) { it.id }
                val newIds = normalized.mapNotNull { notification ->
                    notification.id.takeIf { id -> id !in previousIds }
                }
                if (newIds.isNotEmpty()) {
                    realtimeEventsFlow.tryEmit(NotificationRealtimeEvent.NewNotifications(newIds))
                }
            }
        }
    }

    private fun restartNotificationStream(session: UserSession) {
        stopNotificationStream()
        startNotificationStream(session)
    }

    private fun startNotificationStream(session: UserSession) {
        val headers = mapOf("Authorization" to session.bearerToken())
        val result = runCatching {
            sseClient.open(
                path = "sse/notifications",
                headers = headers,
                listener = object : SseListener {
                    override fun onMessage(message: SseMessage) {
                        scope.launch {
                            handleRealtimeNotificationEvent()
                        }
                    }

                    override fun onClosed() {
                        scheduleNotificationReconnect()
                    }

                    override fun onFailure(cause: Throwable) {
                        scheduleNotificationReconnect()
                    }
                }
            )
        }

        notificationConnection = result.getOrElse {
            scheduleNotificationReconnect()
            null
        }
    }

    private fun stopNotificationStream() {
        notificationConnection?.close()
        notificationConnection = null
    }

    private fun scheduleNotificationReconnect(delayMillis: Long = 2_000) {
        if (!reconnectScheduled.compareAndSet(false, true)) return
        scope.launch {
            delay(delayMillis)
            reconnectScheduled.set(false)
            val session = activeSession ?: return@launch
            restartNotificationStream(session)
        }
    }

    private suspend fun handleRealtimeNotificationEvent() {
        val session = activeSession ?: return
        fetchNotifications(session, UpdateReason.Realtime)
    }

    private enum class UpdateReason {
        Initial,
        ManualRefresh,
        Realtime
    }

    private suspend fun currentSessionOrNull(): UserSession? {
        return userSessionRepository.sessionFlow.firstOrNull()
    }

    private suspend fun NotificationResponse.toDomain(now: Instant, session: UserSession): ForumNotification? {
        val id = notificationId.toString()
        val action = actionType?.lowercase(Locale.US) ?: "unknown"
        val target = when (targetType?.lowercase(Locale.US)) {
            "comment" -> {
                val commentId = (targetId ?: actionId)?.takeIf { it > 0 }
                if (commentId != null) {
                    val initialPostId = actionId?.takeIf { it > 0 && it != commentId }?.toString()
                    val resolvedPostId = initialPostId ?: fetchPostIdForComment(session, commentId)
                    ForumNotificationTarget.Comment(
                        commentId = commentId.toString(),
                        postId = resolvedPostId
                    )
                } else {
                    ForumNotificationTarget.Unknown(targetType, targetId?.toString(), actionId?.toString())
                }
            }

            "post" -> {
                val postId = targetId?.takeIf { it > 0 } ?: actionId
                if (postId != null && postId > 0) {
                    ForumNotificationTarget.Post(postId.toString())
                } else {
                    ForumNotificationTarget.Unknown(targetType, targetId?.toString(), actionId?.toString())
                }
            }

            else -> ForumNotificationTarget.Unknown(targetType, targetId?.toString(), actionId?.toString())
        }

        val (title, description) = buildCopy(actorUsername, action)
        val minutesAgo = createdAt.toMinutesAgo(now)

        return ForumNotification(
            id = id,
            actorName = resolveActorName(actorUsername),
            actorAvatarUrl = resolveAvatarUrl(actorAvatar),
            title = title,
            description = description,
            minutesAgo = minutesAgo,
            target = target,
            isRead = isRead ?: false
        )
    }

    private fun resolveActorName(raw: String?): String {
        return raw?.takeIf { it.isNotBlank() } ?: DEFAULT_ACTOR_NAME
    }

    private fun buildCopy(actor: String?, action: String): Pair<String, String?> {
        val displayName = resolveActorName(actor)
        return when (action) {
            "comment" -> displayName + COMMENT_HEADLINE_SUFFIX to null
            "reply" -> displayName + REPLY_HEADLINE_SUFFIX to null
            "post" -> displayName + POST_HEADLINE_SUFFIX to null
            "vote_post" -> displayName + VOTE_POST_HEADLINE_SUFFIX to null
            "vote_comment" -> displayName + VOTE_COMMENT_HEADLINE_SUFFIX to null
            else -> displayName + GENERIC_HEADLINE_SUFFIX to null
        }
    }

    private fun String?.toMinutesAgo(now: Instant): Int {
        if (this.isNullOrBlank()) return 0
        return try {
            val timestamp = OffsetDateTime.parse(this).toInstant()
            val delta = Duration.between(timestamp, now)
            val safeDelta = if (delta.isNegative) Duration.ZERO else delta
            safeDelta.toMinutes().coerceAtLeast(0L).toInt()
        } catch (error: DateTimeParseException) {
            0
        }
    }

    private fun resolveAvatarUrl(raw: String?): String? {
        val candidate = raw?.takeIf { it.isNotBlank() } ?: return null
        if (candidate.startsWith("http://", ignoreCase = true) || candidate.startsWith("https://", ignoreCase = true)) {
            return candidate
        }
        val normalizedBase = BuildConfig.API_BASE_URL.trimEnd('/')
        return when {
            candidate.startsWith("/download/") -> "$normalizedBase$candidate"
            candidate.startsWith("download/") -> "$normalizedBase/$candidate"
            candidate.startsWith("/") -> "$normalizedBase/download$candidate"
            else -> "$normalizedBase/download/$candidate"
        }
    }

    private companion object {
        private const val DEFAULT_ACTOR_NAME = "Thành viên diễn đàn"

        private const val COMMENT_HEADLINE_SUFFIX = " đã bình luận bài viết của bạn"

        private const val REPLY_HEADLINE_SUFFIX = " đã trả lời bình luận của bạn"

        private const val POST_HEADLINE_SUFFIX = " vừa đăng bài mới"

        private const val VOTE_POST_HEADLINE_SUFFIX = " đã bày tỏ cảm xúc với bài viết của bạn"

        private const val VOTE_COMMENT_HEADLINE_SUFFIX = " đã bày tỏ cảm xúc với bình luận của bạn"

        private const val GENERIC_HEADLINE_SUFFIX = " đã tương tác cùng bạn"
    }
}
