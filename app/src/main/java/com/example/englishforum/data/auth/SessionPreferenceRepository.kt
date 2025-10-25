package com.example.englishforum.data.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SessionPreferenceRepository {
    val keepLoggedInFlow: Flow<Boolean>

    suspend fun setKeepLoggedIn(enabled: Boolean)
}

class DataStoreSessionPreferenceRepository(context: Context) : SessionPreferenceRepository {
    private val appContext = context.applicationContext
    private val dataStore = appContext.sessionPreferenceDataStore

    override val keepLoggedInFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEEP_LOGGED_IN] ?: false
    }

    override suspend fun setKeepLoggedIn(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEEP_LOGGED_IN] = enabled
        }
    }

    private companion object {
        val KEEP_LOGGED_IN = booleanPreferencesKey("keep_logged_in")
    }
}

private val Context.sessionPreferenceDataStore by preferencesDataStore(name = "session_preferences")
