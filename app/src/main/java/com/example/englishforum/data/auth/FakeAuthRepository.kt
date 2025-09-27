package com.example.englishforum.data.auth

import kotlinx.coroutines.delay

class FakeAuthRepository : AuthRepository {

    override suspend fun login(username: String, password: String): Result<UserSession> {
        delay(900)
        val normalizedUsername = username.trim()
        val normalizedPassword = password.trim()
        return if (normalizedUsername.equals("user", ignoreCase = true) &&
            normalizedPassword.equals("pass", ignoreCase = true)
        ) {
            Result.success(
                UserSession(
                    userId = "demo-user",
                    username = normalizedUsername.ifEmpty { "user" },
                    token = "fake-token"
                )
            )
        } else {
            Result.failure(IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không đúng"))
        }
    }

    override suspend fun requestOtp(contact: String): Result<Unit> {
        delay(800)
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

    override suspend fun verifyOtp(code: String): Result<Boolean> {
        delay(500)
        val sanitizedCode = code.filter { it.isDigit() }
        if (sanitizedCode.length < VALID_OTP.length) {
            return Result.failure(IllegalArgumentException("Mã OTP phải gồm 6 số"))
        }
        return Result.success(sanitizedCode == VALID_OTP)
    }

    override suspend fun resetPassword(newPassword: String): Result<Unit> {
        delay(800)
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
