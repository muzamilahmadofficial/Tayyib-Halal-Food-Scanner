package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.legal.LegalDocumentType
import com.example.localization.AppLanguage
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.settings.AppThemeMode
import com.example.settings.UserSettings

@Composable
fun SettingsScreen(
    settings: UserSettings,
    cachedCount: Int,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (AppThemeMode) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onVibrationToggle: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLegal: (LegalDocumentType) -> Unit
) {
    val isFr = isFrenchLocale()
    var confirmAction by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = tStr(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Language Selection (Instant live switch)
        item {
            SettingsCard {
                Text(
                    text = tStr(R.string.settings_language),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val label = when (lang) {
                            AppLanguage.SYSTEM -> if (isFr) "Système" else "System"
                            AppLanguage.ENGLISH -> "English"
                            AppLanguage.FRENCH -> "Français"
                        }
                        FilterChip(
                            selected = settings.language == lang,
                            onClick = { onLanguageChange(lang) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("settings_lang_${lang.code}")
                        )
                    }
                }
            }
        }

        // Appearance Theme
        item {
            SettingsCard {
                Text(
                    text = tStr(R.string.settings_theme),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            AppThemeMode.SYSTEM -> if (isFr) "Système" else "System"
                            AppThemeMode.LIGHT -> if (isFr) "Clair" else "Light"
                            AppThemeMode.DARK -> if (isFr) "Sombre" else "Dark"
                        }
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeChange(mode) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("settings_theme_${mode.code}")
                        )
                    }
                }
            }
        }

        // Sound & Vibration (Independent controls)
        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tStr(R.string.settings_sound), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (isFr) "Sons distincts pour Halal, Haram, Douteux et Inconnu" else "Distinct chimes for Halal, Haram, Doubtful & Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = onSoundToggle,
                        modifier = Modifier.testTag("settings_sound_switch")
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tStr(R.string.settings_vibration), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (isFr) "Retour haptique lors du scan et du résultat" else "Haptic pulse on barcode read and verdict",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = onVibrationToggle,
                        modifier = Modifier.testTag("settings_vibration_switch")
                    )
                }
            }
        }

        // Local Cache & History Controls
        item {
            SettingsCard {
                SettingsRowItem(
                    icon = Icons.Outlined.CleaningServices,
                    title = tStr(R.string.settings_clear_cache),
                    subtitle = if (isFr) "$cachedCount produit(s) en cache hors ligne" else "$cachedCount cached product(s) offline",
                    onClick = { confirmAction = "cache" },
                    testTag = "settings_clear_cache_row"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Outlined.DeleteSweep,
                    title = tStr(R.string.settings_clear_history),
                    subtitle = if (isFr) "Effacer l'historique local sur cet appareil" else "Remove local scan history on this device",
                    onClick = { confirmAction = "history" },
                    testTag = "settings_clear_history_row"
                )
            }
        }

        // About & Legal Section
        item {
            Text(
                text = tStr(R.string.settings_legal_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            SettingsCard {
                SettingsRowItem(
                    icon = Icons.Outlined.Info,
                    title = tStr(R.string.settings_about),
                    subtitle = "Muzamil Ahmad · Tayyib v1.0.0",
                    onClick = onOpenAbout,
                    testTag = "settings_about_row"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Outlined.VerifiedUser,
                    title = tStr(R.string.legal_methodology),
                    onClick = { onOpenLegal(LegalDocumentType.HALAL_METHODOLOGY) },
                    testTag = "settings_legal_methodology"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Outlined.Gavel,
                    title = tStr(R.string.legal_disclaimer),
                    onClick = { onOpenLegal(LegalDocumentType.RELIGIOUS_DISCLAIMER) },
                    testTag = "settings_legal_disclaimer"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Outlined.PrivacyTip,
                    title = tStr(R.string.legal_privacy),
                    onClick = { onOpenLegal(LegalDocumentType.PRIVACY_POLICY) },
                    testTag = "settings_legal_privacy"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Outlined.Description,
                    title = tStr(R.string.legal_terms),
                    onClick = { onOpenLegal(LegalDocumentType.TERMS_OF_USE) },
                    testTag = "settings_legal_terms"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Outlined.Public,
                    title = tStr(R.string.legal_attribution),
                    onClick = { onOpenLegal(LegalDocumentType.DATA_ATTRIBUTION) },
                    testTag = "settings_legal_attribution"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRowItem(
                    icon = Icons.Outlined.Source,
                    title = tStr(R.string.legal_licenses),
                    onClick = { onOpenLegal(LegalDocumentType.OPEN_SOURCE_LICENSES) },
                    testTag = "settings_legal_licenses"
                )
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }

    if (confirmAction != null) {
        val isCache = confirmAction == "cache"
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = {
                Text(if (isCache) tStr(R.string.settings_clear_cache) else tStr(R.string.settings_clear_history))
            },
            text = {
                Text(
                    if (isCache) {
                        if (isFr) "Supprimer tous les produits mis en cache localement ?" else "Delete all locally cached product records?"
                    } else {
                        tStr(R.string.history_clear_confirm)
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (isCache) onClearCache() else onClearHistory()
                    confirmAction = null
                }) {
                    Text(tStr(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text(tStr(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
