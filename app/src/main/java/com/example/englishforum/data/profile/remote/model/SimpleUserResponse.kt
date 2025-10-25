package com.example.englishforum.data.profile.remote.model

import com.squareup.moshi.Json

data class SimpleUserResponse(
    val username: String,
    val bio: String? = null,
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    @Json(name = "avatar_filename")
    val avatarFilename: String? = null
)
