package com.example.englishforum.data.auth

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<UserSession>

    suspend fun requestOtp(contact: String): Result<Unit>

    suspend fun verifyOtp(code: String): Result<Boolean>

    suspend fun resetPassword(newPassword: String): Result<Unit>
}
