package com.example.englishforum.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.common.formatRelativeTime
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostSummary
import com.example.englishforum.data.home.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

class SearchViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(true)

    private val postsStream = repository.postsStream
        .onEach { loading.value = false }

    val uiState: StateFlow<SearchUiState> = combine(
        postsStream,
        query,
        loading
    ) { posts, currentQuery, isLoading ->
        val normalized = currentQuery.trim()
        val filteredPosts = if (normalized.isBlank()) {
            emptyList()
        } else {
            posts.filter { post ->
                post.title.contains(normalized, ignoreCase = true) ||
                    post.body.contains(normalized, ignoreCase = true) ||
                    post.authorName.contains(normalized, ignoreCase = true)
            }
        }

        SearchUiState(
            query = currentQuery,
            isLoading = isLoading,
            results = filteredPosts.map { it.toUiModel() }
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SearchUiState()
        )

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onClearQuery() {
        query.value = ""
    }

    fun onUpvote(postId: String) {
        updateVote(postId, VoteState.UPVOTED)
    }

    fun onDownvote(postId: String) {
        updateVote(postId, VoteState.DOWNVOTED)
    }

    private fun updateVote(postId: String, target: VoteState) {
        viewModelScope.launch {
            repository.setVoteState(postId, target)
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
}

class SearchViewModelFactory(
    private val repository: HomeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
