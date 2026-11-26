package com.example.localization

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    SYSTEM("system", "System Default", "Système"),
    ENGLISH("en", "English", "English"),
    FRENCH("fr", "French", "Français");

    fun resolveLocale(deviceLocale: Locale = Locale.getDefault()): Locale = when (this) {
        ENGLISH -> Locale.ENGLISH
        FRENCH -> Locale.FRENCH
        SYSTEM -> if (deviceLocale.language.equals("fr", ignoreCase = true)) {
            Locale.FRENCH
        } else {
            Locale.ENGLISH
        }
    }

    fun isFrench(deviceLocale: Locale = Locale.getDefault()): Boolean {
        return resolveLocale(deviceLocale).language.equals("fr", ignoreCase = true)
    }

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

class LocalizedContextHolder(
    val context: Context,
    val isFrench: Boolean
) {
    fun str(resId: Int): String = context.getString(resId)
    fun str(resId: Int, vararg args: Any): String = context.getString(resId, *args)
}

val LocalLocalizedContext = staticCompositionLocalOf<LocalizedContextHolder> {
    error("LocalLocalizedContext not provided")
}

@Composable
fun ProvideAppLocale(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val holder = remember(language, baseContext) {
        val locale = language.resolveLocale()
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        val localizedContext = baseContext.createConfigurationContext(config)
        LocalizedContextHolder(
            context = localizedContext,
            isFrench = locale.language.equals("fr", ignoreCase = true)
        )
    }
    CompositionLocalProvider(LocalLocalizedContext provides holder) {
        content()
    }
}

@Composable
fun tStr(resId: Int): String = LocalLocalizedContext.current.str(resId)

@Composable
fun isFrenchLocale(): Boolean = LocalLocalizedContext.current.isFrench
