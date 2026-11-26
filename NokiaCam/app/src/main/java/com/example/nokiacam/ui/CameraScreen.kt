package com.example.nokiacam.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.nokiacam.camera.CameraMode
import com.example.nokiacam.camera.ImageProcessor
import com.example.nokiacam.camera.PhotoStorage
import java.util.concurrent.Executors

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }

    var mode by remember { mutableStateOf(CameraMode.NOKIA) }
    var status by remember { mutableStateOf("Ready") }
    var isCapturing by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
    ) {
        if (hasPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageCapture = imageCapture,
                lifecycleOwner = lifecycleOwner
            )
        } else {
            PermissionPrompt(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        }

        TopBar(mode = mode, onModeChange = { mode = it }, modifier = Modifier.align(Alignment.TopCenter))

        BottomBar(
            mode = mode,
            status = status,
            isCapturing = isCapturing,
            onCapture = {
                if (!hasPermission || isCapturing) return@BottomBar
                isCapturing = true
                status = "Capturing..."
                capturePhoto(
                    imageCapture = imageCapture,
                    executor = cameraExecutor,
                    mainExecutor = mainExecutor,
                    onBytes = { bytes ->
                        try {
                            val decoded = ImageProcessor.decode(bytes)
                            val processed = ImageProcessor.applyMode(decoded, mode)
                            if (processed !== decoded) decoded.recycle()
                            PhotoStorage.saveBitmap(context, processed, mode)
                            processed.recycle()
                            status = "Saved • ${mode.label}"
                        } catch (e: Exception) {
                            status = "Error: ${e.message ?: "save failed"}"
                        } finally {
                            isCapturing = false
                        }
                    },
                    onError = { e ->
                        status = "Error: ${e.message ?: "capture failed"}"
                        isCapturing = false
                    }
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier,
    imageCapture: ImageCapture,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (_: Exception) {
                    // Keep UI alive; status is handled by capture flow.
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
private fun TopBar(mode: CameraMode, onModeChange: (CameraMode) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xAA050505))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NOKIACAM",
            color = Color(0xFFEAEAEA),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeChip("NOKIA", selected = mode == CameraMode.NOKIA) { onModeChange(CameraMode.NOKIA) }
            ModeChip("RETRO", selected = mode == CameraMode.RETRO) { onModeChange(CameraMode.RETRO) }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFFEAEAEA) else Color.Transparent
    val fg = if (selected) Color(0xFF111111) else Color(0xFFBDBDBD)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, Color(0xFF6A6A6A), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    }
}

@Composable
private fun BottomBar(
    mode: CameraMode,
    status: String,
    isCapturing: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC050505))
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = status,
            color = Color(0xFFCFCFCF),
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = mode.label,
                color = Color(0xFF8E8E8E),
                fontSize = 11.sp,
                modifier = Modifier.width(64.dp),
                textAlign = TextAlign.Center
            )
            ShutterButton(isCapturing = isCapturing, onClick = onCapture)
            Spacer(Modifier.width(64.dp))
        }
    }
}

@Composable
private fun ShutterButton(isCapturing: Boolean, onClick: () -> Unit) {
    val border = if (isCapturing) Color(0xFF888888) else Color(0xFFEAEAEA)
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .border(3.dp, border, CircleShape)
            .clickable(enabled = !isCapturing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isCapturing) Color(0xFF777777) else Color(0xFFEAEAEA))
        )
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera access needed", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(
            "NokiaCam needs camera permission to take photos.",
            color = Color(0xFFBBBBBB),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFEAEAEA))
                .clickable(onClick = onRequest)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("Allow camera", color = Color(0xFF111111), fontWeight = FontWeight.Medium)
        }
    }
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    mainExecutor: java.util.concurrent.Executor,
    onBytes: (ByteArray) -> Unit,
    onError: (Exception) -> Unit
) {
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
            val bytes = try {
                val buffer = image.planes[0].buffer
                val data = ByteArray(buffer.remaining())
                buffer.get(data)
                data
            } catch (e: Exception) {
                image.close()
                mainExecutor.execute { onError(e) }
                return
            }
            image.close()
            mainExecutor.execute { onBytes(bytes) }
        }

        override fun onError(exception: ImageCaptureException) {
            mainExecutor.execute { onError(exception) }
        }
    })
}
