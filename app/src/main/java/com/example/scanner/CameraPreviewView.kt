package com.example.scanner

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreviewView(
    torchEnabled: Boolean,
    isPaused: Boolean,
    onTorchAvailabilityChanged: (Boolean) -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var pausedRef by remember { mutableStateOf(isPaused) }
    pausedRef = isPaused

    val analyzer = remember {
        BarcodeScannerAnalyzer(
            isPaused = { pausedRef },
            onBarcodeDetected = onBarcodeDetected
        )
    }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(torchEnabled, boundCamera) {
        runCatching {
            boundCamera?.cameraControl?.enableTorch(torchEnabled)
        }
    }

    LaunchedEffect(isPaused) {
        if (!isPaused) {
            analyzer.resetLastBarcode()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    boundCamera = camera
                    onTorchAvailabilityChanged(camera.cameraInfo.hasFlashUnit())
                    camera.cameraControl.enableTorch(torchEnabled)
                } catch (e: Exception) {
                    Log.e("CameraPreviewView", "Failed to bind camera use cases", e)
                    onCameraError(e.localizedMessage ?: "Camera unavailable")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
