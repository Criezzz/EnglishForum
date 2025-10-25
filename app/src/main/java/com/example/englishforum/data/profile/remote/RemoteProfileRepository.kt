package com.example.englishforum.data.profile.remote

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.englishforum.BuildConfig
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumProfilePost
import com.example.englishforum.core.model.forum.ForumProfileReply
import com.example.englishforum.core.model.forum.ForumProfileStats
import com.example.englishforum.core.model.forum.ForumUserProfile
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.bearerToken
import com.example.englishforum.data.post.remote.PostDetailApi
import com.example.englishforum.data.profile.ProfileAvatarImage
import com.example.englishforum.data.profile.ProfileRepository
import com.example.englishforum.data.profile.remote.model.SimpleUserResponse
import com.example.englishforum.data.profile.remote.model.UserCommentResponse
import com.example.englishforum.data.profile.remote.model.UserPostResponse
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.util.concurrent.ConcurrentHashMap

internal class RemoteProfileRepository(
    private val profileApi: ProfileApi,
    private val userSessionRepository: UserSessionRepository,
    private val contentResolver: ContentResolver,
    private val postDetailApi: PostDetailApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProfileRepository {

    private val profileState = MutableStateFlow<ForumUserProfile?>(null)
    private var cachedUserId: String? = null
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
    private val postTitleCache = ConcurrentHashMap<Int, String>()

    override fun observeProfile(userId: String): Flow<ForumUserProfile> = flow {
        refreshProfile(userId, force = cachedUserId != userId)
        emitAll(
            profileState
                .filterNotNull()
                .distinctUntilChanged()
        )
    }

    override suspend fun updateDisplayName(userId: String, displayName: String): Result<Unit> {
        val sanitized = displayName.trim()
        if (sanitized.isBlank()) {
            return Result.failure(ProfileRepositoryException("Display name cannot be empty"))
        }

        val session = currentSessionOrNull()
            ?: return Result.failure(ProfileRepositoryException("Session expired. Please sign in again."))

        return runCatching {
            withContext(ioDispatcher) {
                profileApi.updateUsername(session.bearerToken(), sanitized)
            }
            userSessionRepository.saveSession(session.copy(username = sanitized))
            profileState.update { current ->
                if (current != null && current.userId == session.userId) {
                    current.copy(displayName = sanitized)
                } else {
                    current
                }
            }
            refreshProfile(session.userId, force = true)
        }.mapFailure { it.toProfileRepositoryException() }
    }

    override suspend fun updateBio(userId: String, bio: String): Result<Unit> {
        val sanitized = bio.trim()
        if (sanitized.isBlank()) {
            return Result.failure(ProfileRepositoryException("Bio cannot be empty"))
        }

        val session = currentSessionOrNull()
            ?: return Result.failure(ProfileRepositoryException("Session expired. Please sign in again."))

        return runCatching {
            withContext(ioDispatcher) {
                profileApi.updateBio(session.bearerToken(), sanitized)
            }
            profileState.update { current ->
                if (current != null && current.userId == session.userId) {
                    current.copy(bio = sanitized)
                } else {
                    current
                }
            }
            refreshProfile(session.userId, force = true)
        }.mapFailure { it.toProfileRepositoryException() }
    }

    override suspend fun updateAvatar(userId: String, avatar: ProfileAvatarImage): Result<Unit> {
        val session = currentSessionOrNull()
            ?: return Result.failure(ProfileRepositoryException("Session expired. Please sign in again."))

        if (session.userId != userId) {
            return Result.failure(ProfileRepositoryException("You can only update your own avatar."))
        }

        val avatarPart = withContext(ioDispatcher) {
            runCatching { buildAvatarPart(avatar) }
        }.getOrElse { throwable ->
            val failure = if (throwable is ProfileRepositoryException) {
                throwable
            } else {
                ProfileRepositoryException(AVATAR_FILE_READ_ERROR_MESSAGE, throwable)
            }
            return Result.failure(failure)
        }

        return runCatching {
            withContext(ioDispatcher) {
                profileApi.updateAvatar(session.bearerToken(), avatarPart)
            }
            refreshProfile(session.userId, force = true)
        }.mapFailure { it.toProfileRepositoryException() }
    }

    override suspend fun setPostVote(userId: String, postId: String, target: VoteState): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Post interactions are not supported yet"))
    }

    override suspend fun setReplyVote(userId: String, replyId: String, target: VoteState): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Reply interactions are not supported yet"))
    }

    private fun buildAvatarPart(avatar: ProfileAvatarImage): MultipartBody.Part {
        val uri = avatar.uri
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw ProfileRepositoryException(AVATAR_FILE_READ_ERROR_MESSAGE)
        val bytes = inputStream.use { stream -> stream.readBytes() }
        if (bytes.isEmpty()) {
            throw ProfileRepositoryException(AVATAR_FILE_READ_ERROR_MESSAGE)
        }

        val mediaType = resolveMimeType(uri)?.toMediaTypeOrNull() ?: DEFAULT_AVATAR_MEDIA_TYPE
        val fileName = avatar.displayName?.takeIf { it.isNotBlank() }
            ?: resolveDisplayName(uri)
            ?: buildDefaultAvatarName(mediaType)
        val requestBody = bytes.toRequestBody(mediaType)
        return MultipartBody.Part.createFormData(AVATAR_FIELD_NAME, fileName, requestBody)
    }

    private fun resolveMimeType(uri: Uri): String? {
        val detected = contentResolver.getType(uri)
        if (!detected.isNullOrBlank()) return detected
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()
        return extension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else {
                        null
                    }
                }
        }.getOrNull()
    }

    private fun buildDefaultAvatarName(mediaType: MediaType): String {
        val extension = resolveExtension(mediaType)
        return "avatar_${System.currentTimeMillis()}.$extension"
    }

    private fun resolveExtension(mediaType: MediaType?): String {
        if (mediaType == null) return DEFAULT_AVATAR_EXTENSION
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mediaType.toString())
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_AVATAR_EXTENSION
    }

    private suspend fun refreshProfile(userId: String, force: Boolean = false) {
        val session = currentSessionOrNull() ?: return
        if (!force && cachedUserId == userId && profileState.value != null) {
            return
        }

        cachedUserId = userId
        val fallback = profileState.value ?: session.toFallbackProfile()
        profileState.value = fallback

        val remote = runCatching {
            withContext(ioDispatcher) {
                coroutineScope {
                    val bearer = session.bearerToken()
                    val targetUsername = if (userId == session.userId) session.username else userId

                    val userDeferred = async {
                        if (userId == session.userId) {
                            profileApi.getCurrentUser(bearer)
                        } else {
                            profileApi.getUserByUsername(bearer, targetUsername)
                        }
                    }

                    val postsDeferred = async {
                        runCatching {
                            profileApi.getUserPosts(bearer, targetUsername)
                        }.getOrElse { throwable ->
                            if (throwable is CancellationException) throw throwable
                            emptyList()
                        }
                    }

                    val commentsDeferred = async {
                        runCatching {
                            profileApi.getUserComments(bearer, targetUsername)
                        }.getOrElse { throwable ->
                            if (throwable is CancellationException) throw throwable
                            emptyList()
                        }
                    }

                    val userResponse = userDeferred.await()
                    val postResponses = postsDeferred.await()
                    val commentResponses = commentsDeferred.await()

                    val identifier = if (userId == session.userId) session.userId else userId
                    val postTitleLookup = buildPostTitleLookup(
                        session = session,
                        posts = postResponses,
                        comments = commentResponses
                    )
                    val posts = postResponses.toProfilePosts()
                    val replies = commentResponses.toProfileReplies(postTitleLookup)

                    userResponse.toDomain(
                        userId = identifier,
                        posts = posts,
                        replies = replies
                    )
                }
            }
        }

        profileState.value = remote.getOrDefault(fallback)
    }

    private suspend fun currentSessionOrNull(): UserSession? {
        return userSessionRepository.sessionFlow.filterNotNull().firstOrNull()
    }

    private fun SimpleUserResponse.toDomain(
        userId: String,
        posts: List<ForumProfilePost>,
        replies: List<ForumProfileReply>
    ): ForumUserProfile {
        val normalizedDisplayName = username.ifBlank { userId }
        val resolvedUpvotes = upvoteCount?.takeIf { it >= 0 }
            ?: calculateTotalUpvotes(posts, replies)
        val resolvedPostCount = postCount?.takeIf { it >= 0 } ?: posts.size
        val resolvedCommentCount = commentCount?.takeIf { it >= 0 } ?: replies.size

        return ForumUserProfile(
            userId = userId,
            displayName = normalizedDisplayName,
            avatarUrl = resolveAvatarPath().toAvatarUrl(),
            bio = bio?.ifBlank { null },
            stats = ForumProfileStats(
                upvotes = resolvedUpvotes,
                posts = resolvedPostCount,
                answers = resolvedCommentCount
            ),
            posts = posts,
            replies = replies
        )
    }

    private fun SimpleUserResponse.resolveAvatarPath(): String? {
        return avatarUrl?.ifBlank { null } ?: avatarFilename?.ifBlank { null }
    }

    private fun String?.toAvatarUrl(): String? {
        val raw = this?.takeIf { it.isNotBlank() } ?: return null
        return when {
            raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true) -> raw
            raw.startsWith("/download") -> "$baseUrl$raw"
            raw.startsWith("download/") -> "$baseUrl/$raw"
            raw.startsWith("/") -> "$baseUrl$raw"
            else -> "$baseUrl/download/$raw"
        }
    }

    private suspend fun buildPostTitleLookup(
        session: UserSession,
        posts: List<UserPostResponse>,
        comments: List<UserCommentResponse>
    ): Map<Int, String> {
        val titlesFromPosts = posts.associatePostTitles()
        if (titlesFromPosts.isNotEmpty()) {
            postTitleCache.putAll(titlesFromPosts)
        }

        val commentPostIds = comments.mapNotNull { it.postId }.toSet()
        if (commentPostIds.isEmpty()) {
            return titlesFromPosts
        }

        val cachedMatches = mutableMapOf<Int, String>()
        commentPostIds.forEach { postId ->
            postTitleCache[postId]?.let { cachedMatches[postId] = it }
        }

        val missingIds = commentPostIds - cachedMatches.keys
        val fetchedTitles = if (missingIds.isEmpty()) {
            emptyMap()
        } else {
            fetchPostTitles(session, missingIds.toList())
        }
        if (fetchedTitles.isNotEmpty()) {
            postTitleCache.putAll(fetchedTitles)
        }

        return mutableMapOf<Int, String>().apply {
            putAll(cachedMatches)
            putAll(fetchedTitles)
            putAll(titlesFromPosts)
        }
    }

    private suspend fun fetchPostTitles(
        session: UserSession,
        postIds: List<Int>
    ): Map<Int, String> {
        if (postIds.isEmpty()) return emptyMap()
        val bearer = session.bearerToken()
        return coroutineScope {
            postIds.map { postId ->
                async {
                    runCatching {
                        val detail = postDetailApi.getPostDetail(bearer, postId)
                        postId to resolvePostTitle(postId, detail.title)
                    }.getOrElse { throwable ->
                        if (throwable is CancellationException) throw throwable
                        postId to resolvePostTitle(postId, null)
                    }
                }
            }.awaitAll().toMap()
        }
    }

    private fun List<UserPostResponse>.associatePostTitles(): Map<Int, String> {
        return associate { response ->
            response.postId to resolvePostTitle(response.postId, response.title)
        }
    }

    private fun List<UserPostResponse>.toProfilePosts(): List<ForumProfilePost> {
        return map { response -> response.toProfilePost() }
    }

    private fun UserPostResponse.toProfilePost(): ForumProfilePost {
        return ForumProfilePost(
            id = postId.toString(),
            title = resolvePostTitle(postId, title),
            body = content.orEmpty(),
            timestampLabel = createdAt.toProfileTimestampLabel(),
            voteCount = voteCount ?: 0,
            voteState = userVote.toVoteState()
        )
    }

    private fun resolvePostTitle(postId: Int, rawTitle: String?): String {
        val sanitized = rawTitle?.trim()?.takeIf { it.isNotEmpty() }
        return sanitized ?: "${DEFAULT_POST_TITLE_PREFIX}$postId"
    }

    private fun List<UserCommentResponse>.toProfileReplies(
        postTitleLookup: Map<Int, String>
    ): List<ForumProfileReply> {
        return map { response -> response.toProfileReply(postTitleLookup) }
    }

    private fun UserCommentResponse.toProfileReply(
        postTitleLookup: Map<Int, String>
    ): ForumProfileReply {
        val postIdentifier = postId?.toString() ?: ""
        val resolvedTitle = postId?.let { postTitleLookup[it] } ?: postId?.let {
            "${DEFAULT_POST_TITLE_PREFIX}$it"
        } ?: DEFAULT_POST_FALLBACK_TITLE

        return ForumProfileReply(
            id = commentId.toString(),
            postId = postIdentifier,
            questionTitle = resolvedTitle,
            body = content.orEmpty(),
            timestampLabel = createdAt.toProfileTimestampLabel(),
            voteCount = voteCount ?: 0,
            voteState = userVote.toVoteState()
        )
    }

    private fun Int?.toVoteState(): VoteState = when (this) {
        1 -> VoteState.UPVOTED
        -1 -> VoteState.DOWNVOTED
        else -> VoteState.NONE
    }

    private fun String?.toProfileTimestampLabel(): String {
        if (this.isNullOrBlank()) return DEFAULT_DATE_PLACEHOLDER
        val createdInstant = parseInstant(this) ?: return DEFAULT_DATE_PLACEHOLDER
        val now = Instant.now()
        val minutes = Duration.between(createdInstant, now).toMinutes().coerceAtLeast(0)
        return if (minutes < RECENT_WINDOW_MINUTES) {
            val displayMinutes = minutes.coerceAtLeast(1)
            "$displayMinutes phút trước"
        } else {
            val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN, Locale.getDefault())
            createdInstant.atZone(ZoneId.systemDefault()).format(formatter)
        }
    }

    private fun parseInstant(raw: String): Instant? {
        return runCatching { Instant.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
            .recoverCatching {
                LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .toInstant(ZoneOffset.UTC)
            }
            .getOrNull()
    }

    private fun calculateTotalUpvotes(
        posts: List<ForumProfilePost>,
        replies: List<ForumProfileReply>
    ): Int {
        val postVotes = posts.sumOf { max(0, it.voteCount) }
        val replyVotes = replies.sumOf { max(0, it.voteCount) }
        return postVotes + replyVotes
    }

    private fun UserSession.toFallbackProfile(): ForumUserProfile {
        return ForumUserProfile(
            userId = userId,
            displayName = username,
            avatarUrl = null,
            bio = null,
            stats = ForumProfileStats(
                upvotes = 0,
                posts = 0,
                answers = 0
            ),
            posts = emptyList(),
            replies = emptyList()
        )
    }

    companion object {
        private const val AVATAR_FIELD_NAME = "new_avatar"
        private const val DEFAULT_AVATAR_EXTENSION = "jpg"
        private val DEFAULT_AVATAR_MEDIA_TYPE = "image/jpeg".toMediaType()
        private const val AVATAR_FILE_READ_ERROR_MESSAGE = "Unable to read selected image."
        private const val DEFAULT_POST_TITLE_PREFIX = "Post #"
        private const val DEFAULT_POST_FALLBACK_TITLE = "Post"
        private const val RECENT_WINDOW_MINUTES = 60L
        private const val DATE_FORMAT_PATTERN = "dd/MM/yyyy HH:mm"
        private const val DEFAULT_DATE_PLACEHOLDER = "--/--/---- --:--"
    }
}

private class ProfileRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)

private fun Throwable.toProfileRepositoryException(): ProfileRepositoryException = when (this) {
    is ProfileRepositoryException -> this
    is HttpException -> {
        val status = this.code()
        val message = when (status) {
            400, 422 -> "Invalid profile information. Please review and try again."
            401 -> "Session expired. Please sign in again."
            404 -> "User not found."
            409 -> "This username is already taken."
            else -> "Unexpected server error ($status)."
        }
        ProfileRepositoryException(message, this)
    }
    is IOException -> ProfileRepositoryException("Network connection error. Please try again.", this)
    else -> ProfileRepositoryException(message ?: "Unexpected error", this)
}

private fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(transform(it)) }
    )
