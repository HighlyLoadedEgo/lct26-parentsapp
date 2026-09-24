package ru.nksk.parentsapp.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.nksk.parentsapp.app.navigation.AppStartupState
import ru.nksk.parentsapp.app.navigation.AppStartupViewModel
import ru.nksk.parentsapp.app.navigation.ParentsNavHost
import ru.nksk.parentsapp.core.ui.theme.ParentsAppTheme
import ru.nksk.parentsapp.feature.pin.navigation.PinLock
import ru.nksk.parentsapp.feature.pin.navigation.PinSetup
import ru.nksk.parentsapp.feature.report.navigation.Statistics

/** Composition root for the startup gate, shared presentation and app-owned navigation. */
@Composable
fun ParentsApp() {
    ParentsAppTheme {
        val startup: AppStartupViewModel = hiltViewModel()
        val state by startup.uiState.collectAsStateWithLifecycle()
        when (val current = state) {
            AppStartupState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            is AppStartupState.Ready -> ParentsNavHost(
                startKey = if (current.hasPin) PinLock else PinSetup,
            )
        }
    }
}
