package com.example.englishforum.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {
    var contact by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var otp by mutableStateOf("")
        private set

    var isOtpRequested by mutableStateOf(false)
        private set

    var otpErrorMessage by mutableStateOf<String?>(null)
        private set

    var isOtpVerified by mutableStateOf(false)
        private set

    var newPassword by mutableStateOf("")
        private set

    var confirmNewPassword by mutableStateOf("")
        private set

    var passwordErrorMessage by mutableStateOf<String?>(null)
        private set

    var isChangingPassword by mutableStateOf(false)
        private set

    var otpSecondsRemaining by mutableStateOf(0)
        private set

    private var otpTimerJob: Job? = null

    fun onContactChange(v: String) {
        contact = v
        errorMessage = null
        successMessage = null
        isOtpRequested = false
        otp = ""
        otpErrorMessage = null
        isOtpVerified = false
        newPassword = ""
        confirmNewPassword = ""
        passwordErrorMessage = null
        otpTimerJob?.cancel()
        otpTimerJob = null
        otpSecondsRemaining = 0
    }

    fun submit() {
        val value = contact.trim()
        if (value.isEmpty()) {
            errorMessage = "Vui lòng nhập số điện thoại hoặc email"
            return
        }

        // simple validation: if contains @ treat as email, else phone
        isLoading = true
        errorMessage = null
        successMessage = null

        viewModelScope.launch {
            delay(800) // simulate network
            isLoading = false
            // mock success
            successMessage = "Mã OTP đã được gửi đến $value"
            isOtpRequested = true
            otp = ""
            isOtpVerified = false
            newPassword = ""
            confirmNewPassword = ""
            passwordErrorMessage = null
            otpErrorMessage = null
            startOtpCountdown()
        }
    }

    fun onOtpChange(value: String) {
        otp = value.filter { it.isDigit() }.take(6)
        otpErrorMessage = null
        if (otp.length < VALID_OTP.length) {
            isOtpVerified = false
        }
        if (otp.length == VALID_OTP.length) {
            verifyOtp()
        }
    }

    fun verifyOtp() {
        if (!isOtpRequested) {
            otpErrorMessage = "Vui lòng yêu cầu mã OTP trước"
            return
        }
        if (otp.length < 6) {
            otpErrorMessage = "Mã OTP phải gồm 6 số"
            return
        }

        if (otp != VALID_OTP) {
            otpErrorMessage = "Mã OTP không đúng"
            isOtpVerified = false
            return
        }

        isOtpVerified = true
        otpErrorMessage = null
        successMessage = "OTP hợp lệ, vui lòng đặt mật khẩu mới"
    }

    fun onNewPasswordChange(value: String) {
        newPassword = value
        passwordErrorMessage = null
    }

    fun onConfirmNewPasswordChange(value: String) {
        confirmNewPassword = value
        passwordErrorMessage = null
    }

    fun changePassword(onSuccess: () -> Unit) {
        if (!isOtpVerified) {
            passwordErrorMessage = "Vui lòng xác thực OTP trước"
            return
        }
        val trimmedPassword = newPassword.trim()
        val trimmedConfirm = confirmNewPassword.trim()
        if (trimmedPassword.length < 6) {
            passwordErrorMessage = "Mật khẩu phải có ít nhất 6 ký tự"
            return
        }
        if (trimmedConfirm.isEmpty()) {
            passwordErrorMessage = "Vui lòng xác nhận mật khẩu"
            return
        }
        if (trimmedPassword != trimmedConfirm) {
            passwordErrorMessage = "Mật khẩu xác nhận không khớp"
            return
        }

        isChangingPassword = true
        passwordErrorMessage = null
        viewModelScope.launch {
            delay(800)
            isChangingPassword = false
            successMessage = "Mật khẩu của bạn đã được đổi"
            onSuccess()
        }
    }

    fun clearMessages() {
        successMessage = null
        errorMessage = null
        isOtpRequested = false
        otp = ""
        otpErrorMessage = null
        isOtpVerified = false
        newPassword = ""
        confirmNewPassword = ""
        passwordErrorMessage = null
        otpTimerJob?.cancel()
        otpTimerJob = null
        otpSecondsRemaining = 0
    }

    private fun startOtpCountdown() {
        otpTimerJob?.cancel()
        otpSecondsRemaining = OTP_COUNTDOWN_SECONDS
        otpTimerJob = viewModelScope.launch {
            while (otpSecondsRemaining > 0) {
                delay(1000)
                otpSecondsRemaining -= 1
            }
            otpTimerJob = null
        }
    }

    companion object {
        private const val VALID_OTP = "000000"
        private const val OTP_COUNTDOWN_SECONDS = 60
    }
}
