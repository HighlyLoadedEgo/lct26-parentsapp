package ru.nksk.parentsapp.feature.pin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.nksk.parentsapp.feature.pin.ui.PinSetupScreen
import ru.nksk.parentsapp.feature.pin.ui.PinSetupViewModel

/** First-launch gate; fires [onDone] once the PIN is stored. */
@Composable
fun PinSetupEntry(onDone: () -> Unit) {
    val viewModel: PinSetupViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) {
        if (state.completed) onDone()
    }
    PinSetupScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
