package com.example.englishforum.data.profile

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumUserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(userId: String): Flow<ForumUserProfile>

    suspend fun updateDisplayName(userId: String, displayName: String): Result<Unit>

    suspend fun setPostVote(userId: String, postId: String, target: VoteState): Result<Unit>

    suspend fun setReplyVote(userId: String, replyId: String, target: VoteState): Result<Unit>
}
