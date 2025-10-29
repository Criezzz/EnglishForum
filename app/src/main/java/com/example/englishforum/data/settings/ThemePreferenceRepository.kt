package com.example.englishforum.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.englishforum.core.model.DEFAULT_SEED_COLOR
import com.example.englishforum.core.model.ThemeOption
import com.example.englishforum.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemePreferenceRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.themePreferenceDataStore

    val themePreferencesFlow: Flow<ThemePreferences> = dataStore.data.map { prefs ->
        val themeOption = prefs[THEME_KEY]?.let { storedValue ->
            runCatching { ThemeOption.valueOf(storedValue) }.getOrNull()
        } ?: ThemeOption.FOLLOW_SYSTEM

        ThemePreferences(
            themeOption = themeOption,
            isMaterialThemeEnabled = prefs[MATERIAL_THEME_KEY] ?: false,
            isAmoledEnabled = prefs[AMOLED_KEY] ?: false,
            seedColor = prefs[SEED_COLOR_KEY] ?: DEFAULT_SEED_COLOR
        )
    }

    val themeOptionFlow: Flow<ThemeOption> = themePreferencesFlow.map { it.themeOption }

    suspend fun setThemeOption(option: ThemeOption) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = option.name
        }
    }

    suspend fun setMaterialThemeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[MATERIAL_THEME_KEY] = enabled
        }
    }

    suspend fun setAmoledEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AMOLED_KEY] = enabled
        }
    }

    suspend fun setSeedColor(color: Long) {
        dataStore.edit { prefs ->
            prefs[SEED_COLOR_KEY] = color
        }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_option")
        val MATERIAL_THEME_KEY = booleanPreferencesKey("material_theme_enabled")
        val AMOLED_KEY = booleanPreferencesKey("amoled_enabled")
        val SEED_COLOR_KEY = longPreferencesKey("seed_color")
    }
}

private val Context.themePreferenceDataStore by preferencesDataStore(name = "theme_preferences")
