package com.example.englishforum.data.auth.remote.model

import com.squareup.moshi.Json

data class RefreshTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "message") val message: String? = null
)
