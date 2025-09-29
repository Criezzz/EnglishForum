package com.example.englishforum.data.auth

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<UserSession>

    suspend fun register(
        name: String,
        phone: String, 
        email: String,
        password: String,
        dob: String
    ): Result<UserSession>

    suspend fun requestOtp(contact: String): Result<Unit>

    suspend fun verifyOtp(code: String): Result<Boolean>

    suspend fun resetPassword(newPassword: String): Result<Unit>
}
