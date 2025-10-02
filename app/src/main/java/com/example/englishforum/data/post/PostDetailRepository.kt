package com.example.englishforum.data.post

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostDetail
import kotlinx.coroutines.flow.Flow

interface PostDetailRepository {
    fun observePost(postId: String): Flow<ForumPostDetail?>

    suspend fun setPostVote(postId: String, target: VoteState): Result<Unit>

    suspend fun setCommentVote(postId: String, commentId: String, target: VoteState): Result<Unit>
}
