package com.example.englishforum.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.common.formatRelativeTime
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostSummary
import com.example.englishforum.data.home.FakeHomeRepository
import com.example.englishforum.data.home.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.postsStream,
        searchQuery,
        loading
    ) { posts, query, isLoading ->
        val filtered = filterPosts(posts, query)
        HomeUiState(
            isLoading = isLoading,
            searchQuery = query,
            posts = filtered.map { it.toUiModel() }
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState()
        )

    init {
        viewModelScope.launch {
            repository.postsStream.collect {
                loading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onClearSearchQuery() {
        searchQuery.value = ""
    }

    fun onUpvote(postId: String) {
        updateVote(postId, VoteState.UPVOTED)
    }

    fun onDownvote(postId: String) {
        updateVote(postId, VoteState.DOWNVOTED)
    }

    private fun updateVote(postId: String, targetState: VoteState) {
        viewModelScope.launch {
            repository.setVoteState(postId, targetState)
        }
    }

    private fun filterPosts(posts: List<ForumPostSummary>, query: String): List<ForumPostSummary> {
        if (query.isBlank()) return posts
        val normalized = query.trim().lowercase()
        return posts.filter { post ->
            post.title.contains(normalized, ignoreCase = true) ||
                post.body.contains(normalized, ignoreCase = true) ||
                post.authorName.contains(normalized, ignoreCase = true)
        }
    }

    private fun ForumPostSummary.toUiModel(): HomePostUi {
        return HomePostUi(
            id = id,
            authorName = authorName,
            relativeTimeText = formatRelativeTime(minutesAgo),
            title = title,
            body = body,
            voteCount = voteCount,
            voteState = voteState,
            commentCount = max(commentCount, 0)
        )
    }
}

class HomeViewModelFactory(
    private val repository: HomeRepository = FakeHomeRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
