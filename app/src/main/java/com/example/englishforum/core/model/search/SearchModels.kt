package com.example.englishforum.core.model.search

import com.example.englishforum.core.model.forum.ForumPostSummary

data class SearchResult(
    val posts: List<ForumPostSummary>,
    val users: List<SearchUser>
)

data class SearchUser(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
    val bio: String? = null
)
