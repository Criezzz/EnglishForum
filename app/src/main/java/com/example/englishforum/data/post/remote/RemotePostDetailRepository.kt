package com.example.englishforum.data.post.remote

import com.example.englishforum.core.common.resolveVoteChange
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumComment
import com.example.englishforum.core.model.forum.ForumPostDetail
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.bearerToken
import com.example.englishforum.data.post.PostDetailRepository
import com.example.englishforum.data.post.ForumPostSummaryStore
import com.example.englishforum.data.post.remote.model.PostCommentResponse
import com.example.englishforum.data.post.remote.model.PostDetailResponse
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

internal class RemotePostDetailRepository(
    private val api: PostDetailApi,
    private val userSessionRepository: UserSessionRepository,
    private val summaryStore: ForumPostSummaryStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PostDetailRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val postStates = mutableMapOf<String, MutableStateFlow<ForumPostDetail?>>()
    private val commentIdLookup = mutableMapOf<String, Map<String, Int>>()

    override fun observePost(postId: String): Flow<ForumPostDetail?> {
        val state = postStates.getOrPut(postId) { MutableStateFlow<ForumPostDetail?>(null) }
        scope.launch { fetchAndStorePost(postId) }
        return state.asStateFlow()
    }

    override suspend fun setPostVote(postId: String, target: VoteState): Result<Unit> {
        val session = currentSessionOrNull()
            ?: return Result.failure(IllegalStateException(SESSION_EXPIRED_MESSAGE))
        val numericId = postId.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException(INVALID_POST_ID_MESSAGE))

        val current = postStates[postId]?.value
            ?: return Result.failure(IllegalStateException(POST_NOT_AVAILABLE_MESSAGE))

        val (nextState, delta) = resolveVoteChange(current.voteState, target)
        val voteValue = nextState.toVoteValue()

        val result = withContext(ioDispatcher) {
            runCatching {
                api.votePost(
                    bearer = session.bearerToken(),
                    postId = numericId,
                    voteType = voteValue
                )
            }
        }

        return result
            .map {
                val updated = current.copy(
                    voteState = nextState,
                    voteCount = current.voteCount + delta
                )
                postStates[postId]?.value = updated
                summaryStore.upsertFromDetail(updated)
                Unit
            }
            .mapFailure { it.toFriendlyException() }
    }

    override suspend fun setCommentVote(postId: String, commentId: String, target: VoteState): Result<Unit> {
        val session = currentSessionOrNull()
            ?: return Result.failure(IllegalStateException(SESSION_EXPIRED_MESSAGE))
        val numericPostId = postId.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException(INVALID_POST_ID_MESSAGE))

        val lookup = commentIdLookup[postId]
        val numericCommentId = lookup?.get(commentId)
            ?: commentId.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException(COMMENT_ID_UNAVAILABLE_MESSAGE))

        val currentPost = postStates[postId]?.value
            ?: return Result.failure(IllegalStateException(POST_NOT_AVAILABLE_MESSAGE))

        val currentComment = currentPost.findComment(commentId)
            ?: return Result.failure(IllegalArgumentException(COMMENT_NOT_FOUND_MESSAGE))

        val (nextState, delta) = resolveVoteChange(currentComment.voteState, target)
        val voteValue = nextState.toVoteValue()

        val result = withContext(ioDispatcher) {
            runCatching {
                api.voteComment(
                    bearer = session.bearerToken(),
                    commentId = numericCommentId,
                    voteType = voteValue
                )
            }
        }

        return result
            .map {
                val updatedComments = currentPost.comments.updateCommentVote(commentId, nextState, delta)
                val updated = currentPost.copy(comments = updatedComments)
                postStates[postId]?.value = updated
                summaryStore.upsertFromDetail(updated)
                Unit
            }
            .mapFailure { it.toFriendlyException() }
    }

    override suspend fun reportPost(postId: String, reason: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException(REPORT_UNSUPPORTED_MESSAGE))
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        val session = currentSessionOrNull()
            ?: return Result.failure(IllegalStateException(SESSION_EXPIRED_MESSAGE))
        val numericId = postId.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException(INVALID_POST_ID_MESSAGE))

        val result = withContext(ioDispatcher) {
            runCatching {
                api.deletePost(
                    bearer = session.bearerToken(),
                    postId = numericId
                )
            }
        }

        return result
            .map {
                postStates[postId]?.value = null
                commentIdLookup.remove(postId)
                summaryStore.remove(postId)
                Unit
            }
            .mapFailure { it.toFriendlyException() }
    }

    override suspend fun addComment(
        postId: String,
        content: String,
        replyToCommentId: String?
    ): Result<Unit> {
        val session = currentSessionOrNull()
            ?: return Result.failure(IllegalStateException(SESSION_EXPIRED_MESSAGE))
        val numericPostId = postId.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException(INVALID_POST_ID_MESSAGE))

        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) {
            return Result.failure(IllegalArgumentException(EMPTY_COMMENT_CONTENT_MESSAGE))
        }

        val numericReplyId = when {
            replyToCommentId.isNullOrBlank() -> null
            else -> {
                val lookup = commentIdLookup[postId]
                lookup?.get(replyToCommentId)
                    ?: replyToCommentId.toIntOrNull()
                    ?: return Result.failure(IllegalArgumentException(REPLY_TARGET_NOT_FOUND_MESSAGE))
            }
        }

        val submission = withContext(ioDispatcher) {
            runCatching {
                api.postComment(
                    bearer = session.bearerToken(),
                    postId = numericPostId,
                    content = trimmedContent,
                    replyCommentId = numericReplyId
                )
            }
        }

        return submission
            .mapCatching {
                fetchAndStorePost(postId).getOrThrow()
                Unit
            }
            .mapFailure { it.toFriendlyException() }
    }

    override suspend fun updatePost(
        postId: String,
        title: String,
        body: String,
        tag: PostTag,
        previewImageUrl: String?,
        galleryImageUrls: List<String>
    ): Result<Unit> {
        val session = currentSessionOrNull()
            ?: return Result.failure(IllegalStateException(SESSION_EXPIRED_MESSAGE))
        val numericId = postId.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException(INVALID_POST_ID_MESSAGE))

        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()
        if (trimmedTitle.isEmpty() || trimmedBody.isEmpty()) {
            return Result.failure(IllegalArgumentException(EMPTY_POST_FIELDS_MESSAGE))
        }

        val result = withContext(ioDispatcher) {
            runCatching {
                api.updatePost(
                    bearer = session.bearerToken(),
                    postId = numericId,
                    title = trimmedTitle,
                    content = trimmedBody,
                    tag = tag.name
                )
            }
        }

        return result
            .map {
                fetchAndStorePost(postId)
                Unit
            }
            .mapFailure { it.toFriendlyException() }
    }

    override suspend fun refreshPost(postId: String): Result<Unit> {
        return fetchAndStorePost(postId)
    }

    private suspend fun fetchAndStorePost(postId: String): Result<Unit> {
        val session = currentSessionOrNull()
            ?: return Result.failure(IllegalStateException(SESSION_EXPIRED_MESSAGE))
        val numericId = postId.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException(INVALID_POST_ID_MESSAGE))

        val fetchResult = withContext(ioDispatcher) {
            runCatching {
                val detail = api.getPostDetail(
                    bearer = session.bearerToken(),
                    postId = numericId
                )
                val comments = api.getPostComments(
                    bearer = session.bearerToken(),
                    postId = numericId
                )
                detail to comments
            }
        }

        return fetchResult
            .map { (detail, comments) ->
                val postAuthorId = resolveAuthorIdentifier(detail)
                val forumComments = comments.toForumComments(postAuthorId)
                commentIdLookup[postId] = comments.associateCommentIds()
                val forumPost = detail.toDomain(postAuthorId, forumComments)
                postStates.getOrPut(postId) { MutableStateFlow(null) }.value = forumPost
                summaryStore.upsertFromDetail(forumPost)
                Unit
            }
            .mapFailure { it.toFriendlyException() }
    }

    private suspend fun currentSessionOrNull(): UserSession? {
        return userSessionRepository.sessionFlow.firstOrNull()
    }

    private fun resolveAuthorIdentifier(detail: PostDetailResponse): String? {
        return when {
            detail.authorId != null -> detail.authorId.toString()
            !detail.authorUsername.isNullOrBlank() -> detail.authorUsername
            else -> null
        }
    }

    private fun PostDetailResponse.toDomain(
        authorId: String?,
        comments: List<ForumComment>
    ): ForumPostDetail {
        return ForumPostDetail(
            id = postId.toString(),
            authorId = authorId ?: UNKNOWN_AUTHOR_ID,
            authorName = resolveAuthorName(),
            minutesAgo = createdAt.toMinutesAgo(),
            title = title,
            body = body,
            voteCount = voteCount,
            voteState = userVote.toVoteState(),
            comments = comments,
            tag = tag.toPostTag(),
            authorAvatarUrl = authorAvatarUrl?.takeIf { it.isNotBlank() },
            previewImageUrl = attachments.resolvePreviewUrl(),
            galleryImages = attachments.resolveGalleryUrls()
        )
    }

    private fun PostDetailResponse.resolveAuthorName(): String {
        return listOfNotNull(
            authorDisplayName,
            authorUsername,
            authorFullName
        ).firstOrNull { !it.isNullOrBlank() }?.trim().takeUnless { it.isNullOrBlank() }
            ?: DEFAULT_AUTHOR_NAME
    }

    private fun List<PostCommentResponse>.toForumComments(postAuthorId: String?): List<ForumComment> {
        if (isEmpty()) return emptyList()

        val nodes = map { response -> RemoteCommentNode(response) }
        val byId = mutableMapOf<Int, RemoteCommentNode>()
        nodes.forEach { node ->
            node.response.commentId?.let { byId[it] = node }
        }

        val roots = mutableListOf<RemoteCommentNode>()
        nodes.forEach { node ->
            val parentId = node.response.replyToId
            val parent = parentId?.let { byId[it] }
            if (parent != null && parent !== node) {
                parent.children.add(node)
            } else {
                roots.add(node)
            }
        }

        return roots.map { it.toForumComment(postAuthorId) }
    }

    private fun RemoteCommentNode.toForumComment(postAuthorId: String?): ForumComment {
        val backendId = response.commentId
        val commentId = backendId?.toString() ?: response.syntheticId()
        val authorName = response.resolveAuthorName()
        val replies = children.map { it.toForumComment(postAuthorId) }

        return ForumComment(
            id = commentId,
            authorName = authorName,
            minutesAgo = response.createdAt.toMinutesAgo(),
            body = response.content,
            voteCount = response.voteCount,
            voteState = response.userVote.toVoteState(),
            isAuthor = postAuthorId != null && response.authorId?.toString() == postAuthorId,
            replies = replies
        )
    }

    private fun PostCommentResponse.resolveAuthorName(): String {
        return listOfNotNull(
            authorDisplayName,
            authorUsername,
            authorFullName,
            authorId?.let { "Thành viên #$it" }
        ).firstOrNull { !it.isNullOrBlank() }?.trim().takeUnless { it.isNullOrBlank() }
            ?: DEFAULT_AUTHOR_NAME
    }

    private fun PostCommentResponse.syntheticId(): String {
        val authorPart = authorId?.toString() ?: "anon"
        val timePart = createdAt.ifBlank { "" }
        return "synthetic-$authorPart-$timePart-${content.hashCode()}"
    }

    private fun List<PostCommentResponse>.associateCommentIds(): Map<String, Int> {
        if (isEmpty()) return emptyMap()
        val mapping = mutableMapOf<String, Int>()
        for (response in this) {
            val backendId = response.commentId ?: continue
            mapping[backendId.toString()] = backendId
        }
        return mapping
    }

    private fun List<ForumComment>.updateCommentVote(
        commentId: String,
        targetState: VoteState,
        delta: Int
    ): List<ForumComment> {
        return map { comment ->
            when {
                comment.id == commentId -> comment.copy(
                    voteState = targetState,
                    voteCount = comment.voteCount + delta
                )
                comment.replies.isNotEmpty() -> comment.copy(
                    replies = comment.replies.updateCommentVote(commentId, targetState, delta)
                )
                else -> comment
            }
        }
    }

    private fun ForumPostDetail.findComment(commentId: String): ForumComment? {
        fun search(list: List<ForumComment>): ForumComment? {
            list.forEach { comment ->
                if (comment.id == commentId) return comment
                val nested = search(comment.replies)
                if (nested != null) return nested
            }
            return null
        }
        return search(comments)
    }

    private fun List<com.example.englishforum.data.home.remote.model.AttachmentResponse>?.resolvePreviewUrl(): String? {
        if (this.isNullOrEmpty()) return null
        return this
            .sortedWith(compareBy({ it.index ?: Int.MAX_VALUE }))
            .firstNotNullOfOrNull { it.mediaUrl?.takeIf { url -> url.isNotBlank() } }
    }

    private fun List<com.example.englishforum.data.home.remote.model.AttachmentResponse>?.resolveGalleryUrls(): List<String>? {
        if (this.isNullOrEmpty()) return null
        val urls = this
            .sortedWith(compareBy({ it.index ?: Int.MAX_VALUE }))
            .mapNotNull { it.mediaUrl?.takeIf { url -> url.isNotBlank() } }
        return urls.takeIf { it.isNotEmpty() }
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

    private fun VoteState.toVoteValue(): Int = when (this) {
        VoteState.NONE -> 0
        VoteState.UPVOTED -> 1
        VoteState.DOWNVOTED -> -1
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

    private fun Throwable.toFriendlyException(): Throwable = when (this) {
        is HttpException -> {
            val message = parseErrorMessage(response()?.errorBody()?.string())
            IllegalStateException(message, this)
        }
        is IOException -> IOException(CONNECTION_ERROR_MESSAGE, this)
        else -> this
    }

    private fun parseErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return GENERIC_ERROR_MESSAGE
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

    private data class RemoteCommentNode(
        val response: PostCommentResponse,
        val children: MutableList<RemoteCommentNode> = mutableListOf()
    )


    companion object {
        private const val SESSION_EXPIRED_MESSAGE = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
        private const val INVALID_POST_ID_MESSAGE = "Định danh bài viết không hợp lệ"
        private const val COMMENT_ID_UNAVAILABLE_MESSAGE = "Không thể xác định bình luận để xử lý"
        private const val COMMENT_NOT_FOUND_MESSAGE = "Không tìm thấy bình luận"
        private const val POST_NOT_AVAILABLE_MESSAGE = "Bài viết không còn khả dụng"
        private const val REPORT_UNSUPPORTED_MESSAGE = "Tính năng báo cáo bài viết tạm thời chưa khả dụng"
        private const val EMPTY_POST_FIELDS_MESSAGE = "Tiêu đề và nội dung không được để trống"
        private const val EMPTY_COMMENT_CONTENT_MESSAGE = "Nội dung bình luận không được để trống"
        private const val REPLY_TARGET_NOT_FOUND_MESSAGE = "Không tìm thấy bình luận để trả lời"
        private const val CONNECTION_ERROR_MESSAGE = "Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại mạng."
        private const val GENERIC_ERROR_MESSAGE = "Đã xảy ra lỗi, vui lòng thử lại"
        private const val DEFAULT_AUTHOR_NAME = "Ẩn danh"
        private const val UNKNOWN_AUTHOR_ID = "unknown"
    }
}
