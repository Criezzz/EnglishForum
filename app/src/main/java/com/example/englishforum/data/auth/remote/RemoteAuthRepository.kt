package com.example.englishforum.data.auth.remote

import android.util.Base64
import com.example.englishforum.data.auth.AuthRepository
import com.example.englishforum.data.auth.AuthResult
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.remote.model.TokenResponse
import com.example.englishforum.data.auth.bearerToken
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject
import retrofit2.HttpException
import java.nio.charset.StandardCharsets

class RemoteAuthRepository(
    private val authApi: AuthApi,
    private val userSessionRepository: UserSessionRepository
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<AuthResult> = runCatching {
        val response = authApi.login(username = username.trim(), password = password)
        val session = response.toSession(
            fallbackUsername = username.trim(),
            isEmailVerified = true
        )
        userSessionRepository.saveSession(session)
        AuthResult(session = session, requiresEmailVerification = false)
    }.recoverHttpFailure()

    override suspend fun register(username: String, email: String, password: String): Result<AuthResult> = runCatching {
        val response = authApi.register(
            username = username.trim(),
            password = password,
            email = email.trim()
        )
        val session = response.toSession(
            fallbackUsername = username.trim(),
            isEmailVerified = false
        )
        userSessionRepository.saveSession(session)
        AuthResult(session = session, requiresEmailVerification = true)
    }.recoverHttpFailure()

    override suspend fun verifyEmail(otp: String): Result<Unit> = runCatching {
        val session = userSessionRepository.sessionFlow.firstOrNull()
            ?: throw IllegalStateException("Bạn chưa đăng nhập")
        authApi.verifyEmail(bearer = session.bearerToken(), otp = otp.trim())
        userSessionRepository.markEmailVerified()
        Unit
    }.recoverHttpFailure()

    override suspend fun resendVerificationOtp(): Result<Unit> = runCatching {
        val session = userSessionRepository.sessionFlow.firstOrNull()
            ?: throw IllegalStateException("Bạn chưa đăng nhập")
        authApi.resendVerification(bearer = session.bearerToken())
        Unit
    }.recoverHttpFailure()

    override suspend fun requestRecoveryOtp(contact: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Khôi phục mật khẩu sẽ được cập nhật sau"))

    override suspend fun verifyRecoveryOtp(code: String): Result<Boolean> =
        Result.failure(UnsupportedOperationException("Khôi phục mật khẩu sẽ được cập nhật sau"))

    override suspend fun resetPassword(newPassword: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Khôi phục mật khẩu sẽ được cập nhật sau"))

    private fun TokenResponse.toSession(
        fallbackUsername: String,
        isEmailVerified: Boolean
    ): UserSession {
        val resolvedTokenType = tokenType?.ifBlank { "Bearer" } ?: "Bearer"
        val userId = accessToken.extractJwtField("sub") ?: fallbackUsername
        val resolvedUsername = accessToken.extractJwtField("username") ?: fallbackUsername
        return UserSession(
            userId = userId,
            username = resolvedUsername,
            accessToken = accessToken,
            refreshToken = refreshToken.orEmpty(),
            tokenType = resolvedTokenType,
            isEmailVerified = isEmailVerified
        )
    }

    private fun String.extractJwtField(key: String): String? {
        return try {
            val segments = split('.')
            if (segments.size < 2) {
                null
            } else {
                val payload = segments[1]
                val padding = (4 - payload.length % 4) % 4
                val padded = payload + "=".repeat(padding)
                val decoded = Base64.decode(padded, Base64.URL_SAFE)
                val json = JSONObject(String(decoded, StandardCharsets.UTF_8))
                json.optString(key, null)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun <T> Result<T>.recoverHttpFailure(): Result<T> {
        return fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable ->
                if (throwable is HttpException) {
                    val errorMessage = throwable.response()?.errorBody()?.string()
                    Result.failure(IllegalStateException(parseErrorMessage(errorMessage), throwable))
                } else {
                    Result.failure(throwable)
                }
            }
        )
    }

    private fun parseErrorMessage(raw: String?): String {
        return try {
            if (raw.isNullOrBlank()) {
                "Đã xảy ra lỗi, vui lòng thử lại"
            } else {
                val json = JSONObject(raw)
                json.optString("detail", json.optString("message", raw))
            }
        } catch (_: Exception) {
            raw ?: "Đã xảy ra lỗi, vui lòng thử lại"
        }
    }
}
