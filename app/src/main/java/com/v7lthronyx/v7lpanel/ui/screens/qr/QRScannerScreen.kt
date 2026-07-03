package com.v7lthronyx.v7lpanel.ui.screens.qr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.v7lthronyx.v7lpanel.ui.theme.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QRScannerScreen(
    onResult: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            CameraPreview(onResult = onResult)
            // Viewfinder overlay
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .border(2.dp, LocalAccent.current, RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        S.scanQr,
                        color = Color.White,
                        fontFamily = FiraCode,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        S.cameraPermNeeded,
                        color = Color.White,
                        fontFamily = FiraCode,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current)
                    ) {
                        Text(S.grantPermission,
                            fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
    }
}

@Composable
private fun CameraPreview(onResult: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val mainHandler    = remember { Handler(Looper.getMainLooper()) }
    // Use AtomicBoolean for thread-safe flag (accessed from analyzer thread)
    val hasResult      = remember { AtomicBoolean(false) }

    val reader = remember {
        MultiFormatReader().apply {
            setHints(mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            ))
        }
    }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    if (!hasResult.get()) {
                        val result = decodeQR(imageProxy, reader)
                        if (result != null && hasResult.compareAndSet(false, true)) {
                            // Post result to main thread for safe Compose navigation
                            mainHandler.post { onResult(result) }
                        }
                    }
                    imageProxy.close()
                }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun decodeQR(imageProxy: ImageProxy, reader: MultiFormatReader): String? {
    return try {
        val mediaImage = imageProxy.image ?: return null
        val yPlane     = mediaImage.planes[0]
        val yBuffer    = yPlane.buffer
        val width      = imageProxy.width
        val height     = imageProxy.height
        val rowStride  = yPlane.rowStride

        // Only need luminance (Y plane) for ZXing. Handle row stride padding.
        val data: ByteArray
        if (rowStride == width) {
            data = ByteArray(yBuffer.remaining())
            yBuffer.get(data)
        } else {
            // Row stride is larger than width — copy row by row to remove padding
            data = ByteArray(width * height)
            for (row in 0 until height) {
                yBuffer.position(row * rowStride)
                yBuffer.get(data, row * width, width)
            }
        }

        val source = PlanarYUVLuminanceSource(
            data, width, height,
            0, 0, width, height, false
        )
        val bmp = BinaryBitmap(HybridBinarizer(source))
        reader.decodeWithState(bmp).text.also { reader.reset() }
    } catch (_: NotFoundException) {
        reader.reset()
        null
    } catch (_: Exception) {
        reader.reset()
        null
    }
}
