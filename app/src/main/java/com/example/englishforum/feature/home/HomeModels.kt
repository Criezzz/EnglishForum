package com.example.englishforum.feature.home

import com.example.englishforum.core.model.VoteState

data class HomePostUi(
    val id: String,
    val authorName: String,
    val relativeTimeText: String,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val commentCount: Int
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val posts: List<HomePostUi> = emptyList()
)
