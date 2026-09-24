package ru.nksk.parentsapp.feature.scanner.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScannerUiState(
    val cameraGranted: Boolean = false,
    val permissionDecided: Boolean = false,
    /** False once a QR is captured; the camera stops and the entry navigates to the report. */
    val scanning: Boolean = true,
    val result: String? = null,
)

sealed interface ScannerAction {
    data class CameraPermission(val granted: Boolean) : ScannerAction
    data class QrDetected(val value: String) : ScannerAction
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    val frameAnalyzer: QrFrameAnalyzer,
) : ViewModel() {
    private val state = MutableStateFlow(ScannerUiState())
    val uiState = state.asStateFlow()

    fun onAction(action: ScannerAction) {
        when (action) {
            is ScannerAction.CameraPermission -> state.update {
                it.copy(cameraGranted = action.granted, permissionDecided = true)
            }
            is ScannerAction.QrDetected -> state.update {
                if (!it.scanning || it.result != null) it
                else it.copy(scanning = false, result = action.value)
            }
        }
    }
}
