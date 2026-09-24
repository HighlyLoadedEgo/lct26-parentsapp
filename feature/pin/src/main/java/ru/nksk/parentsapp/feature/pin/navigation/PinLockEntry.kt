package ru.nksk.parentsapp.feature.pin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.nksk.parentsapp.feature.pin.ui.PinLockScreen
import ru.nksk.parentsapp.feature.pin.ui.PinLockViewModel

/** Launch gate for every run after setup; fires [onUnlocked] on a correct PIN. */
@Composable
fun PinLockEntry(onUnlocked: () -> Unit) {
    val viewModel: PinLockViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.unlocked) {
        if (state.unlocked) onUnlocked()
    }
    PinLockScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
