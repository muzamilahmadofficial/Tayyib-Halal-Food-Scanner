package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CenterFocusWeak
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.ManualCheckResult
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.scanner.IngredientLensCameraOverlay
import com.example.scanner.IngredientOcrExtractor
import com.example.ui.components.EvidenceQualityChip
import com.example.ui.components.VerdictBadge
import com.example.ui.theme.LocalTayyibColors
import com.example.ui.theme.TayyibDarkGreen
import com.example.ui.theme.TayyibForestGreen
import com.example.ui.theme.TayyibGold
import com.example.ui.theme.TayyibIvory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientCheckerScreen(
    inputText: String,
    result: ManualCheckResult?,
    onInputChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onIngredientsScanned: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isFr = isFrenchLocale()
    val semantic = LocalTayyibColors.current

    var showLensCamera by remember { mutableStateOf(false) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var ocrStatusBanner by remember { mutableStateOf<String?>(null) }

    val extractedMsg = tStr(R.string.checker_lens_extracted)
    val noTextMsg = tStr(R.string.checker_lens_no_text)
    val googleLensHint = tStr(R.string.checker_google_lens_hint)
    val googleLensUnavailable = tStr(R.string.checker_google_lens_unavailable)

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ocrStatusBanner = null
            showLensCamera = true
        } else {
            ocrStatusBanner = if (isFr) {
                "Autorisation caméra refusée. Vous pouvez utiliser « Depuis photo » ou coller la liste manuellement."
            } else {
                "Camera permission denied. You can still scan using 'From Photo' or paste the ingredients manually."
            }
        }
    }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingPhoto = true
            ocrStatusBanner = null
            IngredientOcrExtractor.recognizeFromUri(
                context = context,
                uri = uri,
                onSuccess = { extracted ->
                    isProcessingPhoto = false
                    ocrStatusBanner = extractedMsg
                    onIngredientsScanned(extracted)
                },
                onEmpty = {
                    isProcessingPhoto = false
                    ocrStatusBanner = noTextMsg
                },
                onError = { err ->
                    isProcessingPhoto = false
                    ocrStatusBanner = err
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(tStr(R.string.checker_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("checker_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = tStr(R.string.action_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("ingredient_checker_screen"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = tStr(R.string.checker_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Visual Lens OCR Card (Camera Lens + Zero-Permission Photo Picker + Optional Google Lens App)
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("checker_lens_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TayyibForestGreen,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.CenterFocusWeak,
                                            contentDescription = null,
                                            tint = TayyibGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tStr(R.string.checker_lens_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = tStr(R.string.checker_lens_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            ocrStatusBanner = null
                                            showLensCamera = true
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("checker_lens_camera_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TayyibForestGreen,
                                        contentColor = TayyibIvory
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DocumentScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tStr(R.string.checker_lens_camera),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isProcessingPhoto,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("checker_pick_photo_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (isProcessingPhoto) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.PhotoLibrary,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tStr(R.string.checker_lens_photo),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Optional external Google Lens shortcut (for foreign/non-Latin label translation & copy)
                            TextButton(
                                onClick = {
                                    val launched = IngredientOcrExtractor.launchGoogleLensApp(context)
                                    ocrStatusBanner = if (launched) googleLensHint else googleLensUnavailable
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checker_google_lens_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = TayyibDarkGreen
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tStr(R.string.checker_google_lens_app),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Inline OCR feedback banner when scanning/picking/launching Lens
                if (ocrStatusBanner != null) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier.testTag("checker_ocr_status_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DocumentScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ocrStatusBanner.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        placeholder = { Text(tStr(R.string.checker_input_hint)) },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checker_input_field")
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onAnalyze,
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("checker_analyze_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TayyibForestGreen,
                                contentColor = TayyibIvory
                            )
                        ) {
                            Icon(Icons.Outlined.FactCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tStr(R.string.checker_analyze_btn), fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                ocrStatusBanner = null
                                onClear()
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("checker_clear_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(tStr(R.string.checker_clear_btn))
                        }
                    }
                }

                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tStr(R.string.checker_disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (result != null) {
                    val assessment = result.assessment
                    val accent = semantic.accentFor(assessment.finalVerdict)
                    val container = semantic.containerFor(assessment.finalVerdict)

                    item {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = container),
                            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("checker_result_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                VerdictBadge(verdict = assessment.finalVerdict, large = true)
                                EvidenceQualityChip(quality = assessment.evidenceQuality)
                                Text(
                                    text = if (isFr) assessment.explanationFr else assessment.explanationEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (assessment.matchedIngredients.isNotEmpty()) {
                        item {
                            Text(
                                text = tStr(R.string.detail_additives),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        items(assessment.matchedIngredients, key = { it.entry.id }) { matched ->
                            DetectedIngredientCard(matched = matched, isFr = isFr)
                        }
                    } else {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = if (isFr) {
                                        "Aucun additif E ou ingrédient sensible répertorié dans notre taxonomie locale n'a été trouvé dans ce texte."
                                    } else {
                                        "No prohibited or source-dependent additives or ingredients from Tayyib's offline taxonomy were detected in this text."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(14.dp)) }
            }
        }

        if (showLensCamera) {
            IngredientLensCameraOverlay(
                onTextExtracted = { extracted ->
                    showLensCamera = false
                    ocrStatusBanner = extractedMsg
                    onIngredientsScanned(extracted)
                },
                onClose = { showLensCamera = false }
            )
        }
    }
}
