package com.example.englishforum.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.common.formatRelativeTime
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostSummary
import com.example.englishforum.core.model.forum.PostTag
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

    private val loading = MutableStateFlow(true)
    private val refreshing = MutableStateFlow(false)
    private val selectedFilter = MutableStateFlow<HomeFeedFilter>(HomeFeedFilter.Latest)

    private val filterOptions = listOf(
        HomeFeedFilter.Latest,
        HomeFeedFilter.Trending,
        HomeFeedFilter.Tag(PostTag.Tutorial),
        HomeFeedFilter.Tag(PostTag.AskQuestion),
        HomeFeedFilter.Tag(PostTag.Resource),
        HomeFeedFilter.Tag(PostTag.Experience)
    )

    val uiState: StateFlow<HomeUiState> = combine(
        repository.postsStream,
        loading,
        refreshing,
        selectedFilter
    ) { posts, isLoading, isRefreshing, filter ->
        val filtered = filterPosts(posts, filter)
        HomeUiState(
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            posts = filtered.map { it.toUiModel() },
            availableFilters = filterOptions,
            selectedFilter = filter
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState()
        )

    init {
        viewModelScope.launch {
            refreshFeed(isInitial = true)
        }
    }

    fun onRefresh() {
        if (loading.value || refreshing.value) return
        viewModelScope.launch {
            refreshFeed(isInitial = false)
        }
    }

    fun onFilterSelected(filter: HomeFeedFilter) {
        if (selectedFilter.value.id == filter.id) return
        selectedFilter.value = filter
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

    private suspend fun refreshFeed(isInitial: Boolean) {
        if (!isInitial) {
            refreshing.value = true
        }
        try {
            repository.refresh()
        } finally {
            loading.value = false
            if (!isInitial) {
                refreshing.value = false
            }
        }
    }

    private fun filterPosts(
        posts: List<ForumPostSummary>,
        filter: HomeFeedFilter
    ): List<ForumPostSummary> {
        return when (filter) {
            HomeFeedFilter.Latest -> posts.sortedBy { it.minutesAgo }
            HomeFeedFilter.Trending -> posts.sortedWith(
                compareByDescending<ForumPostSummary> { it.voteCount }
                    .thenBy { it.minutesAgo }
            )
            is HomeFeedFilter.Tag -> posts
                .filter { it.tag == filter.tag }
                .sortedBy { it.minutesAgo }
        }
    }

    private fun ForumPostSummary.toUiModel(): HomePostUi {
        return HomePostUi(
            id = id,
            authorName = authorName,
            authorUsername = authorUsername,
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
