package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.BarcodeValidator
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.scanner.CameraPreviewView
import com.example.ui.theme.TayyibDarkGreen
import com.example.ui.theme.TayyibForestGreen
import com.example.ui.theme.TayyibGold
import com.example.ui.theme.TayyibIvory
import com.example.ui.viewmodel.ScannerUiState

@Composable
fun ScannerScreen(
    scannerState: ScannerUiState,
    isOnline: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onCancelScan: () -> Unit,
    onResetScanner: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateToChecker: () -> Unit
) {
    val context = LocalContext.current
    val isFr = isFrenchLocale()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var torchEnabled by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(true) }
    var showManualDialog by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            torchEnabled = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("scanner_screen")
    ) {
        if (hasCameraPermission && cameraError == null) {
            CameraPreviewView(
                torchEnabled = torchEnabled,
                isPaused = scannerState !is ScannerUiState.Ready || showManualDialog,
                onTorchAvailabilityChanged = { hasTorch = it },
                onBarcodeDetected = onBarcodeDetected,
                onCameraError = { cameraError = it },
                modifier = Modifier.fillMaxSize()
            )

            // Animated viewfinder overlay
            ScannerOverlayCanvas(
                isScanningActive = scannerState is ScannerUiState.Ready,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission request or camera fallback card (Manual entry always available!)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = tStr(R.string.scanner_permission_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cameraError ?: tStr(R.string.scanner_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                if (!hasCameraPermission) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("scanner_allow_camera_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TayyibForestGreen,
                            contentColor = TayyibIvory
                        )
                    ) {
                        Text(tStr(R.string.scanner_permission_grant))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                OutlinedButton(
                    onClick = { showManualDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("scanner_manual_fallback_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Keyboard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tStr(R.string.scanner_manual_entry))
                }
            }
        }

        // Top Bar Controls over camera
        if (hasCameraPermission && cameraError == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = TayyibDarkGreen.copy(alpha = 0.82f)
                    ) {
                        Text(
                            text = tStr(R.string.scanner_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = TayyibIvory,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                    }

                    if (hasTorch) {
                        Surface(
                            shape = CircleShape,
                            color = if (torchEnabled) TayyibGold else TayyibDarkGreen.copy(alpha = 0.82f)
                        ) {
                            IconButton(
                                onClick = { torchEnabled = !torchEnabled },
                                modifier = Modifier.testTag("scanner_flashlight_button")
                            ) {
                                Icon(
                                    imageVector = if (torchEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                                    contentDescription = tStr(R.string.scanner_flashlight),
                                    tint = if (torchEnabled) TayyibDarkGreen else TayyibIvory
                                )
                            }
                        }
                    }
                }

                if (!isOnline) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TayyibDarkGreen.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WifiOff,
                                contentDescription = null,
                                tint = TayyibGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tStr(R.string.scanner_offline_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = TayyibIvory
                            )
                        }
                    }
                }
            }

            // Bottom Guidance & Manual Barcode Entry CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.62f)
                ) {
                    Text(
                        text = tStr(R.string.scanner_instruction),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showManualDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("scanner_manual_entry_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TayyibIvory,
                            contentColor = TayyibDarkGreen
                        )
                    ) {
                        Icon(Icons.Outlined.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tStr(R.string.scanner_manual_entry),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = onNavigateToChecker,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("scanner_lens_ingredients_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TayyibGold,
                            contentColor = TayyibDarkGreen
                        )
                    ) {
                        Icon(Icons.Outlined.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tStr(R.string.scanner_lens_ingredients),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Loading / Not Found / Offline / Error Result Overlays
        when (scannerState) {
            is ScannerUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = TayyibForestGreen)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = tStr(R.string.scanner_loading),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scannerState.barcode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(
                                onClick = onCancelScan,
                                modifier = Modifier.testTag("scanner_cancel_button")
                            ) {
                                Text(tStr(R.string.action_cancel))
                            }
                        }
                    }
                }
            }

            is ScannerUiState.NotFound -> {
                ScannerOutcomeModal(
                    title = tStr(R.string.scanner_not_found_title),
                    body = "${tStr(R.string.scanner_not_found_desc)}\n\n${tStr(R.string.detail_barcode)}: ${scannerState.barcode}",
                    onRetry = onResetScanner,
                    onSearchManually = {
                        onResetScanner()
                        onNavigateToSearch(scannerState.barcode)
                    },
                    onCheckIngredients = {
                        onResetScanner()
                        onNavigateToChecker()
                    }
                )
            }

            is ScannerUiState.OfflineNotCached -> {
                ScannerOutcomeModal(
                    title = if (isFr) "Connexion Internet requise" else "Internet Connection Required",
                    body = if (isFr) {
                        "Le code-barres ${scannerState.barcode} n'est pas encore enregistré dans votre cache hors ligne. Connectez-vous à Internet ou analysez les ingrédients manuellement hors ligne."
                    } else {
                        "Barcode ${scannerState.barcode} is not saved in your offline cache yet. Connect to the internet to look it up, or check its ingredients offline."
                    },
                    onRetry = onResetScanner,
                    onSearchManually = null,
                    onCheckIngredients = {
                        onResetScanner()
                        onNavigateToChecker()
                    }
                )
            }

            is ScannerUiState.Error -> {
                ScannerOutcomeModal(
                    title = if (isFr) "Erreur de vérification" else "Lookup Unavailable",
                    body = if (isFr) {
                        "Impossible de contacter Halal Open Food Facts ni le serveur de secours Open Food Facts (${scannerState.message}). Réessayez ou scannez les ingrédients hors ligne."
                    } else {
                        "Could not reach Halal Open Food Facts or the Open Food Facts backup service (${scannerState.message}). Please retry or scan ingredients offline."
                    },
                    onRetry = onResetScanner,
                    onSearchManually = {
                        onResetScanner()
                        onNavigateToSearch(scannerState.barcode)
                    },
                    onCheckIngredients = {
                        onResetScanner()
                        onNavigateToChecker()
                    }
                )
            }

            ScannerUiState.Ready, is ScannerUiState.ProductFound -> Unit
        }
    }

    if (showManualDialog) {
        ManualBarcodeDialog(
            onDismiss = { showManualDialog = false },
            onSubmit = { code ->
                showManualDialog = false
                onBarcodeDetected(code)
            }
        )
    }
}

