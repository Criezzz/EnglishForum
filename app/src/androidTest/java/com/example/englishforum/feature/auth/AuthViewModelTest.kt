package com.example.englishforum.feature.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.data.auth.AuthRepository
import com.example.englishforum.data.auth.AuthResult
import com.example.englishforum.data.auth.SessionPreferenceRepository
import com.example.englishforum.data.auth.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun login_updatesKeepLoggedInPreference() = runTest {
        val authRepo = StubAuthRepository()
        val sessionPrefs = StubSessionPreferenceRepository(initial = false)
        val viewModel = LoginViewModel(authRepo, sessionPrefs)

        viewModel.onKeepLoggedInChange(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.keepLoggedIn)
        assertEquals(listOf(true), sessionPrefs.recordedValues)
    }

    @Test
    fun login_routesToVerificationWhenRequired() = runTest {
        val authRepo = StubAuthRepository(
            loginResult = Result.success(
                AuthResult(testSession(isVerified = false), requiresEmailVerification = true)
            )
        )
        val viewModel = LoginViewModel(authRepo, StubSessionPreferenceRepository())

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        var successCalled = false
        var verificationCalled = false

        viewModel.login(
            onSuccess = { successCalled = true },
            onRequiresVerification = { verificationCalled = true }
        )
        advanceUntilIdle()

        assertFalse(successCalled)
        assertTrue(verificationCalled)
        assertFalse(viewModel.uiState.isLoading)
        assertNull(viewModel.uiState.error)
    }

    @Test
    fun login_setsErrorOnFailure() = runTest {
        val authRepo = StubAuthRepository(
            loginResult = Result.failure(IllegalStateException("boom"))
        )
        val viewModel = LoginViewModel(authRepo, StubSessionPreferenceRepository())

        viewModel.login(onSuccess = {}, onRequiresVerification = {})
        advanceUntilIdle()

        assertEquals("boom", viewModel.uiState.error)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun register_withoutVerification_invokesSuccessCallback() = runTest {
        val authRepo = StubAuthRepository(
            registerResult = Result.success(
                AuthResult(testSession(isVerified = false), requiresEmailVerification = false)
            )
        )
        val viewModel = RegisterViewModel(authRepo)
        var verificationCalled = false
        var successCalled = false

        viewModel.onUsernameChange("newuser123")
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("StrongPass")
        viewModel.onConfirmPasswordChange("StrongPass")

        viewModel.register(
            onVerificationRequired = { verificationCalled = true },
            onRegistrationComplete = { successCalled = true }
        )
        advanceUntilIdle()

        assertTrue(successCalled)
        assertFalse(verificationCalled)
        assertEquals("Đăng ký thành công", viewModel.uiState.successMessage)
        assertFalse(viewModel.uiState.requiresVerification)
    }

    @Test
    fun emailVerification_clearMessages_resetsState() = runTest {
        val authRepo = StubAuthRepository(
            verifyEmailResult = Result.failure(IllegalArgumentException("otp sai"))
        )
        val viewModel = EmailVerificationViewModel(authRepo)

        viewModel.onOtpChange("000000")
        viewModel.verify(onSuccess = {})
        advanceUntilIdle()
        assertEquals("otp sai", viewModel.uiState.error)

        viewModel.clearMessages()

        assertNull(viewModel.uiState.error)
        assertNull(viewModel.uiState.message)
    }

    @Test
    fun emailVerification_resendSkipsDuringCountdown() = runTest {
        val viewModel = EmailVerificationViewModel(StubAuthRepository())

        viewModel.resend()
        advanceUntilIdle()
        val initialMessage = viewModel.uiState.message
        val initialSeconds = viewModel.uiState.secondsRemaining

        viewModel.resend()
        advanceUntilIdle()

        assertEquals(initialSeconds, viewModel.uiState.secondsRemaining)
        assertEquals(initialMessage, viewModel.uiState.message)

        advanceTimeBy(60_000)
        advanceUntilIdle()
    }

    @Test
    fun forgotPassword_verifyOtpWithoutRequest_showsError() = runTest {
        val viewModel = ForgotPasswordViewModel(StubAuthRepository())

        viewModel.verifyOtp()
        advanceUntilIdle()

        assertEquals("Vui lòng yêu cầu mã OTP trước", viewModel.uiState.otpErrorMessage)
    }

    @Test
    fun forgotPassword_changePasswordWithoutVerification_showsError() = runTest {
        val viewModel = ForgotPasswordViewModel(StubAuthRepository())

        viewModel.changePassword {}
        advanceUntilIdle()

        assertEquals("Vui lòng xác thực OTP trước", viewModel.uiState.passwordErrorMessage)
    }

    @Test
    fun forgotPassword_fullFlow_changesPassword() = runTest {
        val authRepo = StubAuthRepository(
            requestRecoveryResult = Result.success(Unit),
            verifyRecoveryResult = Result.success("reset-token"),
            resetPasswordResult = Result.success(Unit)
        )
        val viewModel = ForgotPasswordViewModel(authRepo)
        var successCalled = false

        viewModel.onContactChange("user@example.com")
        viewModel.submit()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.isOtpRequested)

        viewModel.onOtpChange("000000")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.isOtpVerified)
        assertEquals("reset-token", viewModel.uiState.resetToken)

        viewModel.onNewPasswordChange("NewPass123")
        viewModel.onConfirmNewPasswordChange("NewPass123")
        viewModel.changePassword { successCalled = true }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals("Mật khẩu của bạn đã được đổi", viewModel.uiState.successMessage)

        advanceTimeBy(60_000)
        advanceUntilIdle()
    }
}

