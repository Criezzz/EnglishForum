package com.example.englishforum.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserSessionRepository : UserSessionRepository {
    private val _sessionFlow = MutableStateFlow<UserSession?>(null)
    override val sessionFlow: StateFlow<UserSession?> = _sessionFlow.asStateFlow()

    override suspend fun saveSession(session: UserSession) {
        _sessionFlow.value = session
    }

    override suspend fun clearSession() {
        _sessionFlow.value = null
    }

    override suspend fun markEmailVerified() {
        _sessionFlow.value?.let { session ->
            _sessionFlow.value = session.copy(isEmailVerified = true)
        }
    }

    // Helper for tests
    fun setSession(session: UserSession) {
        _sessionFlow.value = session
    }
}

