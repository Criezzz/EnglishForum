package com.example.englishforum.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface UserSessionRepository {
    val sessionFlow: Flow<UserSession?>

    suspend fun saveSession(session: UserSession)

    suspend fun clearSession()
}

class DataStoreUserSessionRepository(context: Context) : UserSessionRepository {
    private val appContext = context.applicationContext
    private val dataStore = appContext.userSessionDataStore

    override val sessionFlow: Flow<UserSession?> = dataStore.data.map { preferences ->
        val userId = preferences[USER_ID]
        val username = preferences[USERNAME]
        val token = preferences[TOKEN]

        if (userId != null && username != null && token != null) {
            UserSession(
                userId = userId,
                username = username,
                token = token
            )
        } else {
            null
        }
    }

    override suspend fun saveSession(session: UserSession) {
        dataStore.edit { prefs ->
            prefs[USER_ID] = session.userId
            prefs[USERNAME] = session.username
            prefs[TOKEN] = session.token
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val TOKEN = stringPreferencesKey("token")
    }
}

private val Context.userSessionDataStore by preferencesDataStore(name = "user_session")
