package com.example.englishforum.data.post

import com.example.englishforum.core.model.VoteState
import kotlinx.coroutines.flow.Flow

data class PostDetail(
    val id: String,
    val authorName: String,
    val minutesAgo: Int,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val commentCount: Int,
    val comments: List<PostComment>
)

data class PostComment(
    val id: String,
    val authorName: String,
    val minutesAgo: Int,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val isAuthor: Boolean = false
)

interface PostDetailRepository {
    fun observePost(postId: String): Flow<PostDetail?>

    suspend fun setPostVote(postId: String, target: VoteState): Result<Unit>

    suspend fun setCommentVote(postId: String, commentId: String, target: VoteState): Result<Unit>
}
