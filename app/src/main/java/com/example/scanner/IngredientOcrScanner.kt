package com.example.scanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.localization.isFrenchLocale
import com.example.localization.tStr
import com.example.ui.theme.TayyibDarkGreen
import com.example.ui.theme.TayyibGold
import com.example.ui.theme.TayyibIvory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object IngredientOcrExtractor {

    private val ingredientsHeaderRegex = Regex(
        pattern = "(?i)(?:ingredients|ingr[eé]dients|zutaten|ingredientes|ingredienti)\\s*[:\\-]?"
    )

    /**
     * Cleans raw OCR text from food packaging:
     * - Rejoins hyphenated line wraps (e.g. "emulsi-\nfier" -> "emulsifier")
     * - Normalizes line breaks and excess whitespace
     * - Focuses from the "Ingredients:" / "Ingrédients:" label header when clearly present
     */
    fun cleanExtractedText(rawText: String): String {
        if (rawText.isBlank()) return ""

        val rejoinedHyphens = rawText
            .replace("\r\n", "\n")
            .replace(Regex("-\\s*\\n\\s*"), "")
            .replace(Regex("\\s*\\n\\s*"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        val headerMatch = ingredientsHeaderRegex.find(rejoinedHyphens)
        val focused = if (headerMatch != null && headerMatch.range.first > 0) {
            val afterHeader = rejoinedHyphens.substring(headerMatch.range.first).trim()
            if (afterHeader.length >= 18) afterHeader else rejoinedHyphens
        } else {
            rejoinedHyphens
        }

        return focused.trim()
    }

    /**
     * Runs on-device ML Kit Text Recognition on a user-selected image URI (zero upload, offline).
     */
    fun recognizeFromUri(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onEmpty: () -> Unit,
        onError: (String) -> Unit
    ) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val cleaned = cleanExtractedText(visionText.text)
                    if (cleaned.isNotBlank()) {
                        onSuccess(cleaned)
                    } else {
                        onEmpty()
                    }
                }
                .addOnFailureListener { err ->
                    onError(err.localizedMessage ?: "OCR failed")
                }
                .addOnCompleteListener {
                    runCatching { recognizer.close() }
                }
        } catch (e: Exception) {
            runCatching { recognizer.close() }
            onError(e.localizedMessage ?: "Unable to read selected image")
        }
    }

    /**
     * Attempts to launch the installed Google Lens app (useful for copying/translating
     * non-Latin labels like Arabic, Urdu, Japanese, or Korean before pasting into Tayyib).
     * Returns true if launched, or false if Google Lens is not installed on this device.
     */
    fun launchGoogleLensApp(context: Context): Boolean {
        val pm = context.packageManager

        val directLensIntent = pm.getLaunchIntentForPackage("com.google.ar.lens")
        if (directLensIntent != null) {
            directLensIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching {
                context.startActivity(directLensIntent)
                true
            }.getOrDefault(false)
        }

        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://lens")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (deepLinkIntent.resolveActivity(pm) != null) {
            return runCatching {
                context.startActivity(deepLinkIntent)
                true
            }.getOrDefault(false)
        }

        return false
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun IngredientLensCameraOverlay(
    onTextExtracted: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isFr = isFrenchLocale()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val captureRequested = remember { AtomicBoolean(false) }

    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(true) }
    var isCapturing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onClose)

    LaunchedEffect(torchEnabled, cameraRef) {
        val cam = cameraRef
        if (cam != null && cam.cameraInfo.hasFlashUnit()) {
            cam.cameraControl.enableTorch(torchEnabled)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                cameraRef?.cameraControl?.enableTorch(false)
                recognizer.close()
                cameraExecutor.shutdown()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("ingredient_lens_camera_overlay")
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener(
                    {
                        try {
                            val cameraProvider = providerFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (!captureRequested.compareAndSet(true, false)) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val mediaImage = imageProxy.image
                                if (mediaImage == null) {
                                    imageProxy.close()
                                    mainHandler.post {
                                        isCapturing = false
                                        statusMessage = if (isFr) {
                                            "Impossible de lire cette image. Réessayez."
                                        } else {
                                            "Could not read frame. Please try again."
                                        }
                                    }
                                    return@setAnalyzer
                                }

                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                recognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        val cleaned = IngredientOcrExtractor.cleanExtractedText(visionText.text)
                                        mainHandler.post {
                                            isCapturing = false
                                            if (cleaned.isNotBlank()) {
                                                onTextExtracted(cleaned)
                                            } else {
                                                statusMessage = if (isFr) {
                                                    "Aucun texte lisible détecté. Rapprochez la caméra de la liste d'ingrédients."
                                                } else {
                                                    "No readable text detected. Move closer to the ingredients list and ensure good lighting."
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        mainHandler.post {
                                            isCapturing = false
                                            statusMessage = if (isFr) {
                                                "Erreur de lecture du texte. Réessayez."
                                            } else {
                                                "Text recognition failed. Please try again."
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }

                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                            cameraRef = camera
                            hasTorch = camera.cameraInfo.hasFlashUnit()
                        } catch (e: Exception) {
                            statusMessage = e.localizedMessage ?: "Camera unavailable"
                        }
                    },
                    ContextCompat.getMainExecutor(ctx)
                )

                previewView
            }
        )

        // Lens viewfinder overlay tailored for ingredient label blocks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameWidth = size.width * 0.86f
            val frameHeight = (size.height * 0.42f).coerceAtMost(frameWidth * 0.82f)
            val left = (size.width - frameWidth) / 2f
            val top = (size.height - frameHeight) / 2f

            drawRect(Color.Black.copy(alpha = 0.48f), Offset.Zero, Size(size.width, top))
            drawRect(
                Color.Black.copy(alpha = 0.48f),
                Offset(0f, top + frameHeight),
                Size(size.width, size.height - top - frameHeight)
            )
            drawRect(Color.Black.copy(alpha = 0.48f), Offset(0f, top), Size(left, frameHeight))
            drawRect(
                Color.Black.copy(alpha = 0.48f),
                Offset(left + frameWidth, top),
                Size(size.width - left - frameWidth, frameHeight)
            )

            drawRoundRect(
                color = TayyibIvory.copy(alpha = 0.72f),
                topLeft = Offset(left, top),
                size = Size(frameWidth, frameHeight),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 3f)
            )

            val cornerLen = 40f
            val stroke = 7f
            val c = TayyibGold
            drawLine(c, Offset(left, top + cornerLen), Offset(left, top), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(c, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(c, Offset(left + frameWidth - cornerLen, top), Offset(left + frameWidth, top), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(c, Offset(left + frameWidth, top), Offset(left + frameWidth, top + cornerLen), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(c, Offset(left, top + frameHeight - cornerLen), Offset(left, top + frameHeight), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(c, Offset(left, top + frameHeight), Offset(left + cornerLen, top + frameHeight), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(c, Offset(left + frameWidth - cornerLen, top + frameHeight), Offset(left + frameWidth, top + frameHeight), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(c, Offset(left + frameWidth, top + frameHeight - cornerLen), Offset(left + frameWidth, top + frameHeight), strokeWidth = stroke, cap = StrokeCap.Round)
        }

        // Top Bar Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = TayyibDarkGreen.copy(alpha = 0.85f)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("lens_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = tStr(R.string.action_cancel),
                        tint = TayyibIvory
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = TayyibDarkGreen.copy(alpha = 0.85f)
            ) {
                Text(
                    text = tStr(R.string.checker_lens_camera),
                    style = MaterialTheme.typography.titleSmall,
                    color = TayyibIvory,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }

            if (hasTorch) {
                Surface(
                    shape = CircleShape,
                    color = if (torchEnabled) TayyibGold else TayyibDarkGreen.copy(alpha = 0.85f)
                ) {
                    IconButton(
                        onClick = { torchEnabled = !torchEnabled },
                        modifier = Modifier.testTag("lens_flashlight_button")
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                            contentDescription = tStr(R.string.scanner_flashlight),
                            tint = if (torchEnabled) TayyibDarkGreen else TayyibIvory
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Bottom Capture Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            statusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xCC7F1D1D)
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.64f)
            ) {
                Text(
                    text = tStr(R.string.checker_lens_guide),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }

            Button(
                onClick = {
                    if (!isCapturing) {
                        statusMessage = null
                        isCapturing = true
                        captureRequested.set(true)
                    }
                },
                enabled = !isCapturing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("lens_capture_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TayyibGold,
                    contentColor = TayyibDarkGreen
                )
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        color = TayyibDarkGreen,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(tStr(R.string.checker_lens_reading), fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Outlined.DocumentScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(tStr(R.string.checker_lens_capture_btn), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
