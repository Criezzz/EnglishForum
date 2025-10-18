package com.example.englishforum.data.auth.remote

import com.example.englishforum.data.auth.SessionValidationResult
import com.example.englishforum.data.auth.SessionValidator
import com.example.englishforum.data.auth.UserSession
import com.example.englishforum.data.auth.bearerToken
import com.example.englishforum.data.profile.remote.ProfileApi
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RemoteSessionValidator(
    private val profileApi: ProfileApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SessionValidator {

    override suspend fun validate(session: UserSession): SessionValidationResult {
        return withContext(ioDispatcher) {
            runCatching {
                profileApi.getCurrentUser(session.bearerToken())
            }.fold(
                onSuccess = { SessionValidationResult.Valid },
                onFailure = { it.toSessionValidationResult() }
            )
        }
    }

    private fun Throwable.toSessionValidationResult(): SessionValidationResult = when (this) {
        is HttpException -> when (code()) {
            401, 403 -> SessionValidationResult.Invalid
            else -> SessionValidationResult.Error("Server error ${code()}")
        }
        is IOException -> SessionValidationResult.Offline
        else -> SessionValidationResult.Error(message)
    }
}
