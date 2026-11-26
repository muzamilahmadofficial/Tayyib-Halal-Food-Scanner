package com.example.scanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.core.BarcodeValidator
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeScannerAnalyzer(
    private val isPaused: () -> Boolean,
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_QR_CODE
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private val isProcessingFrame = AtomicBoolean(false)
    private var lastBarcode: String? = null
    private var lastDetectedAtMs: Long = 0L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused() || !isProcessingFrame.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            isProcessingFrame.set(false)
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val raw = barcodes.firstNotNullOfOrNull { it.rawValue }
                if (raw != null) {
                    val normalized = BarcodeValidator.normalizeAndValidate(raw)
                    val now = System.currentTimeMillis()
                    if (normalized != null) {
                        val isDuplicateInCooldown =
                            normalized == lastBarcode && (now - lastDetectedAtMs) < DUPLICATE_COOLDOWN_MS
                        if (!isDuplicateInCooldown) {
                            lastBarcode = normalized
                            lastDetectedAtMs = now
                            onBarcodeDetected(normalized)
                        }
                    }
                }
            }
            .addOnCompleteListener {
                isProcessingFrame.set(false)
                imageProxy.close()
            }
    }

    fun resetLastBarcode() {
        lastBarcode = null
        lastDetectedAtMs = 0L
    }

    fun close() {
        scanner.close()
    }

    companion object {
        private const val DUPLICATE_COOLDOWN_MS = 3500L
    }
}
