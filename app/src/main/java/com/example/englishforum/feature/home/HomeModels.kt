package com.example.englishforum.feature.home

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.PostTag

data class HomePostUi(
    val id: String,
    val authorName: String,
    val relativeTimeText: String,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val commentCount: Int,
    val tag: PostTag
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val posts: List<HomePostUi> = emptyList(),
    val availableFilters: List<HomeFeedFilter> = emptyList(),
    val selectedFilter: HomeFeedFilter = HomeFeedFilter.Latest
)

sealed class HomeFeedFilter(open val id: String) {
    object Latest : HomeFeedFilter(id = "latest")
    object Trending : HomeFeedFilter(id = "trending")
    data class Tag(val tag: PostTag) : HomeFeedFilter(id = "tag-${tag.name}")
}
