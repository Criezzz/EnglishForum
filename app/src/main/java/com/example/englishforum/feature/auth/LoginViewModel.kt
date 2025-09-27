package com.example.englishforum.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishforum.data.auth.AuthRepository
import com.example.englishforum.data.auth.FakeAuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = FakeAuthRepository()
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onUsernameChange(value: String) {
        uiState = uiState.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, error = null)
    }

    fun login(onSuccess: () -> Unit) {
        val username = uiState.username
        val password = uiState.password

        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.login(username, password)
            uiState = uiState.copy(isLoading = false)

            result.onSuccess {
                onSuccess()
            }.onFailure { throwable ->
                uiState = uiState.copy(error = throwable.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
