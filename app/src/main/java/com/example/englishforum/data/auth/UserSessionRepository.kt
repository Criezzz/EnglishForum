package com.example.englishforum.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface UserSessionRepository {
    val sessionFlow: Flow<UserSession?>

    suspend fun saveSession(session: UserSession)

    suspend fun clearSession()

    suspend fun markEmailVerified()
}

class DataStoreUserSessionRepository(context: Context) : UserSessionRepository {
    private val appContext = context.applicationContext
    private val dataStore = appContext.userSessionDataStore

    override val sessionFlow: Flow<UserSession?> = dataStore.data.map { preferences ->
        val userId = preferences[USER_ID]
        val username = preferences[USERNAME]
        val accessToken = preferences[ACCESS_TOKEN]
        val refreshToken = preferences[REFRESH_TOKEN]
        val tokenType = preferences[TOKEN_TYPE] ?: "Bearer"
        val isEmailVerified = preferences[IS_EMAIL_VERIFIED] ?: true

        if (userId != null && username != null && accessToken != null && refreshToken != null) {
            UserSession(
                userId = userId,
                username = username,
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = tokenType,
                isEmailVerified = isEmailVerified
            )
        } else {
            null
        }
    }

    override suspend fun saveSession(session: UserSession) {
        dataStore.edit { prefs ->
            prefs[USER_ID] = session.userId
            prefs[USERNAME] = session.username
            prefs[ACCESS_TOKEN] = session.accessToken
            prefs[REFRESH_TOKEN] = session.refreshToken
            prefs[TOKEN_TYPE] = session.tokenType
            prefs[IS_EMAIL_VERIFIED] = session.isEmailVerified
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    override suspend fun markEmailVerified() {
        dataStore.edit { prefs ->
            prefs[IS_EMAIL_VERIFIED] = true
        }
    }

    private companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val TOKEN_TYPE = stringPreferencesKey("token_type")
        val IS_EMAIL_VERIFIED = booleanPreferencesKey("is_email_verified")
    }
}

private val Context.userSessionDataStore by preferencesDataStore(name = "user_session")