@Composable
private fun ScannerOutcomeModal(
    title: String,
    body: String,
    onRetry: () -> Unit,
    onSearchManually: (() -> Unit)?,
    onCheckIngredients: () -> Unit
) {
    val isFr = isFrenchLocale()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("scanner_retry_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tStr(R.string.scanner_try_again))
                }
                if (onSearchManually != null) {
                    FilledTonalButton(
                        onClick = onSearchManually,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFr) "Rechercher manuellement" else "Search Manually")
                    }
                }
                OutlinedButton(
                    onClick = onCheckIngredients,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(tStr(R.string.home_tool_checker))
                }
            }
        }
    }
}

@Composable
private fun ManualBarcodeDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val trySubmit = {
        val cleaned = input.trim().replace(" ", "").replace("-", "")
        if (BarcodeValidator.isValidManualBarcode(cleaned)) {
            onSubmit(cleaned)
        } else {
            showError = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tStr(R.string.scanner_manual_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        showError = false
                    },
                    placeholder = { Text(tStr(R.string.scanner_manual_dialog_hint)) },
                    singleLine = true,
                    isError = showError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { trySubmit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_barcode_input")
                )
                if (showError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tStr(R.string.scanner_invalid_barcode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = trySubmit,
                modifier = Modifier.testTag("manual_barcode_submit")
            ) {
                Text(tStr(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tStr(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ScannerOverlayCanvas(
    isScanningActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val lineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_y"
    )

    Canvas(modifier = modifier) {
        val frameWidth = (size.width * 0.78f).coerceAtMost(size.height * 0.48f)
        val frameHeight = frameWidth * 0.66f
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f

        // Subtle dimmed surround around viewfinder
        drawRect(
            color = Color.Black.copy(alpha = 0.42f),
            topLeft = Offset.Zero,
            size = Size(size.width, top)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.42f),
            topLeft = Offset(0f, top + frameHeight),
            size = Size(size.width, size.height - top - frameHeight)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.42f),
            topLeft = Offset(0f, top),
            size = Size(left, frameHeight)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.42f),
            topLeft = Offset(left + frameWidth, top),
            size = Size(size.width - left - frameWidth, frameHeight)
        )

        // Frame border
        drawRoundRect(
            color = TayyibIvory.copy(alpha = 0.7f),
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 3f)
        )

        // Gold corner brackets
        val cornerLen = 36f
        val stroke = 7f
        val c = TayyibGold
        // Top-Left
        drawLine(c, Offset(left, top + cornerLen), Offset(left, top), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(c, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = stroke, cap = StrokeCap.Round)
        // Top-Right
        drawLine(c, Offset(left + frameWidth - cornerLen, top), Offset(left + frameWidth, top), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(c, Offset(left + frameWidth, top), Offset(left + frameWidth, top + cornerLen), strokeWidth = stroke, cap = StrokeCap.Round)
        // Bottom-Left
        drawLine(c, Offset(left, top + frameHeight - cornerLen), Offset(left, top + frameHeight), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(c, Offset(left, top + frameHeight), Offset(left + cornerLen, top + frameHeight), strokeWidth = stroke, cap = StrokeCap.Round)
        // Bottom-Right
        drawLine(c, Offset(left + frameWidth - cornerLen, top + frameHeight), Offset(left + frameWidth, top + frameHeight), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(c, Offset(left + frameWidth, top + frameHeight - cornerLen), Offset(left + frameWidth, top + frameHeight), strokeWidth = stroke, cap = StrokeCap.Round)

        // Animated laser indicator
        if (isScanningActive) {
            val y = top + 16f + (frameHeight - 32f) * lineProgress
            drawLine(
                color = TayyibGold.copy(alpha = 0.9f),
                start = Offset(left + 16f, y),
                end = Offset(left + frameWidth - 16f, y),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}
