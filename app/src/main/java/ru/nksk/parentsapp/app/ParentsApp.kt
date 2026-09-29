package ru.nksk.parentsapp.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.nksk.parentsapp.app.navigation.ParentsNavHost
import ru.nksk.parentsapp.app.updates.AppUpdateHost
import ru.nksk.parentsapp.core.ui.theme.ParentsAppTheme
import ru.nksk.parentsapp.feature.pin.access.ParentsAccessViewModel
import ru.nksk.parentsapp.feature.pin.access.ParentsPinScreen
import ru.nksk.parentsapp.feature.report.navigation.Statistics

/** In-memory PIN gate precedes even a restored navigation stack. */
@Composable
fun ParentsApp(access: ParentsAccessViewModel, onClose: () -> Unit) {
    ParentsAppTheme {
        val state by access.uiState.collectAsStateWithLifecycle()
        if (state.unlocked) {
            ParentsNavHost(startKey = Statistics())
            AppUpdateHost()
        } else {
            ParentsPinScreen(state, access::onDigit, access::onDelete, access::retry, onClose)
        }
    }
}
