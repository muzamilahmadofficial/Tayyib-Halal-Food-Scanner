package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dangerous
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.EvidenceQuality
import com.example.domain.HalalVerdict
import com.example.domain.IngredientCategory
import com.example.domain.ReportIssueType
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.ui.theme.LocalTayyibColors
import com.example.ui.theme.TayyibForestGreen
import com.example.ui.theme.TayyibGold
import com.example.ui.theme.TayyibIvory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TayyibLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = TayyibForestGreen
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_tayyib_logo_mark),
            contentDescription = "Tayyib emblem",
            tint = Color.Unspecified,
            modifier = Modifier.size(size * 0.78f)
        )
    }
}

@Composable
fun VerdictBadge(
    verdict: HalalVerdict,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val semantic = LocalTayyibColors.current
    val accent = semantic.accentFor(verdict)
    val container = semantic.containerFor(verdict)
    val label = verdictLabel(verdict)
    val icon = verdictIcon(verdict)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (large) 16.dp else 10.dp,
                vertical = if (large) 8.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (large) 8.dp else 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(if (large) 20.dp else 15.dp)
            )
            Text(
                text = label.uppercase(),
                style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun verdictLabel(verdict: HalalVerdict): String = when (verdict) {
    HalalVerdict.HALAL -> tStr(R.string.verdict_halal)
    HalalVerdict.HARAM -> tStr(R.string.verdict_haram)
    HalalVerdict.DOUBTFUL -> tStr(R.string.verdict_doubtful)
    HalalVerdict.UNKNOWN -> tStr(R.string.verdict_unknown)
}

fun verdictIcon(verdict: HalalVerdict): ImageVector = when (verdict) {
    HalalVerdict.HALAL -> Icons.Outlined.CheckCircle
    HalalVerdict.HARAM -> Icons.Outlined.Dangerous
    HalalVerdict.DOUBTFUL -> Icons.Outlined.WarningAmber
    HalalVerdict.UNKNOWN -> Icons.Outlined.HelpOutline
}

@Composable
fun EvidenceQualityChip(
    quality: EvidenceQuality,
    modifier: Modifier = Modifier
) {
    val isFr = isFrenchLocale()
    val label = when (quality) {
        EvidenceQuality.HIGH -> if (isFr) "Qualité de preuve : Élevée" else "Evidence Quality: High"
        EvidenceQuality.MODERATE -> if (isFr) "Qualité de preuve : Modérée" else "Evidence Quality: Moderate"
        EvidenceQuality.LIMITED -> if (isFr) "Qualité de preuve : Limitée" else "Evidence Quality: Limited"
        EvidenceQuality.INSUFFICIENT -> if (isFr) "Qualité de preuve : Insuffisante" else "Evidence Quality: Insufficient"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun IngredientCategoryChip(category: IngredientCategory) {
    val isFr = isFrenchLocale()
    val semantic = LocalTayyibColors.current
    val (label, accent, container) = when (category) {
        IngredientCategory.CLEARLY_PROHIBITED -> Triple(
            if (isFr) "Interdit" else "Prohibited",
            semantic.haram,
            semantic.haramContainer
        )
        IngredientCategory.SOURCE_DEPENDENT -> Triple(
            if (isFr) "Selon origine" else "Source-Dependent",
            semantic.doubtful,
            semantic.doubtfulContainer
        )
        IngredientCategory.REQUIRES_VERIFICATION -> Triple(
            if (isFr) "À vérifier" else "Verify",
            semantic.doubtful,
            semantic.doubtfulContainer
        )
        IngredientCategory.GENERALLY_PERMISSIBLE -> Triple(
            if (isFr) "Permis" else "Permissible",
            semantic.halal,
            semantic.halalContainer
        )
        IngredientCategory.INSUFFICIENT_INFO -> Triple(
            if (isFr) "Données limitées" else "Insufficient Info",
            semantic.unknown,
            semantic.unknownContainer
        )
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ProductSummaryCard(
    title: String,
    subtitle: String,
    barcode: String,
    imageUrl: String?,
    verdict: HalalVerdict,
    footerText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { barcode },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VerdictBadge(verdict = verdict)
                    if (!footerText.isNullOrBlank()) {
                        Text(
                            text = footerText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (trailingAction != null) {
                Spacer(modifier = Modifier.width(4.dp))
                trailingAction()
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_state_1795716317688),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 188.dp, height = 140.dp)
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TayyibForestGreen,
                    contentColor = TayyibIvory
                )
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ReportIssueDialog(
    barcode: String,
    productName: String,
    onDismiss: () -> Unit,
    onLaunchEmailDraft: (ReportIssueType, String) -> Unit
) {
    val isFr = isFrenchLocale()
    var selectedType by remember { mutableStateOf(ReportIssueType.INCORRECT_CLASSIFICATION) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tStr(R.string.detail_report_issue),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isFr) {
                        "Tayyib n'utilise pas de serveur privé de signalement. Sélectionnez le problème pour préparer un brouillon d'e-mail que vous vérifierez avant envoi."
                    } else {
                        "Tayyib does not run a private reporting server. Select the issue below to prepare an email draft in your email app for your review before sending."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                ReportIssueType.entries.forEach { issue ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedType = issue }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == issue,
                            onClick = { selectedType = issue }
                        )
                        Text(
                            text = if (isFr) issue.labelFr else issue.labelEn,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isFr) "Précisions (optionnel)" else "Additional details (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_notes_input"),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onLaunchEmailDraft(selectedType, notes) },
                modifier = Modifier.testTag("report_prepare_email_button")
            ) {
                Text(tStr(R.string.action_open_email))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tStr(R.string.action_cancel))
            }
        }
    )
}

fun launchReportEmailDraft(
    context: Context,
    barcode: String,
    productName: String,
    issueType: ReportIssueType,
    notes: String,
    isFrench: Boolean
) {
    val subject = "[Tayyib Report] ${issueType.labelEn} — $barcode"
    val body = buildString {
        appendLine(if (isFrench) "Signalement de fiche produit Tayyib" else "Tayyib Product Data Report")
        appendLine("---------------------------------")
        appendLine("Product: $productName")
        appendLine("Barcode: $barcode")
        appendLine("Issue: ${issueType.labelEn} / ${issueType.labelFr}")
        if (notes.isNotBlank()) {
            appendLine("Details: $notes")
        }
        appendLine()
        appendLine("Sent via Tayyib: Halal Food Scanner (user-confirmed draft)")
    }
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:Muzamilofficially@gmail.com")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

fun formatTimestamp(millis: Long, isFrench: Boolean): String {
    val pattern = if (isFrench) "dd MMM yyyy" else "MMM dd, yyyy"
    val locale = if (isFrench) Locale.FRENCH else Locale.ENGLISH
    return SimpleDateFormat(pattern, locale).format(Date(millis))
}

fun cleanOffTag(tag: String): String {
    return tag.substringAfter(':')
        .replace('-', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}

@Composable
fun GoldAccentDot() {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(TayyibGold)
    )
}

@Composable
fun rememberShimmerBrush(): Brush {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 280f, 180f)
    )
}

@Composable
fun FeaturedProductCard(
    title: String,
    brand: String,
    imageUrl: String?,
    verdict: HalalVerdict,
    supportingNote: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(176.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(34.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    VerdictBadge(verdict = verdict)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
                if (brand.isNotBlank()) {
                    Text(
                        text = brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!supportingNote.isNullOrBlank()) {
                    Text(
                        text = supportingNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalProductCardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Card(
        modifier = modifier.width(176.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(brush)
            )
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun ProductSummaryCardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(74.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(brush)
                )
            }
        }
    }
}
