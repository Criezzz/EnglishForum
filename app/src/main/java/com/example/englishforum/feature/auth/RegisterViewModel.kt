package com.example.englishforum.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

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

    fun register(onSuccess: () -> Unit) {
        if (!validate()) return
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            delay(900)
            uiState = uiState.copy(isLoading = false)
            onSuccess()
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
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
    val errorMessage: String? = null
)
