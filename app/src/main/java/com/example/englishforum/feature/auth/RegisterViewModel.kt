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

class RegisterViewModel(
    private val authRepository: AuthRepository = FakeAuthRepository()
) : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

    private var otpTimerJob: Job? = null

    fun onNameChange(value: String) {
        uiState = uiState.copy(name = value, errorMessage = null)
    }

    fun onPhoneChange(value: String) {
        uiState = uiState.copy(phone = value, errorMessage = null)
    }

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, errorMessage = null)
    }

    fun onConfirmPasswordChange(value: String) {
        uiState = uiState.copy(confirmPassword = value, errorMessage = null)
    }

    fun onDobChange(value: String) {
        uiState = uiState.copy(dob = value, errorMessage = null)
    }

    fun setError(message: String) {
        uiState = uiState.copy(errorMessage = message)
    }

    private fun validate(): Boolean {
        val state = uiState
        if (state.name.trim().isEmpty()) {
            uiState = state.copy(errorMessage = "Vui lòng nhập tên")
            return false
        }
        if (state.phone.trim().isEmpty()) {
            uiState = state.copy(errorMessage = "Vui lòng nhập số điện thoại")
            return false
        }
        if (state.email.trim().isEmpty() || !state.email.contains("@")) {
            uiState = state.copy(errorMessage = "Vui lòng nhập email hợp lệ")
            return false
        }
        if (state.password.trim().length < 6) {
            uiState = state.copy(errorMessage = "Mật khẩu phải có ít nhất 6 ký tự")
            return false
        }
        if (state.confirmPassword.trim().isEmpty()) {
            uiState = state.copy(errorMessage = "Vui lòng xác nhận mật khẩu")
            return false
        }
        if (state.password != state.confirmPassword) {
            uiState = state.copy(errorMessage = "Mật khẩu xác nhận không khớp")
            return false
        }
        if (state.dob.trim().isEmpty()) {
            uiState = state.copy(errorMessage = "Vui lòng chọn ngày sinh")
            return false
        }
        return true
    }

    fun register() {
        if (!validate()) return
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        
        // First step: request OTP
        viewModelScope.launch {
            val result = authRepository.requestOtp(uiState.email)
            result.onSuccess {
                uiState = uiState.copy(
                    isLoading = false,
                    isOtpRequested = true,
                    successMessage = "Mã OTP đã được gửi đến ${uiState.email}",
                    errorMessage = null,
                    otp = "",
                    otpErrorMessage = null
                )
                startOtpCountdown()
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Không gửi được OTP"
                )
            }
        }
    }

    fun onOtpChange(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(6)
        uiState = uiState.copy(otp = sanitized, otpErrorMessage = null)
        if (sanitized.length < OTP_LENGTH) {
            return
        }
        verifyOtpAndRegister()
    }

    private fun verifyOtpAndRegister() {
        val state = uiState
        if (!state.isOtpRequested) {
            uiState = state.copy(otpErrorMessage = "Vui lòng yêu cầu mã OTP trước")
            return
        }
        if (state.otp.length < OTP_LENGTH) {
            uiState = state.copy(otpErrorMessage = "Mã OTP phải gồm 6 số")
            return
        }

        uiState = uiState.copy(isRegistering = true, otpErrorMessage = null)
        viewModelScope.launch {
            // First verify OTP
            val otpResult = authRepository.verifyOtp(state.otp)
            otpResult.onSuccess { isValid ->
                if (isValid) {
                    // OTP is valid, proceed with registration
                    val registerResult = authRepository.register(
                        name = state.name,
                        phone = state.phone,
                        email = state.email,
                        password = state.password,
                        dob = state.dob
                    )
                    registerResult.onSuccess { session ->
                        uiState = uiState.copy(
                            isRegistering = false,
                            isRegistrationComplete = true,
                            successMessage = "Đăng ký thành công!"
                        )
                        // Navigation will be handled by the composable observing isRegistrationComplete
                    }.onFailure { throwable ->
                        uiState = uiState.copy(
                            isRegistering = false,
                            otpErrorMessage = throwable.message ?: "Đăng ký thất bại"
                        )
                    }
                } else {
                    uiState = uiState.copy(
                        isRegistering = false,
                        otpErrorMessage = "Mã OTP không đúng"
                    )
                }
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    isRegistering = false,
                    otpErrorMessage = throwable.message ?: "Xác thực OTP thất bại"
                )
            }
        }
    }

    fun requestNewOtp() {
        if (uiState.otpSecondsRemaining > 0) return
        
        uiState = uiState.copy(isLoading = true, otpErrorMessage = null)
        viewModelScope.launch {
            val result = authRepository.requestOtp(uiState.email)
            result.onSuccess {
                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = "Mã OTP mới đã được gửi đến ${uiState.email}",
                    otp = "",
                    otpErrorMessage = null
                )
                startOtpCountdown()
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    isLoading = false,
                    otpErrorMessage = throwable.message ?: "Không gửi được OTP"
                )
            }
        }
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

    fun clearError() {
        uiState = uiState.copy(errorMessage = null, otpErrorMessage = null)
    }

    private companion object {
        private const val OTP_COUNTDOWN_SECONDS = 60
        private const val OTP_LENGTH = 6
    }
}

class RegisterViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

data class RegisterUiState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val dob: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val otp: String = "",
    val isOtpRequested: Boolean = false,
    val otpErrorMessage: String? = null,
    val isRegistering: Boolean = false,
    val isRegistrationComplete: Boolean = false,
    val otpSecondsRemaining: Int = 0
)
