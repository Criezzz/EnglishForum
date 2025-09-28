package com.example.englishforum.data.post

import com.example.englishforum.core.model.VoteState
import kotlinx.coroutines.flow.Flow

class FakePostDetailRepository(
    private val store: FakePostStore = FakePostStore
) : PostDetailRepository {

    override fun observePost(postId: String): Flow<PostDetail?> {
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
}
