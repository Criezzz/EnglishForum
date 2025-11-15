package com.example.englishforum.data.post

import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostDetail
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.data.post.PostAttachmentEdit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull

class FakePostDetailRepository(
    private val store: FakePostStore = FakePostStore,
    private val userSessionRepository: UserSessionRepository? = null
) : PostDetailRepository {

    override fun observePost(postId: String): Flow<ForumPostDetail?> {
        return store.observePost(postId)
    }

    override fun observeRealtimeEvents(postId: String): Flow<PostRealtimeEvent> = emptyFlow()

    override suspend fun refreshPost(postId: String): Result<Unit> = Result.success(Unit)

    override suspend fun setPostVote(postId: String, target: VoteState): Result<Unit> {
        return if (store.updatePostVote(postId, target)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Post not found"))
        }
    }

    override suspend fun setCommentVote(postId: String, commentId: String, target: VoteState): Result<Unit> {
        val (postFound, commentFound) = store.updateCommentVote(postId, commentId, target)
        return when {
            !postFound -> Result.failure(IllegalArgumentException("Post not found"))
            !commentFound -> Result.failure(IllegalArgumentException("Comment not found"))
            else -> Result.success(Unit)
        }
    }

    override suspend fun reportPost(postId: String, reason: String): Result<Unit> {
        return if (store.reportPost(postId, reason)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Post not found"))
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return if (store.deletePost(postId)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Post not found"))
        }
    }

    override suspend fun addComment(
        postId: String,
        content: String,
        replyToCommentId: String?
    ): Result<Unit> {
        val sanitized = content.trim()
        if (sanitized.isEmpty()) {
            return Result.failure(IllegalArgumentException("Nội dung bình luận không được để trống"))
        }
        val session = userSessionRepository?.sessionFlow?.firstOrNull()
        val authorName = session?.username ?: "Bạn"
        return if (
            store.addComment(
                postId = postId,
                content = sanitized,
                authorId = session?.userId,
                authorName = authorName,
                authorUsername = session?.username,
                replyToCommentId = replyToCommentId
            )
        ) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Post not found"))
        }
    }

    override suspend fun updatePost(
        postId: String,
        title: String,
        body: String,
        tag: PostTag,
        attachmentEdits: PostAttachmentEdit?
    ): Result<Unit> {
        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()
        if (trimmedTitle.isEmpty() || trimmedBody.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tiêu đề và nội dung không được để trống"))
        }
        return if (
            store.updatePostContent(
                postId = postId,
                title = trimmedTitle,
                body = trimmedBody,
                tag = tag,
                attachmentEdit = attachmentEdits
            )
        ) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Post not found"))
        }
    }

    override suspend fun updateComment(
        postId: String,
        commentId: String,
        content: String
    ): Result<Unit> {
        val sanitized = content.trim()
        if (sanitized.isEmpty()) {
            return Result.failure(IllegalArgumentException("Nội dung bình luận không được để trống"))
        }
        return if (store.updateComment(postId, commentId, sanitized)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Comment not found"))
        }
    }

    override suspend fun deleteComment(
        postId: String,
        commentId: String
    ): Result<Unit> {
        return if (store.deleteComment(postId, commentId)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Comment not found"))
        }
    }
}
