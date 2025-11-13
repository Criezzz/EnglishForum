package com.example.englishforum.data.auth.remote

import android.util.Base64
import com.example.englishforum.data.auth.AuthRepository
import com.example.englishforum.data.auth.AuthResult
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
import com.example.englishforum.data.auth.remote.model.TokenResponse
import com.example.englishforum.data.auth.bearerToken
import com.example.englishforum.data.profile.remote.ProfileApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.filterNotNull
import org.json.JSONObject
import retrofit2.HttpException
import java.nio.charset.StandardCharsets

class RemoteAuthRepository(
    private val authApi: AuthApi,
    private val userSessionRepository: UserSessionRepository,
    private val profileApi: ProfileApi
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<AuthResult> = runCatching {
        val response = authApi.login(username = username.trim(), password = password)
        val baseSession = response.toSession(
            fallbackUsername = username.trim(),
            isEmailVerified = true
        )
        val verificationStatus = determineVerificationStatus(baseSession)
        val resolvedSession = when (verificationStatus) {
            VerificationStatus.Verified -> baseSession.copy(isEmailVerified = true)
            VerificationStatus.RequiresVerification -> baseSession.copy(isEmailVerified = false)
            VerificationStatus.Unknown -> baseSession
        }
        userSessionRepository.saveSession(resolvedSession)
        AuthResult(
            session = resolvedSession,
            requiresEmailVerification = verificationStatus == VerificationStatus.RequiresVerification
        )
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
        val session = userSessionRepository.sessionFlow.filterNotNull().firstOrNull()
            ?: throw IllegalStateException("Bạn chưa đăng nhập")
        authApi.verifyEmail(bearer = session.bearerToken(), otp = otp.trim())
        userSessionRepository.markEmailVerified()
        Unit
    }.recoverHttpFailure()

    override suspend fun resendVerificationOtp(): Result<Unit> = runCatching {
        val session = userSessionRepository.sessionFlow.filterNotNull().firstOrNull()
            ?: throw IllegalStateException("Bạn chưa đăng nhập")
        authApi.resendVerification(bearer = session.bearerToken())
        Unit
    }.recoverHttpFailure()

    override suspend fun requestRecoveryOtp(contact: String): Result<Unit> = runCatching {
        authApi.requestPasswordRecovery(contact.trim())
        Unit
    }.recoverHttpFailure()

    override suspend fun verifyRecoveryOtp(contact: String, code: String): Result<String> = runCatching {
        val response = authApi.verifyRecoveryOtp(
            otp = code.trim(),
            contact = contact.trim()
        )
        response.resetToken
    }.recoverHttpFailure()

    override suspend fun resetPassword(resetToken: String, newPassword: String): Result<Unit> = runCatching {
        authApi.resetPassword(
            resetToken = resetToken,
            newPassword = newPassword.trim()
        )
        Unit
    }.recoverHttpFailure()

    override suspend fun refreshSession(session: UserSession): Result<UserSession> = runCatching {
        if (session.refreshToken.isBlank()) {
            throw IllegalStateException("Không tìm thấy refresh token")
        }
        val response = authApi.refreshAccessToken(refreshToken = session.refreshToken)
        val refreshedSession = session.copy(accessToken = response.accessToken)
        userSessionRepository.saveSession(refreshedSession)
        refreshedSession
    }.recoverHttpFailure()

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
                val errorMsg = json.optString("detail", json.optString("message", raw))
                translateErrorMessage(errorMsg)
            }
        } catch (_: Exception) {
            translateErrorMessage(raw ?: "Đã xảy ra lỗi, vui lòng thử lại")
        }
    }

    private fun translateErrorMessage(message: String): String {
        return when {
            message.contains("User not found", ignoreCase = true) -> "Tài khoản không tồn tại"
            message.contains("password must be at least 8", ignoreCase = true) -> "Mật khẩu phải có ít nhất 8 ký tự"
            message.contains("password must be at least 6", ignoreCase = true) -> "Mật khẩu phải có ít nhất 8 ký tự"
            message.contains("Invalid credentials", ignoreCase = true) -> "Tên đăng nhập hoặc mật khẩu không đúng"
            message.contains("username already exists", ignoreCase = true) -> "Tên đăng nhập đã tồn tại"
            message.contains("email already exists", ignoreCase = true) -> "Email đã được sử dụng"
            message.contains("Invalid OTP", ignoreCase = true) -> "Mã OTP không hợp lệ"
            message.contains("OTP expired", ignoreCase = true) -> "Mã OTP đã hết hạn"
            else -> message
        }
    }

    private suspend fun determineVerificationStatus(session: UserSession): VerificationStatus {
        return runCatching {
            profileApi.getCurrentUser(session.bearerToken())
        }.fold(
            onSuccess = { VerificationStatus.Verified },
            onFailure = { throwable ->
                if (throwable is HttpException && throwable.code() == 401) {
                    val raw = throwable.response()?.errorBody()?.string()
                    val message = parseErrorMessage(raw)
                    return if (raw.containsVerificationHint() || message.containsVerificationHint()) {
                        VerificationStatus.RequiresVerification
                    } else {
                        VerificationStatus.Unknown
                    }
                }
                VerificationStatus.Unknown
            }
        )
    }

    private fun String?.containsVerificationHint(): Boolean {
        if (this.isNullOrBlank()) return false
        return contains("verify your email", ignoreCase = true)
    }

    private enum class VerificationStatus {
        Verified,
        RequiresVerification,
        Unknown
    }
}
