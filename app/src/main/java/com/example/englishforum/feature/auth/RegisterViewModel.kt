package com.example.englishforum.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    var name by mutableStateOf("")
        private set

    var phone by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var dob by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onNameChange(v: String) {
        name = v
        clearError()
    }

    fun onPhoneChange(v: String) {
        phone = v
        clearError()
    }

    fun onEmailChange(v: String) {
        email = v
        clearError()
    }

    fun onPasswordChange(v: String) {
        password = v
        clearError()
    }

    fun onConfirmPasswordChange(v: String) {
        confirmPassword = v
        clearError()
    }

    fun onDobChange(v: String) {
        dob = v
        clearError()
    }

    // allow UI to set an arbitrary error (e.g. DOB validation)
    fun setError(message: String) {
        errorMessage = message
    }

    private fun validate(): Boolean {
        if (name.trim().isEmpty()) {
            errorMessage = "Vui lòng nhập tên"
            return false
        }
        if (phone.trim().isEmpty()) {
            errorMessage = "Vui lòng nhập số điện thoại"
            return false
        }
        if (email.trim().isEmpty() || !email.contains("@")) {
            errorMessage = "Vui lòng nhập email hợp lệ"
            return false
        }
        if (password.trim().length < 6) {
            errorMessage = "Mật khẩu phải có ít nhất 6 ký tự"
            return false
        }
        if (confirmPassword.trim().isEmpty()) {
            errorMessage = "Vui lòng xác nhận mật khẩu"
            return false
        }
        if (password != confirmPassword) {
            errorMessage = "Mật khẩu xác nhận không khớp"
            return false
        }
        if (dob.trim().isEmpty()) {
            errorMessage = "Vui lòng chọn ngày sinh"
            return false
        }
        return true
    }

    fun register(onSuccess: () -> Unit) {
        if (!validate()) return
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            delay(900)
            isLoading = false
            // mock success
            onSuccess()
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
