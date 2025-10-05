package com.example.englishforum.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.data.auth.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onUsernameChange(value: String) {
        uiState = uiState.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, error = null)
    }

    fun login(
        onSuccess: () -> Unit,
        onRequiresVerification: () -> Unit
    ) {
        val username = uiState.username
        val password = uiState.password

        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.login(username, password)
            result
                .onSuccess { authResult ->
                    uiState = uiState.copy(isLoading = false)
                    if (authResult.requiresEmailVerification) {
                        onRequiresVerification()
                    } else {
                        onSuccess()
                    }
                }.onFailure { throwable ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = throwable.message ?: "Đăng nhập thất bại"
                    )
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

class LoginViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
