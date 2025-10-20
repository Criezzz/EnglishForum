package com.example.englishforum.data.home.remote

import com.example.englishforum.core.common.resolveVoteChange
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostSummary
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.bearerToken
import com.example.englishforum.data.home.HomeRepository
import com.example.englishforum.data.home.remote.model.AttachmentResponse
import com.example.englishforum.data.home.remote.model.FeedPostResponse
import com.example.englishforum.data.post.ForumPostSummaryStore
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

class RemoteHomeRepository(
    private val postsApi: PostsApi,
    private val userSessionRepository: UserSessionRepository,
    private val postStore: ForumPostSummaryStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HomeRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var lastSessionUserId: String? = null

    override val postsStream = postStore.postsStream

    init {
        scope.launch { observeSessionChanges() }
    }

    override suspend fun refresh(): Result<Unit> {
        val session = currentSessionOrNull()
            ?: return Result.failure(IllegalStateException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."))
        return refreshFeed(session)
    }

    override suspend fun setVoteState(postId: String, target: VoteState): Result<Unit> {
        val session = currentSessionOrNull()
            ?: return Result.failure(IllegalStateException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."))

        val numericId = postId.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Định danh bài viết không hợp lệ"))

        val snapshot = postStore.currentPosts
        val currentPost = snapshot.firstOrNull { it.id == postId }
            ?: return Result.failure(IllegalArgumentException("Bài viết không còn khả dụng"))

        val (nextState, delta) = resolveVoteChange(currentPost.voteState, target)
        val voteValue = nextState.toVoteValue()

        val voteResult = withContext(ioDispatcher) {
            runCatching {
                postsApi.votePost(
                    bearer = session.bearerToken(),
                    postId = numericId,
                    voteType = voteValue
                )
            }
        }

        return voteResult
            .map {
                postStore.updatePost(postId) { post ->
                    post.copy(
                        voteState = nextState,
                        voteCount = post.voteCount + delta
                    )
                }
                Unit
            }
            .mapFailure { it.toFeedException() }
    }

    private suspend fun observeSessionChanges() {
        userSessionRepository.sessionFlow.collectLatest { session ->
            if (session == null) {
                lastSessionUserId = null
                postStore.clear()
            } else if (session.userId != lastSessionUserId || postStore.currentPosts.isEmpty()) {
                lastSessionUserId = session.userId
                refreshFeed(session).getOrElse { }
            }
        }
    }

    private suspend fun refreshFeed(session: UserSession): Result<Unit> {
        val feedResult = withContext(ioDispatcher) {
            runCatching {
                postsApi.getFeed(
                    bearer = session.bearerToken(),
                    criteria = DEFAULT_FEED_CRITERIA,
                    limit = DEFAULT_FEED_LIMIT
                )
            }
        }

        return feedResult
            .map { response ->
                postStore.replaceAll(response.map { it.toDomain() })
                Unit
            }
            .mapFailure { throwable ->
                if (postStore.currentPosts.isEmpty()) {
                    postStore.clear()
                }
                throwable.toFeedException()
            }
    }

    private suspend fun currentSessionOrNull(): UserSession? {
        return userSessionRepository.sessionFlow.firstOrNull()
    }

    private fun FeedPostResponse.toDomain(): ForumPostSummary {
        val minutesAgo = createdAt.toMinutesAgo()
        return ForumPostSummary(
            id = postId.toString(),
            authorName = resolveAuthorName(),
            minutesAgo = minutesAgo,
            title = title,
            body = content,
            voteCount = voteCount,
            voteState = userVote.toVoteState(),
            commentCount = commentCount ?: 0,
            tag = tag.toPostTag(),
            authorAvatarUrl = authorAvatarUrl?.ifBlank { null },
            previewImageUrl = attachments.resolvePreviewUrl()
        )
    }

    private fun FeedPostResponse.resolveAuthorName(): String {
        return listOfNotNull(
            authorDisplayName,
            authorUsername,
            authorName
        ).firstOrNull { it.isNotBlank() } ?: ANONYMOUS_AUTHOR
    }

    private fun List<AttachmentResponse>?.resolvePreviewUrl(): String? {
        if (this.isNullOrEmpty()) return null
        return this
            .sortedWith(compareBy<AttachmentResponse> { it.index ?: Int.MAX_VALUE })
            .firstNotNullOfOrNull { attachment ->
                attachment.mediaUrl?.takeIf { it.isNotBlank() }
            }
    }

    private fun VoteState.toVoteValue(): Int = when (this) {
        VoteState.NONE -> 0
        VoteState.UPVOTED -> 1
        VoteState.DOWNVOTED -> -1
    }

    private fun String?.toPostTag(): PostTag {
        return when (this?.lowercase(Locale.ROOT)) {
            "tutorial" -> PostTag.Tutorial
            "question", "ask", "ask_question" -> PostTag.AskQuestion
            "resource" -> PostTag.Resource
            "experience", "discussion" -> PostTag.Experience
            else -> PostTag.Experience
        }
    }

    private fun Int?.toVoteState(): VoteState = when (this) {
        1 -> VoteState.UPVOTED
        -1 -> VoteState.DOWNVOTED
        else -> VoteState.NONE
    }

    private fun String?.toMinutesAgo(): Int {
        if (this.isNullOrBlank()) return 0
        val now = Instant.now()
        val createdInstant = this.parseInstant() ?: now
        val minutes = Duration.between(createdInstant, now).toMinutes()
        return minutes.coerceAtLeast(0).toInt()
    }

    private fun String.parseInstant(): Instant? {
        return runCatching { Instant.parse(this) }
            .recoverCatching { OffsetDateTime.parse(this).toInstant() }
            .recoverCatching { LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC) }
            .getOrNull()
    }

    private fun Throwable.toFeedException(): Throwable = when (this) {
        is HttpException -> {
            val message = parseErrorMessage(response()?.errorBody()?.string())
            IllegalStateException(message, this)
        }
        is IOException -> IOException("Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại mạng.", this)
        else -> this
    }

    private fun parseErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Đã xảy ra lỗi, vui lòng thử lại"
        return try {
            val json = JSONObject(raw)
            json.optString("detail", json.optString("message", raw))
        } catch (_: Exception) {
            raw
        }
    }

    private fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable -> Result.failure(transform(throwable)) }
        )
    }

    companion object {
        private const val DEFAULT_FEED_LIMIT = 30
        private const val DEFAULT_FEED_CRITERIA = "latest"
        private const val ANONYMOUS_AUTHOR = "Ẩn danh"
    }
}
