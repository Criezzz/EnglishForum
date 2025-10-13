package com.example.englishforum.data.post

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostDetail
import com.example.englishforum.core.model.forum.PostTag
import kotlinx.coroutines.flow.Flow

class FakePostDetailRepository(
    private val store: FakePostStore = FakePostStore
) : PostDetailRepository {

    override fun observePost(postId: String): Flow<ForumPostDetail?> {
        return store.observePost(postId)
    }

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

    override suspend fun updatePost(
        postId: String,
        title: String,
        body: String,
        tag: PostTag,
        previewImageUrl: String?,
        galleryImageUrls: List<String>
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
                previewImageUrl = previewImageUrl,
                galleryImageUrls = galleryImageUrls
            )
        ) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Post not found"))
        }
    }
}
