package com.example.englishforum.data.auth

data class UserSession(
    val userId: String,
    val username: String,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val isEmailVerified: Boolean = true
)
