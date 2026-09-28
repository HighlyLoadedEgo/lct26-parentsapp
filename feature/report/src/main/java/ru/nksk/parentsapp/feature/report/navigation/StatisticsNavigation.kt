package ru.nksk.parentsapp.feature.report.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.nksk.parentsapp.feature.report.ui.PetIdInputScreen
import ru.nksk.parentsapp.feature.report.ui.PetIdInputViewModel
import ru.nksk.parentsapp.feature.report.ui.StatisticsScreen
import ru.nksk.parentsapp.feature.report.ui.StatisticsViewModel

/** Root of the "Статистика" tab; a non-null [petId] is a fresh hand-off from the scanner. */
@Serializable
@SerialName("statistics")
data class Statistics(val petId: String? = null) : NavKey

@Serializable
@SerialName("pet_id_input")
data object PetIdInput : NavKey

fun EntryProviderScope<NavKey>.statisticsEntry(
    onScanQr: (Statistics) -> Unit,
    onManualInput: (Statistics) -> Unit,
    onChangePet: (Statistics) -> Unit,
    onOpenTopic: (Statistics, skillId: String, mastered: Boolean) -> Unit,
    onOpenQuests: (Statistics) -> Unit,
) {
    entry<Statistics> { source ->
        val viewModel: StatisticsViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(source) {
            viewModel.load(source.petId)
        }
        // After a confirmed reset the session is cleared first (resetDone is set only once the
        // clear completes); only then swap the entry so the fresh one reads an empty store.
        LaunchedEffect(state.resetDone) {
            if (state.resetDone) {
                onChangePet(source)
                viewModel.load(null)
                // Re-arm the signal so a later reset in the same entry triggers this again.
                viewModel.consumeReset()
            }
        }
        StatisticsScreen(
            state = state,
            onAction = viewModel::onAction,
            onScanQr = dropUnlessResumed { onScanQr(source) },
            onManualInput = dropUnlessResumed { onManualInput(source) },
            onOpenTopic = { skillId, mastered -> onOpenTopic(source, skillId, mastered) },
            onOpenQuests = dropUnlessResumed { onOpenQuests(source) },
        )
    }
}

fun EntryProviderScope<NavKey>.petIdInputEntry(
    onBack: (PetIdInput) -> Unit,
    onOpen: (PetIdInput, String) -> Unit,
) {
    entry<PetIdInput> { source ->
        val viewModel: PetIdInputViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(state.openId) {
            state.openId?.let { onOpen(source, it) }
        }
        PetIdInputScreen(
            state = state,
            onAction = viewModel::onAction,
            onBack = dropUnlessResumed { onBack(source) },
        )
    }
}
