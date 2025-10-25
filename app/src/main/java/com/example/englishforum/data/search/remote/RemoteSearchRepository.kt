package com.example.englishforum.data.search.remote

import com.example.englishforum.BuildConfig
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostSummary
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.core.model.search.SearchResult
import com.example.englishforum.core.model.search.SearchUser
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.bearerToken
import com.example.englishforum.data.home.HomeRepository
import com.example.englishforum.data.search.SearchRepository
import com.example.englishforum.data.search.remote.model.SearchPostResponse
import com.example.englishforum.data.search.remote.model.SearchResponse
import com.example.englishforum.data.search.remote.model.SearchUserResponse
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RemoteSearchRepository(
    private val searchApi: SearchApi,
    private val userSessionRepository: UserSessionRepository,
    private val postInteractionRepository: HomeRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SearchRepository {

    override suspend fun search(keyword: String): Result<SearchResult> {
        val sanitized = keyword.trim()
        if (sanitized.isBlank()) {
            return Result.success(SearchResult(emptyList(), emptyList()))
        }

        val session = currentSessionOrNull()
            ?: return Result.failure(SearchRepositoryException(SESSION_EXPIRED_MESSAGE))

        return withContext(ioDispatcher) {
            runCatching {
                val response = searchApi.search(
                    bearer = session.bearerToken(),
                    keyword = sanitized.toBackendPattern()
                )
                response.toDomain()
            }
        }.mapFailure { it.toSearchRepositoryException() }
    }

    override suspend fun updateVote(postId: String, target: VoteState): Result<Unit> {
        val result = postInteractionRepository.setVoteState(postId, target)
        return result.mapFailure { throwable -> throwable.toSearchRepositoryException() }
    }

    private suspend fun currentSessionOrNull(): UserSession? {
        return userSessionRepository.sessionFlow.firstOrNull()
    }

    private fun SearchResponse.toDomain(): SearchResult {
        val normalizedBase = BuildConfig.API_BASE_URL.trimEnd('/')
        val usersDomain = users.orEmpty()
            .mapNotNull { it.toDomain(normalizedBase) }
        val usersLookup = usersDomain
            .associateBy { it.id.toIntOrNull() }
        val postsDomain = posts.orEmpty()
            .mapNotNull { it.toDomain(normalizedBase, usersLookup) }
        return SearchResult(
            posts = postsDomain,
            users = usersDomain
        )
    }

    private fun SearchUserResponse.toDomain(baseUrl: String): SearchUser? {
        val identifier = userId ?: return null
        val normalizedUsername = username?.takeIf { it.isNotBlank() } ?: return null
        return SearchUser(
            id = identifier.toString(),
            username = normalizedUsername,
            avatarUrl = avatarFilename.toDownloadUrl(baseUrl),
            bio = bio?.ifBlank { null }
        )
    }

    private fun SearchPostResponse.toDomain(
        baseUrl: String,
        usersLookup: Map<Int?, SearchUser>
    ): ForumPostSummary {
        return ForumPostSummary(
            id = postId.toString(),
            authorName = resolveAuthorName(usersLookup),
            minutesAgo = createdAt.toMinutesAgo(),
            title = title.orEmpty(),
            body = content.orEmpty(),
            voteCount = voteCount ?: 0,
            voteState = userVote.toVoteState(),
            commentCount = commentCount ?: 0,
            tag = tag.toPostTag(),
            authorAvatarUrl = authorAvatar.toDownloadUrl(baseUrl),
            previewImageUrl = null
        )
    }

    private fun SearchPostResponse.resolveAuthorName(usersLookup: Map<Int?, SearchUser>): String {
        return listOfNotNull(
            authorDisplayName,
            authorUsername,
            authorName,
            usersLookup[authorId]?.username,
            authorId?.let { "Người dùng #$it" }
        ).firstOrNull { it.isNotBlank() } ?: DEFAULT_AUTHOR
    }

    private fun String?.toDownloadUrl(baseUrl: String): String? {
        val filename = this?.takeIf { it.isNotBlank() } ?: return null
        return "$baseUrl/download/$filename"
    }

    private fun Int?.toVoteState(): VoteState = when (this) {
        1 -> VoteState.UPVOTED
        -1 -> VoteState.DOWNVOTED
        else -> VoteState.NONE
    }

    private fun String?.toPostTag(): PostTag {
        return when (this?.lowercase(Locale.ROOT)) {
            "tutorial" -> PostTag.Tutorial
            "question", "ask", "ask_question" -> PostTag.AskQuestion
            "resource" -> PostTag.Resource
            "experience", "discussion" -> PostTag.Experience
            else -> PostTag.Experience
        }
    }

    private fun String?.toMinutesAgo(): Int {
        if (this.isNullOrBlank()) return 0
        val now = Instant.now()
        val createdInstant = parseInstant() ?: now
        val minutes = Duration.between(createdInstant, now).toMinutes()
        return minutes.coerceAtLeast(0).toInt()
    }

    private fun String.parseInstant(): Instant? {
        return runCatching { Instant.parse(this) }
            .recoverCatching { OffsetDateTime.parse(this).toInstant() }
            .recoverCatching { LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC) }
            .getOrNull()
    }

    private fun String.toBackendPattern(): String {
        val escaped = replace("%", "\\%").replace("_", "\\_")
        return "%$escaped%"
    }

    private fun Throwable.toSearchRepositoryException(): SearchRepositoryException = when (this) {
        is SearchRepositoryException -> this
        is HttpException -> {
            val message = parseErrorMessage(response()?.errorBody()?.string())
            SearchRepositoryException(message, this)
        }
        is IOException -> SearchRepositoryException(NETWORK_ERROR_MESSAGE, this)
        else -> SearchRepositoryException(message ?: UNKNOWN_ERROR_MESSAGE, this)
    }

    private fun parseErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return UNKNOWN_ERROR_MESSAGE
        return raw
    }

    private fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable -> Result.failure(transform(throwable)) }
        )
    }

    companion object {
        private const val SESSION_EXPIRED_MESSAGE = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
        private const val NETWORK_ERROR_MESSAGE = "Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại mạng."
        private const val UNKNOWN_ERROR_MESSAGE = "Đã xảy ra lỗi, vui lòng thử lại."
        private const val DEFAULT_AUTHOR = "Ẩn danh"
    }
}

class SearchRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
