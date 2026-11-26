package com.example.settings

enum class AppThemeMode(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromCode(code: String?): AppThemeMode =
            entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

data class UserSettings(
    val language: com.example.localization.AppLanguage = com.example.localization.AppLanguage.SYSTEM,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
