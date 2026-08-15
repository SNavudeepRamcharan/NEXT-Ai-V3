package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.api.ApiConfig
import com.example.data.model.AppSettings
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "next_ai_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_MODEL = stringPreferencesKey("default_model")
        val DEFAULT_PERSONA = stringPreferencesKey("default_persona")
        val WEB_SEARCH_DEFAULT = booleanPreferencesKey("web_search_default")
        val CUSTOM_BASE_URL = stringPreferencesKey("custom_base_url")
        val ENTER_TO_SEND = booleanPreferencesKey("enter_to_send")
        val SHOW_TIMESTAMPS = booleanPreferencesKey("show_timestamps")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val themeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.DARK.name
        val themeMode = try {
            ThemeMode.valueOf(themeString)
        } catch (_: Exception) {
            ThemeMode.DARK
        }

        AppSettings(
            themeMode = themeMode,
            defaultModel = preferences[PreferencesKeys.DEFAULT_MODEL] ?: "auto/best",
            defaultPersona = preferences[PreferencesKeys.DEFAULT_PERSONA] ?: "general",
            webSearchDefault = preferences[PreferencesKeys.WEB_SEARCH_DEFAULT] ?: false,
            customBaseUrl = preferences[PreferencesKeys.CUSTOM_BASE_URL] ?: ApiConfig.DEFAULT_BASE_URL,
            enterToSend = preferences[PreferencesKeys.ENTER_TO_SEND] ?: true,
            showTimestamps = preferences[PreferencesKeys.SHOW_TIMESTAMPS] ?: true
        )
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateDefaultModel(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_MODEL] = modelId
        }
    }

    suspend fun updateDefaultPersona(personaId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PERSONA] = personaId
        }
    }

    suspend fun updateWebSearchDefault(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEB_SEARCH_DEFAULT] = enabled
        }
    }

    suspend fun updateBaseUrl(url: String) {
        val normalized = ApiConfig.normalizeUrl(url)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_BASE_URL] = normalized
        }
    }

    suspend fun updateEnterToSend(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENTER_TO_SEND] = enabled
        }
    }

    suspend fun updateShowTimestamps(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_TIMESTAMPS] = enabled
        }
    }
}
