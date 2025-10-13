package com.example.englishforum.feature.search

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.PostTag

data class SearchResultUi(
    val id: String,
    val authorName: String,
    val relativeTimeText: String,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val commentCount: Int,
    val tag: PostTag,
    val authorAvatarUrl: String? = null,
    val previewImageUrl: String? = null
)

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = true,
    val results: List<SearchResultUi> = emptyList()
)