private class StubAuthRepository(
    private val loginResult: Result<AuthResult> = Result.success(
        AuthResult(testSession(), requiresEmailVerification = false)
    ),
    private val registerResult: Result<AuthResult> = Result.success(
        AuthResult(testSession(), requiresEmailVerification = true)
    ),
    private val verifyEmailResult: Result<Unit> = Result.success(Unit),
    private val resendResult: Result<Unit> = Result.success(Unit),
    private val requestRecoveryResult: Result<Unit> = Result.success(Unit),
    private val verifyRecoveryResult: Result<String> = Result.success("reset-token"),
    private val resetPasswordResult: Result<Unit> = Result.success(Unit)
) : AuthRepository {
    override suspend fun login(username: String, password: String): Result<AuthResult> = loginResult

    override suspend fun register(username: String, email: String, password: String): Result<AuthResult> =
        registerResult

    override suspend fun verifyEmail(otp: String): Result<Unit> = verifyEmailResult

    override suspend fun resendVerificationOtp(): Result<Unit> = resendResult

    override suspend fun requestRecoveryOtp(contact: String): Result<Unit> = requestRecoveryResult

    override suspend fun verifyRecoveryOtp(contact: String, code: String): Result<String> =
        verifyRecoveryResult

    override suspend fun resetPassword(resetToken: String, newPassword: String): Result<Unit> =
        resetPasswordResult

    override suspend fun refreshSession(session: UserSession): Result<UserSession> =
        Result.success(session.copy(accessToken = "refreshed-token"))
}

private class StubSessionPreferenceRepository(initial: Boolean = false) : SessionPreferenceRepository {
    private val flow = MutableStateFlow(initial)
    val recordedValues = mutableListOf<Boolean>()

    override val keepLoggedInFlow: Flow<Boolean> = flow

    override suspend fun setKeepLoggedIn(enabled: Boolean) {
        recordedValues += enabled
        flow.value = enabled
    }
}

class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private fun testSession(isVerified: Boolean = true) = UserSession(
    userId = "id",
    username = "user",
    accessToken = "access",
    refreshToken = "refresh",
    tokenType = "Bearer",
    isEmailVerified = isVerified
)
