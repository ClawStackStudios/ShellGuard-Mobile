package com.clawstack.shellguard.ui.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX ImageAnalysis.Analyzer utilizing Google ML Kit BarcodeScanning.
 *
 * Detects QR codes with zero frame leaks, automatically releasing ImageProxy buffers.
 */
class QrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    @Volatile
    private var isScanningEnabled = true

    fun setScanningEnabled(enabled: Boolean) {
        isScanningEnabled = enabled
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanningEnabled) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.valueType == Barcode.TYPE_TEXT || barcode.valueType == Barcode.TYPE_URL) {
                            barcode.rawValue?.let { code ->
                                if (isScanningEnabled) {
                                    isScanningEnabled = false
                                    onQrCodeDetected(code)
                                }
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
