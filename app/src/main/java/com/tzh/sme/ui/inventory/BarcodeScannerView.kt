package com.tzh.sme.ui.inventory

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tzh.sme.R
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerView(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Use rememberUpdatedState to handle lambda changes without re-triggering the factory
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeScanned)

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    // Manage executor lifecycle at the Composable level
    val executor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context).await()
        } catch (e: Exception) {
            Log.e("BarcodeScanner", "Failed to get camera provider", e)
        }
    }
    BackHandler() {
        onClose()
    }
    // Cleanup resources when the Composable leaves the composition
    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProvider?.unbindAll()
            executor.shutdown()
        }
    }

    var lastScannedBarcode by remember { mutableStateOf("") }

    if (cameraProvider != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val scanner = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    scanner.setAnalyzer(executor, BarcodeAnalyzer { barcode ->
                        if (barcode != lastScannedBarcode) {
                            lastScannedBarcode = barcode
                            currentOnBarcodeDetected(barcode)
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

            Button(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                Text(stringResource(R.string.close_scanner))
            }
        }
    }
}
