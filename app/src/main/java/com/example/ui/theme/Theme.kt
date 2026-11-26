package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.domain.HalalVerdict
import com.example.settings.AppThemeMode

@Immutable
data class TayyibSemanticColors(
    val halal: Color,
    val halalContainer: Color,
    val haram: Color,
    val haramContainer: Color,
    val doubtful: Color,
    val doubtfulContainer: Color,
    val unknown: Color,
    val unknownContainer: Color,
    val gold: Color,
    val goldContainer: Color
) {
    fun accentFor(verdict: HalalVerdict): Color = when (verdict) {
        HalalVerdict.HALAL -> halal
        HalalVerdict.HARAM -> haram
        HalalVerdict.DOUBTFUL -> doubtful
        HalalVerdict.UNKNOWN -> unknown
    }

    fun containerFor(verdict: HalalVerdict): Color = when (verdict) {
        HalalVerdict.HALAL -> halalContainer
        HalalVerdict.HARAM -> haramContainer
        HalalVerdict.DOUBTFUL -> doubtfulContainer
        HalalVerdict.UNKNOWN -> unknownContainer
    }
}

private val LightSemanticColors = TayyibSemanticColors(
    halal = VerdictHalal,
    halalContainer = VerdictHalalContainerLight,
    haram = VerdictHaram,
    haramContainer = VerdictHaramContainerLight,
    doubtful = VerdictDoubtful,
    doubtfulContainer = VerdictDoubtfulContainerLight,
    unknown = VerdictUnknown,
    unknownContainer = VerdictUnknownContainerLight,
    gold = TayyibGold,
    goldContainer = TayyibGoldMuted
)

private val DarkSemanticColors = TayyibSemanticColors(
    halal = Color(0xFF4ADE80),
    halalContainer = VerdictHalalContainerDark,
    haram = Color(0xFFF87171),
    haramContainer = VerdictHaramContainerDark,
    doubtful = Color(0xFFFBBF24),
    doubtfulContainer = VerdictDoubtfulContainerDark,
    unknown = Color(0xFF94A3B8),
    unknownContainer = VerdictUnknownContainerDark,
    gold = Color(0xFFD8B772),
    goldContainer = Color(0xFF2B2416)
)

val LocalTayyibColors = staticCompositionLocalOf { LightSemanticColors }

private val TayyibLightColorScheme = lightColorScheme(
    primary = TayyibForestGreen,
    onPrimary = TayyibIvory,
    primaryContainer = Color(0xFFD8EBE4),
    onPrimaryContainer = TayyibDarkGreen,
    secondary = TayyibGold,
    onSecondary = TayyibDarkGreen,
    secondaryContainer = TayyibGoldMuted,
    onSecondaryContainer = Color(0xFF493610),
    tertiary = TayyibDeepEmerald,
    onTertiary = Color.White,
    background = TayyibIvory,
    onBackground = TayyibInk,
    surface = TayyibWarmPaper,
    onSurface = TayyibInk,
    surfaceVariant = TayyibSandSurface,
    onSurfaceVariant = TayyibInkMuted,
    outline = TayyibSandBorder,
    outlineVariant = Color(0xFFE7E2D6),
    error = VerdictHaram,
    onError = Color.White
)

private val TayyibDarkColorScheme = darkColorScheme(
    primary = Color(0xFF66BFA0),
    onPrimary = TayyibDarkGreen,
    primaryContainer = TayyibForestGreen,
    onPrimaryContainer = TayyibIvory,
    secondary = Color(0xFFD8B772),
    onSecondary = TayyibDarkGreen,
    secondaryContainer = Color(0xFF2C2517),
    onSecondaryContainer = Color(0xFFF4E1B8),
    tertiary = Color(0xFF49A082),
    onTertiary = TayyibDarkGreen,
    background = TayyibDarkBg,
    onBackground = TayyibDarkText,
    surface = TayyibDarkSurface,
    onSurface = TayyibDarkText,
    surfaceVariant = TayyibDarkSurfaceElevated,
    onSurfaceVariant = TayyibDarkTextMuted,
    outline = TayyibDarkBorder,
    outlineVariant = Color(0xFF1A342B),
    error = Color(0xFFF87171),
    onError = Color(0xFF3A1313)
)

@Composable
fun TayyibTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) TayyibDarkColorScheme else TayyibLightColorScheme
    val semanticColors = if (isDark) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalTayyibColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TayyibTypography,
            shapes = TayyibShapes,
            content = content
        )
    }
}
