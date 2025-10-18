package com.example.englishforum.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.network.NetworkMonitor
import com.example.englishforum.data.auth.SessionValidationResult
import com.example.englishforum.data.auth.SessionValidator
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.UserSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_VALIDATION_INTERVAL_MS = 60_000L

class SessionMonitorViewModel(
    private val userSessionRepository: UserSessionRepository,
    private val sessionValidator: SessionValidator,
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val validationIntervalMs: Long = DEFAULT_VALIDATION_INTERVAL_MS
) : ViewModel() {

    private val _state = MutableStateFlow<SessionMonitorState>(SessionMonitorState.SignedOut)
    val state: StateFlow<SessionMonitorState> = _state.asStateFlow()

    init {
        observeSessionAndConnectivity()
    }

    private fun observeSessionAndConnectivity() {
        viewModelScope.launch {
            combine(
                userSessionRepository.sessionFlow,
                networkMonitor.isOnline,
                validationTickerFlow()
            ) { session, isOnline, _ -> session to isOnline }
                .collectLatest { (session, isOnline) ->
                    handleSessionState(session, isOnline)
                }
        }
    }

    private suspend fun handleSessionState(session: UserSession?, isOnline: Boolean) {
        if (session == null) {
            _state.value = SessionMonitorState.SignedOut
            return
        }

        if (!isOnline) {
            _state.value = SessionMonitorState.Offline
            return
        }

        _state.value = SessionMonitorState.Checking
        when (val result = sessionValidator.validate(session)) {
            SessionValidationResult.Valid -> {
                _state.value = SessionMonitorState.Valid
            }

            SessionValidationResult.Invalid -> {
                _state.value = SessionMonitorState.Invalidated
                withContext(ioDispatcher) {
                    userSessionRepository.clearSession()
                }
            }

            SessionValidationResult.Offline -> {
                _state.value = SessionMonitorState.Offline
            }

            is SessionValidationResult.Error -> {
                _state.value = SessionMonitorState.Error(result.message)
            }
        }
    }

    private fun validationTickerFlow(): Flow<Unit> = flow {
        emit(Unit)
        while (true) {
            delay(validationIntervalMs)
            emit(Unit)
        }
    }
}

sealed interface SessionMonitorState {
    data object SignedOut : SessionMonitorState
    data object Checking : SessionMonitorState
    data object Valid : SessionMonitorState
    data object Offline : SessionMonitorState
    data object Invalidated : SessionMonitorState
    data class Error(val message: String?) : SessionMonitorState
}

class SessionMonitorViewModelFactory(
    private val userSessionRepository: UserSessionRepository,
    private val sessionValidator: SessionValidator,
    private val networkMonitor: NetworkMonitor,
    private val validationIntervalMs: Long = DEFAULT_VALIDATION_INTERVAL_MS
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionMonitorViewModel::class.java)) {
            return SessionMonitorViewModel(
                userSessionRepository = userSessionRepository,
                sessionValidator = sessionValidator,
                networkMonitor = networkMonitor,
                validationIntervalMs = validationIntervalMs
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
