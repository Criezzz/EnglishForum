package com.example.englishforum.data.profile.remote.model

import com.squareup.moshi.Json

data class SimpleUserResponse(
    val username: String,
    val bio: String? = null,
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    @Json(name = "avatar_filename")
    val avatarFilename: String? = null,
    @Json(name = "following")
    val isFollowing: Boolean? = null,
    @Json(name = "follower_count")
    val followerCount: Int? = null,
    @Json(name = "following_count")
    val followingCount: Int? = null,
    @Json(name = "post_count")
    val postCount: Int? = null,
    @Json(name = "comment_count")
    val commentCount: Int? = null,
    @Json(name = "upvote_count")
    val upvoteCount: Int? = null
)
