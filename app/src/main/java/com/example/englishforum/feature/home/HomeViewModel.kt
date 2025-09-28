package com.example.englishforum.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.data.home.FakeHomeRepository
import com.example.englishforum.data.home.HomePost
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
    private val repository: HomeRepository = FakeHomeRepository()
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

    private fun filterPosts(posts: List<HomePost>, query: String): List<HomePost> {
        if (query.isBlank()) return posts
        val normalized = query.trim().lowercase()
        return posts.filter { post ->
            post.title.contains(normalized, ignoreCase = true) ||
                post.body.contains(normalized, ignoreCase = true) ||
                post.authorName.contains(normalized, ignoreCase = true)
        }
    }

    private fun HomePost.toUiModel(): HomePostUi {
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

    private fun formatRelativeTime(minutesAgo: Int): String {
        return when {
            minutesAgo < 1 -> "Vừa xong"
            minutesAgo < 60 -> "$minutesAgo phút trước"
            minutesAgo < 1_440 -> {
                val hours = minutesAgo / 60
                "$hours giờ trước"
            }
            else -> {
                val days = minutesAgo / 1_440
                "$days ngày trước"
            }
        }
    }
}
