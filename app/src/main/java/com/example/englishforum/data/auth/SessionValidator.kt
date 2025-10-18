package com.example.englishforum.data.auth

sealed interface SessionValidationResult {
    data object Valid : SessionValidationResult
    data object Invalid : SessionValidationResult
    data object Offline : SessionValidationResult
    data class Error(val message: String? = null) : SessionValidationResult
}

interface SessionValidator {
    suspend fun validate(session: UserSession): SessionValidationResult
}
