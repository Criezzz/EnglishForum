package com.example.englishforum.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.data.auth.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EmailVerificationViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(EmailVerificationUiState())
        private set

    private var countdownJob: Job? = null

    fun onOtpChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(OTP_LENGTH)
        uiState = uiState.copy(otp = digitsOnly, error = null, message = null)
    }

    fun verify(onSuccess: () -> Unit) {
        val otp = uiState.otp
        if (otp.length < OTP_LENGTH) {
            uiState = uiState.copy(error = "Mã OTP phải gồm 6 số")
            return
        }
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.verifyEmail(otp)
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isLoading = false,
                        isVerified = true,
                        message = "Email của bạn đã được xác minh"
                    )
                    onSuccess()
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = throwable.message ?: "Xác minh thất bại"
                    )
                }
        }
    }

    fun resend() {
        if (uiState.secondsRemaining > 0) return
        uiState = uiState.copy(isLoading = true, error = null, message = null)
        viewModelScope.launch {
            val result = authRepository.resendVerificationOtp()
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isLoading = false,
                        message = "Mã OTP mới đã được gửi",
                        otp = ""
                    )
                    startCountdown()
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = throwable.message ?: "Không gửi được OTP"
                    )
                }
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(error = null, message = null)
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            uiState = uiState.copy(secondsRemaining = RESEND_INTERVAL_SECONDS)
            while (uiState.secondsRemaining > 0) {
                delay(1000)
                uiState = uiState.copy(secondsRemaining = uiState.secondsRemaining - 1)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EmailVerificationViewModel::class.java)) {
                return EmailVerificationViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        private const val OTP_LENGTH = 6
        private const val RESEND_INTERVAL_SECONDS = 60
    }
}

data class EmailVerificationUiState(
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val secondsRemaining: Int = 0,
    val isVerified: Boolean = false
)
