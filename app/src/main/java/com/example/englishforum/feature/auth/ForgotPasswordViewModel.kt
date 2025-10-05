package com.example.englishforum.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.data.auth.AuthRepository
import com.example.englishforum.data.auth.FakeAuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(ForgotPasswordUiState())
        private set

    private var otpTimerJob: Job? = null

    fun onContactChange(value: String) {
        otpTimerJob?.cancel()
        otpTimerJob = null
        uiState = ForgotPasswordUiState(contact = value)
    }

    fun submit() {
        val contact = uiState.contact.trim()
        if (contact.isEmpty()) {
            uiState = uiState.copy(errorMessage = "Vui lòng nhập số điện thoại hoặc email")
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = authRepository.requestRecoveryOtp(contact)
            result.onSuccess {
                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = "Mã OTP đã được gửi đến $contact",
                    errorMessage = null,
                    isOtpRequested = true,
                    otp = "",
                    isOtpVerified = false,
                    otpErrorMessage = null,
                    newPassword = "",
                    confirmNewPassword = "",
                    passwordErrorMessage = null
                )
                startOtpCountdown()
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Không gửi được OTP",
                    successMessage = null,
                    isOtpRequested = false,
                    otp = "",
                    otpSecondsRemaining = 0
                )
            }
        }
    }

    fun onOtpChange(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(6)
        uiState = uiState.copy(otp = sanitized, otpErrorMessage = null)
        if (sanitized.length < OTP_LENGTH) {
            uiState = uiState.copy(isOtpVerified = false)
            return
        }
        verifyOtp()
    }

    fun verifyOtp() {
        val state = uiState
        if (!state.isOtpRequested) {
            uiState = state.copy(otpErrorMessage = "Vui lòng yêu cầu mã OTP trước")
            return
        }
        if (state.otp.length < OTP_LENGTH) {
            uiState = state.copy(otpErrorMessage = "Mã OTP phải gồm 6 số", isOtpVerified = false)
            return
        }

        viewModelScope.launch {
            val result = authRepository.verifyRecoveryOtp(state.otp)
            result.onSuccess { isValid ->
                if (isValid) {
                    uiState = uiState.copy(
                        isOtpVerified = true,
                        otpErrorMessage = null,
                        successMessage = "OTP hợp lệ, vui lòng đặt mật khẩu mới"
                    )
                } else {
                    uiState = uiState.copy(
                        isOtpVerified = false,
                        otpErrorMessage = "Mã OTP không đúng"
                    )
                }
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    isOtpVerified = false,
                    otpErrorMessage = throwable.message ?: "Xác thực OTP thất bại"
                )
            }
        }
    }

    fun onNewPasswordChange(value: String) {
        uiState = uiState.copy(newPassword = value, passwordErrorMessage = null)
    }

    fun onConfirmNewPasswordChange(value: String) {
        uiState = uiState.copy(confirmNewPassword = value, passwordErrorMessage = null)
    }

    fun changePassword(onSuccess: () -> Unit) {
        val state = uiState
        if (!state.isOtpVerified) {
            uiState = state.copy(passwordErrorMessage = "Vui lòng xác thực OTP trước")
            return
        }
        val trimmedPassword = state.newPassword.trim()
        val trimmedConfirm = state.confirmNewPassword.trim()
        if (trimmedPassword.length < 6) {
            uiState = state.copy(passwordErrorMessage = "Mật khẩu phải có ít nhất 6 ký tự")
            return
        }
        if (trimmedConfirm.isEmpty()) {
            uiState = state.copy(passwordErrorMessage = "Vui lòng xác nhận mật khẩu")
            return
        }
        if (trimmedPassword != trimmedConfirm) {
            uiState = state.copy(passwordErrorMessage = "Mật khẩu xác nhận không khớp")
            return
        }

        uiState = state.copy(isChangingPassword = true, passwordErrorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = authRepository.resetPassword(trimmedPassword)
            uiState = uiState.copy(isChangingPassword = false)
            result.onSuccess {
                uiState = uiState.copy(successMessage = "Mật khẩu của bạn đã được đổi")
                onSuccess()
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    passwordErrorMessage = throwable.message ?: "Đổi mật khẩu thất bại"
                )
            }
        }
    }

    fun clearMessages() {
        otpTimerJob?.cancel()
        otpTimerJob = null
        uiState = ForgotPasswordUiState()
    }

    private fun startOtpCountdown() {
        otpTimerJob?.cancel()
        otpTimerJob = viewModelScope.launch {
            uiState = uiState.copy(otpSecondsRemaining = OTP_COUNTDOWN_SECONDS)
            while (uiState.otpSecondsRemaining > 0) {
                delay(1000)
                val next = uiState.otpSecondsRemaining - 1
                uiState = uiState.copy(otpSecondsRemaining = next)
            }
            uiState = uiState.copy(otpSecondsRemaining = 0)
        }
    }

    companion object {
        private const val OTP_COUNTDOWN_SECONDS = 60
        private const val OTP_LENGTH = 6
    }
}

class ForgotPasswordViewModelFactory(
    private val authRepository: AuthRepository = FakeAuthRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) {
            return ForgotPasswordViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class ForgotPasswordUiState(
    val contact: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val otp: String = "",
    val isOtpRequested: Boolean = false,
    val otpErrorMessage: String? = null,
    val isOtpVerified: Boolean = false,
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val passwordErrorMessage: String? = null,
    val isChangingPassword: Boolean = false,
    val otpSecondsRemaining: Int = 0
)
