package com.example.englishforum.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.data.auth.AuthRepository
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

    fun onUsernameChange(value: String) {
        uiState = uiState.copy(username = value, errorMessage = null)
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

    fun setError(message: String) {
        uiState = uiState.copy(errorMessage = message)
    }

    private fun validate(): Boolean {
        val state = uiState
        if (state.username.trim().length < 8) {
            uiState = state.copy(errorMessage = "Tên đăng nhập phải có ít nhất 8 ký tự")
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
        return true
    }

    fun register(
        onVerificationRequired: () -> Unit,
        onRegistrationComplete: () -> Unit
    ) {
        if (!validate()) return
        uiState = uiState.copy(isLoading = true, errorMessage = null, successMessage = null)

        viewModelScope.launch {
            val result = authRepository.register(
                username = uiState.username,
                email = uiState.email,
                password = uiState.password
            )

            result
                .onSuccess { registerResult ->
                    uiState = uiState.copy(
                        isLoading = false,
                        isRegistrationComplete = true,
                        requiresVerification = registerResult.requiresEmailVerification,
                        successMessage = if (registerResult.requiresEmailVerification) {
                            "Tài khoản đã được tạo. Vui lòng xác minh email để tiếp tục"
                        } else {
                            "Đăng ký thành công"
                        }
                    )
                    if (registerResult.requiresEmailVerification) {
                        onVerificationRequired()
                    } else {
                        onRegistrationComplete()
                    }
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Đăng ký thất bại",
                        successMessage = null
                    )
                }
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null, successMessage = null)
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
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isRegistrationComplete: Boolean = false,
    val requiresVerification: Boolean = false
)
