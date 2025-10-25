package com.example.englishforum.feature.search

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.PostTag

data class SearchPostUi(
    val id: String,
    val authorName: String,
    val authorUsername: String? = null,
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

data class SearchUserUi(
    val id: String,
    val username: String,
    val bio: String? = null,
    val avatarUrl: String? = null
)

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val posts: List<SearchPostUi> = emptyList(),
    val users: List<SearchUserUi> = emptyList(),
    val errorMessage: String? = null
)
