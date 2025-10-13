package com.example.englishforum.data.auth

fun UserSession.bearerToken(): String =
    "${tokenType.ifBlank { "Bearer" }} $accessToken"
