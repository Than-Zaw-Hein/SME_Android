package com.tzh.sme.ui.inventory

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await // Ensure this import is here
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerView(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Use LaunchedEffect to handle the async camera provider initialization
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(Unit) {
        try {
            // .await() converts the ListenableFuture into a suspending function
            cameraProvider = ProcessCameraProvider.getInstance(context).await()
        } catch (e: Exception) {
            Log.e("BarcodeScanner", "Failed to get camera provider", e)
        }
    }

    var lastScannedBarcode by remember { mutableStateOf("") }

    if (cameraProvider != null) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = Executors.newSingleThreadExecutor()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val scanner = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                scanner.setAnalyzer(executor, BarcodeAnalyzer { barcode ->
                    if (barcode != lastScannedBarcode) {
                        lastScannedBarcode = barcode
                        onBarcodeDetected(barcode)
                    }
                })

                try {
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        scanner
                    )
                } catch (e: Exception) {
                    Log.e("BarcodeScanner", "Camera binding failed", e)
                }

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}