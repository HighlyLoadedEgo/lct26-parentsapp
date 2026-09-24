package ru.nksk.parentsapp.feature.scanner.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import ru.nksk.parentsapp.feature.scanner.R

@Composable
fun ScannerScreen(
    state: ScannerUiState,
    frameAnalyzer: QrFrameAnalyzer,
    onAction: (ScannerAction) -> Unit,
    onBack: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.scanner_back),
                )
            }
            Text(
                text = stringResource(R.string.scanner_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        when {
            !state.cameraGranted -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.scanner_camera_rationale),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onRequestCameraPermission,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.scanner_grant_camera))
                }
            }
            else -> Box(modifier = Modifier.fillMaxSize()) {
                ScannerCameraPreview(
                    active = state.scanning,
                    frameAnalyzer = frameAnalyzer,
                    onQrDetected = { onAction(ScannerAction.QrDetected(it)) },
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = stringResource(R.string.scanner_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Binds the back camera while active; frames feed the analyzer only until [active] turns false. */
@Composable
private fun ScannerCameraPreview(
    active: Boolean,
    frameAnalyzer: QrFrameAnalyzer,
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val preview = remember { Preview.Builder().build() }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    val frameExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(active, lifecycleOwner) {
        var cameraProvider: ProcessCameraProvider? = null
        if (active) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                val provider = future.get()
                cameraProvider = provider
                imageAnalysis.setAnalyzer(frameExecutor) { imageProxy ->
                    frameAnalyzer.analyze(imageProxy, onQrDetected)
                }
                preview.setSurfaceProvider(previewView.surfaceProvider)
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose {
            cameraProvider?.unbindAll()
            imageAnalysis.clearAnalyzer()
        }
    }
    DisposableEffect(Unit) {
        onDispose { frameExecutor.shutdown() }
    }
    AndroidView(modifier = modifier, factory = { previewView })
}
