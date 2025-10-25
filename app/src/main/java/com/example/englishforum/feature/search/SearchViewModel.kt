package com.example.englishforum.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.common.formatRelativeTime
import com.example.englishforum.core.common.resolveVoteChange
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostSummary
import com.example.englishforum.data.search.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

class SearchViewModel(
    private val repository: SearchRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(false)
    private val posts = MutableStateFlow<List<ForumPostSummary>>(emptyList())
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        loading,
        posts,
        error
    ) { currentQuery, isLoading, results, errorMessage ->
        SearchUiState(
            query = currentQuery,
            isLoading = isLoading,
            results = results.map { it.toUiModel() },
            errorMessage = errorMessage
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SearchUiState()
        )

    init {
        observeQuery()
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
        error.value = null
    }

    fun onClearQuery() {
        query.value = ""
        posts.value = emptyList()
        error.value = null
        loading.value = false
    }

    fun onUpvote(postId: String) {
        updateVote(postId, VoteState.UPVOTED)
    }

    fun onDownvote(postId: String) {
        updateVote(postId, VoteState.DOWNVOTED)
    }

    private fun observeQuery() {
        viewModelScope.launch {
            query
                .debounce(QUERY_DEBOUNCE_MS)
                .map(String::trim)
                .distinctUntilChanged()
                .collectLatest { normalized ->
                    if (normalized.isBlank()) {
                        loading.value = false
                        posts.value = emptyList()
                        error.value = null
                    } else {
                        performSearch(normalized)
                    }
                }
        }
    }

    private suspend fun performSearch(keyword: String) {
        loading.value = true
        error.value = null

        val result = repository.search(keyword)
        result.onSuccess { searchResult ->
            posts.value = searchResult.posts
            error.value = null
        }.onFailure { throwable ->
            posts.value = emptyList()
            error.value = throwable.message ?: GENERIC_ERROR_MESSAGE
        }

        loading.value = false
    }

    private fun updateVote(postId: String, target: VoteState) {
        viewModelScope.launch {
            val currentPosts = posts.value
            val index = currentPosts.indexOfFirst { it.id == postId }
            if (index == -1) return@launch

            val targetPost = currentPosts[index]
            val (nextState, delta) = resolveVoteChange(targetPost.voteState, target)
            val optimistic = targetPost.copy(
                voteState = nextState,
                voteCount = (targetPost.voteCount + delta).coerceAtLeast(0)
            )

            posts.update { existing ->
                existing.toMutableList().also { list ->
                    list[index] = optimistic
                }
            }

            val voteResult = repository.updateVote(postId, target)
            if (voteResult.isFailure) {
                posts.value = currentPosts
                error.value = voteResult.exceptionOrNull()?.message ?: GENERIC_ERROR_MESSAGE
            }
        }
    }

    private fun ForumPostSummary.toUiModel(): SearchResultUi {
        return SearchResultUi(
            id = id,
            authorName = authorName,
            relativeTimeText = formatRelativeTime(minutesAgo),
            title = title,
            body = body,
            voteCount = voteCount,
            voteState = voteState,
            commentCount = max(commentCount, 0),
            tag = tag,
            authorAvatarUrl = authorAvatarUrl,
            previewImageUrl = previewImageUrl
        )
    }

    companion object {
        private const val QUERY_DEBOUNCE_MS = 350L
        private const val GENERIC_ERROR_MESSAGE = "Không thể tìm kiếm lúc này. Vui lòng thử lại."
    }
}

class SearchViewModelFactory(
    private val repository: SearchRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
