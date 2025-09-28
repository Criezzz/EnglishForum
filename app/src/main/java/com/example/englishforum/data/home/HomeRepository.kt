package com.example.englishforum.data.home

import com.example.englishforum.core.model.VoteState
import kotlinx.coroutines.flow.Flow

data class HomePost(
    val id: String,
    val authorName: String,
    val minutesAgo: Int,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val commentCount: Int
)

interface HomeRepository {
    val postsStream: Flow<List<HomePost>>

    suspend fun setVoteState(postId: String, target: VoteState): Result<Unit>
}
