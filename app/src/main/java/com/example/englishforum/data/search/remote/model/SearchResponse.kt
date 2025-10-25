package com.example.englishforum.data.search.remote.model

import com.squareup.moshi.Json

data class SearchResponse(
    val posts: List<SearchPostResponse>? = emptyList(),
    val users: List<SearchUserResponse>? = emptyList()
)

data class SearchPostResponse(
    @Json(name = "post_id") val postId: Int,
    @Json(name = "author_id") val authorId: Int? = null,
    val title: String? = null,
    val content: String? = null,
    @Json(name = "vote_count") val voteCount: Int? = null,
    @Json(name = "comment_count") val commentCount: Int? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    val tag: String? = null,
    @Json(name = "author_username") val authorUsername: String? = null,
    @Json(name = "author_display_name") val authorDisplayName: String? = null,
    @Json(name = "author_name") val authorName: String? = null,
    @Json(name = "author_avatar") val authorAvatar: String? = null,
    @Json(name = "user_vote") val userVote: Int? = null
)

data class SearchUserResponse(
    @Json(name = "user_id") val userId: Int? = null,
    val username: String? = null,
    val bio: String? = null,
    @Json(name = "avatar_filename") val avatarFilename: String? = null
)
