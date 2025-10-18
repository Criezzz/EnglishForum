package com.example.englishforum.data.profile.remote

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumProfileStats
import com.example.englishforum.core.model.forum.ForumUserProfile
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.bearerToken
import com.example.englishforum.data.profile.ProfileRepository
import com.example.englishforum.data.profile.remote.model.SimpleUserResponse
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RemoteProfileRepository(
    private val profileApi: ProfileApi,
    private val userSessionRepository: UserSessionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProfileRepository {

    private val profileState = MutableStateFlow<ForumUserProfile?>(null)
    private var cachedUserId: String? = null

    override fun observeProfile(userId: String): Flow<ForumUserProfile> = flow {
        refreshProfile(userId, force = cachedUserId != userId)
        emitAll(
            profileState
                .filterNotNull()
                .distinctUntilChanged()
        )
    }

    override suspend fun updateDisplayName(userId: String, displayName: String): Result<Unit> {
        val sanitized = displayName.trim()
        if (sanitized.isBlank()) {
            return Result.failure(ProfileRepositoryException("Display name cannot be empty"))
        }

        val session = currentSessionOrNull()
            ?: return Result.failure(ProfileRepositoryException("Session expired. Please sign in again."))

        return runCatching {
            withContext(ioDispatcher) {
                profileApi.updateUsername(session.bearerToken(), sanitized)
            }
            userSessionRepository.saveSession(session.copy(username = sanitized))
            profileState.update { current ->
                if (current != null && current.userId == session.userId) {
                    current.copy(displayName = sanitized)
                } else {
                    current
                }
            }
            refreshProfile(session.userId, force = true)
        }.mapFailure { it.toProfileRepositoryException() }
    }

    override suspend fun updateBio(userId: String, bio: String): Result<Unit> {
        val sanitized = bio.trim()
        if (sanitized.isBlank()) {
            return Result.failure(ProfileRepositoryException("Bio cannot be empty"))
        }

        val session = currentSessionOrNull()
            ?: return Result.failure(ProfileRepositoryException("Session expired. Please sign in again."))

        return runCatching {
            withContext(ioDispatcher) {
                profileApi.updateBio(session.bearerToken(), sanitized)
            }
            profileState.update { current ->
                if (current != null && current.userId == session.userId) {
                    current.copy(bio = sanitized)
                } else {
                    current
                }
            }
            refreshProfile(session.userId, force = true)
        }.mapFailure { it.toProfileRepositoryException() }
    }

    override suspend fun setPostVote(userId: String, postId: String, target: VoteState): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Post interactions are not supported yet"))
    }

    override suspend fun setReplyVote(userId: String, replyId: String, target: VoteState): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Reply interactions are not supported yet"))
    }

    private suspend fun refreshProfile(userId: String, force: Boolean = false) {
        val session = currentSessionOrNull() ?: return
        if (!force && cachedUserId == userId && profileState.value != null) {
            return
        }

        cachedUserId = userId
        val fallback = profileState.value ?: session.toFallbackProfile()
        profileState.value = fallback

        val remote = runCatching {
            withContext(ioDispatcher) {
                if (userId == session.userId) {
                    profileApi.getCurrentUser(session.bearerToken())
                } else {
                    profileApi.getUserByUsername(session.bearerToken(), userId)
                }
            }
        }.map { response ->
            val identifier = if (userId == session.userId) session.userId else userId
            response.toDomain(identifier)
        }

        profileState.value = remote.getOrDefault(fallback)
    }

    private suspend fun currentSessionOrNull(): UserSession? {
        return userSessionRepository.sessionFlow.filterNotNull().firstOrNull()
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

private class ProfileRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)

private fun Throwable.toProfileRepositoryException(): ProfileRepositoryException = when (this) {
    is ProfileRepositoryException -> this
    is HttpException -> {
        val status = this.code()
        val message = when (status) {
            400, 422 -> "Invalid profile information. Please review and try again."
            401 -> "Session expired. Please sign in again."
            404 -> "User not found."
            409 -> "This username is already taken."
            else -> "Unexpected server error ($status)."
        }
        ProfileRepositoryException(message, this)
    }
    is IOException -> ProfileRepositoryException("Network connection error. Please try again.", this)
    else -> ProfileRepositoryException(message ?: "Unexpected error", this)
}

private fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(transform(it)) }
    )
