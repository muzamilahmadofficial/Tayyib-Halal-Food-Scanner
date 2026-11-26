package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppLanguage
import com.example.settings.AppThemeMode
import com.example.settings.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tayyibDataStore by preferencesDataStore(name = "tayyib_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val settingsFlow: Flow<UserSettings> = context.tayyibDataStore.data.map { prefs ->
        UserSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            themeMode = AppThemeMode.fromCode(prefs[Keys.THEME]),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.tayyibDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.tayyibDataStore.edit { it[Keys.THEME] = mode.code }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.tayyibDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.tayyibDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }
}
