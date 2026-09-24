package ru.nksk.parentsapp.feature.scanner.ui

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

/** Consumes CameraX frames and reports the raw value of the first detected QR code. */
interface QrFrameAnalyzer {
    fun analyze(imageProxy: ImageProxy, onQrDetected: (String) -> Unit)
}

internal class MlKitQrFrameAnalyzer @Inject constructor(
    private val scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
) : QrFrameAnalyzer {
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy, onQrDetected: (String) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue }?.let(onQrDetected)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
