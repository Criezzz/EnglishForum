package com.example.englishforum.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.englishforum.core.model.ThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemePreferenceRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.themePreferenceDataStore

    val themeOptionFlow: Flow<ThemeOption> = dataStore.data.map { prefs ->
        prefs[THEME_KEY]?.let { storedValue ->
            runCatching { ThemeOption.valueOf(storedValue) }.getOrNull()
        } ?: ThemeOption.FOLLOW_SYSTEM
    }

    suspend fun setThemeOption(option: ThemeOption) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = option.name
        }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_option")
    }
}

private val Context.themePreferenceDataStore by preferencesDataStore(name = "theme_preferences")
