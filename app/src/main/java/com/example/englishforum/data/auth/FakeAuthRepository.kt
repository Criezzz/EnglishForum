package com.example.englishforum.data.auth

class FakeAuthRepository(
    private val userSessionRepository: UserSessionRepository? = null
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<AuthResult> {
        kotlinx.coroutines.delay(900)
        val normalizedUsername = username.trim()
        val normalizedPassword = password.trim()
        return if (normalizedUsername.equals("user", ignoreCase = true) &&
            normalizedPassword.equals("pass", ignoreCase = true)
        ) {
            val session = UserSession(
                userId = "demo-user",
                username = normalizedUsername.ifEmpty { "user" },
                accessToken = "fake-access-token",
                refreshToken = "fake-refresh-token",
                tokenType = "Bearer",
                isEmailVerified = true
            )
            userSessionRepository?.saveSession(session)
            Result.success(AuthResult(session, requiresEmailVerification = false))
        } else {
            Result.failure(IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng"))
        }
    }

    override suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<AuthResult> {
        kotlinx.coroutines.delay(1000)
        val normalizedUsername = username.trim()
        val normalizedEmail = email.trim()
        val normalizedPassword = password.trim()

        if (normalizedUsername.length < 8) {
            return Result.failure(IllegalArgumentException("Tên đăng nhập phải có ít nhất 8 ký tự"))
        }
        if (normalizedEmail.isEmpty() || !normalizedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Email không hợp lệ"))
        }
        if (normalizedPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự"))
        }

        val session = UserSession(
            userId = "user-${System.currentTimeMillis()}",
            username = normalizedUsername,
            accessToken = "fake-access-token-${System.currentTimeMillis()}",
            refreshToken = "fake-refresh-token-${System.currentTimeMillis()}",
            tokenType = "Bearer",
            isEmailVerified = false
        )
        userSessionRepository?.saveSession(session)
        return Result.success(AuthResult(session, requiresEmailVerification = true))
    }

    override suspend fun verifyEmail(otp: String): Result<Unit> {
        kotlinx.coroutines.delay(500)
        return if (otp.trim() == VALID_OTP) {
            userSessionRepository?.markEmailVerified()
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Mã OTP không đúng"))
        }
    }

    override suspend fun resendVerificationOtp(): Result<Unit> {
        kotlinx.coroutines.delay(400)
        return Result.success(Unit)
    }

    override suspend fun requestRecoveryOtp(contact: String): Result<Unit> {
        kotlinx.coroutines.delay(800)
        val normalizedContact = contact.trim()
        if (normalizedContact.isEmpty()) {
            return Result.failure(IllegalArgumentException("Vui lòng nhập số điện thoại hoặc email"))
        }
        val isPhone = normalizedContact.all { it.isDigit() }
        val isEmail = normalizedContact.contains("@")
        return if (isPhone || isEmail) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Thông tin liên hệ không hợp lệ"))
        }
    }

    override suspend fun verifyRecoveryOtp(code: String): Result<Boolean> {
        kotlinx.coroutines.delay(500)
        val sanitizedCode = code.filter { it.isDigit() }
        if (sanitizedCode.length < VALID_OTP.length) {
            return Result.failure(IllegalArgumentException("Mã OTP phải gồm 6 số"))
        }
        return Result.success(sanitizedCode == VALID_OTP)
    }

    override suspend fun resetPassword(newPassword: String): Result<Unit> {
        kotlinx.coroutines.delay(800)
        val normalizedPassword = newPassword.trim()
        return if (normalizedPassword.length >= 6) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự"))
        }
    }

    private companion object {
        const val VALID_OTP = "000000"
    }
}
