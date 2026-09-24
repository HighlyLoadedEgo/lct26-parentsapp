package ru.nksk.parentsapp.feature.scanner.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.nksk.parentsapp.feature.scanner.ui.ScannerAction
import ru.nksk.parentsapp.feature.scanner.ui.ScannerScreen
import ru.nksk.parentsapp.feature.scanner.ui.ScannerViewModel

/** Entry point of the scanner feature; owns the ViewModel and the camera permission flow. */
@Composable
fun ScannerEntry(
    onScanned: (String) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: ScannerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onAction(ScannerAction.CameraPermission(granted))
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onAction(ScannerAction.CameraPermission(granted))
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(state.result) {
        state.result?.let(onScanned)
    }
    ScannerScreen(
        state = state,
        frameAnalyzer = viewModel.frameAnalyzer,
        onAction = viewModel::onAction,
        onBack = onBack,
        onRequestCameraPermission = {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        },
    )
}
