package com.example.englishforum.data.auth.remote.model

import com.squareup.moshi.Json

data class ResetTokenResponse(
    val message: String,
    @Json(name = "reset_token") val resetToken: String
)
