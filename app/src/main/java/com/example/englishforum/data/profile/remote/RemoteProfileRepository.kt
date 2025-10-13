package com.example.englishforum.data.profile.remote

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumProfileStats
import com.example.englishforum.core.model.forum.ForumUserProfile
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.bearerToken
import com.example.englishforum.data.profile.ProfileRepository
import com.example.englishforum.data.profile.remote.model.SimpleUserResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class RemoteProfileRepository(
    private val profileApi: ProfileApi,
    private val userSessionRepository: UserSessionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProfileRepository {

    override fun observeProfile(userId: String): Flow<ForumUserProfile> = flow {
        val session = userSessionRepository.sessionFlow
            .filterNotNull()
            .firstOrNull()
            ?: throw IllegalStateException("No active session found")

        val fallbackProfile = session.toFallbackProfile()

        val profile = runCatching {
            val remoteUser = withContext(ioDispatcher) {
                profileApi.getCurrentUser(session.bearerToken())
            }
            remoteUser.toDomain(session.userId)
        }.getOrElse { fallbackProfile }

        emit(profile)
    }

    override suspend fun updateDisplayName(userId: String, displayName: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Display name updates are not supported yet"))
    }

    override suspend fun updateBio(userId: String, bio: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Bio updates are not supported yet"))
    }

    override suspend fun setPostVote(userId: String, postId: String, target: VoteState): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Post interactions are not supported yet"))
    }

    override suspend fun setReplyVote(userId: String, replyId: String, target: VoteState): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Reply interactions are not supported yet"))
    }

    private fun SimpleUserResponse.toDomain(userId: String): ForumUserProfile {
        return ForumUserProfile(
            userId = userId,
            displayName = username,
            avatarUrl = avatarUrl?.ifBlank { null },
            bio = bio?.ifBlank { null },
            stats = ForumProfileStats(
                upvotes = 0,
                posts = 0,
                answers = 0
            ),
            posts = emptyList(),
            replies = emptyList()
        )
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
}
