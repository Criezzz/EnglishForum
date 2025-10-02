package com.example.englishforum.data.home

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostSummary
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    val postsStream: Flow<List<ForumPostSummary>>

    suspend fun setVoteState(postId: String, target: VoteState): Result<Unit>
}
