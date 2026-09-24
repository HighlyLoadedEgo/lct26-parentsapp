package ru.nksk.parentsapp.feature.scanner.ui

import androidx.camera.core.ImageProxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerViewModelTest {
    private class FakeAnalyzer : QrFrameAnalyzer {
        override fun analyze(imageProxy: ImageProxy, onQrDetected: (String) -> Unit) = Unit
    }

    @Test
    fun `initial state waits for camera permission and scans`() {
        val viewModel = ScannerViewModel(FakeAnalyzer())
        val state = viewModel.uiState.value
        assertFalse(state.cameraGranted)
        assertFalse(state.permissionDecided)
        assertTrue(state.scanning)
        assertNull(state.result)
    }

    @Test
    fun `granted permission makes camera ready`() {
        val viewModel = ScannerViewModel(FakeAnalyzer())
        viewModel.onAction(ScannerAction.CameraPermission(granted = true))
        val state = viewModel.uiState.value
        assertTrue(state.cameraGranted)
        assertTrue(state.permissionDecided)
    }

    @Test
    fun `detected qr pauses scanning and stores result once`() {
        val viewModel = ScannerViewModel(FakeAnalyzer())
        viewModel.onAction(ScannerAction.CameraPermission(granted = true))
        viewModel.onAction(ScannerAction.QrDetected("first"))
        viewModel.onAction(ScannerAction.QrDetected("second"))
        val state = viewModel.uiState.value
        assertFalse(state.scanning)
        assertEquals("first", state.result)
    }
}
