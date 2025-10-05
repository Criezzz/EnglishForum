package com.example.englishforum.data.auth

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<AuthResult>

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<AuthResult>

    suspend fun verifyEmail(otp: String): Result<Unit>

    suspend fun resendVerificationOtp(): Result<Unit>

    suspend fun requestRecoveryOtp(contact: String): Result<Unit>

    suspend fun verifyRecoveryOtp(code: String): Result<Boolean>

    suspend fun resetPassword(newPassword: String): Result<Unit>
}

data class AuthResult(
    val session: UserSession,
    val requiresEmailVerification: Boolean
)
