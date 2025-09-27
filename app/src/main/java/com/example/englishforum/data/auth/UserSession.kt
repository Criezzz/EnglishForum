package com.example.englishforum.data.auth

data class UserSession(
    val userId: String,
    val username: String,
    val token: String
)
