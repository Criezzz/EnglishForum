package com.example.englishforum.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onUsernameChange(value: String) {
        username = value
        clearError()
    }

    fun onPasswordChange(value: String) {
        password = value
        clearError()
    }

    // Mock login: succeed when username == "user" and password == "pass"
    fun login(onSuccess: () -> Unit) {
        errorMessage = null
        isLoading = true
        viewModelScope.launch {
            delay(900) // simulate network
            isLoading = false

            val u = username.trim()
            val p = password.trim()

            // accept common casing variations
            if (u.equals("user", ignoreCase = true) && p.equals("pass", ignoreCase = true)) {
                onSuccess()
            } else {
                errorMessage = "Tên đăng nhập hoặc mật khẩu không đúng"
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
