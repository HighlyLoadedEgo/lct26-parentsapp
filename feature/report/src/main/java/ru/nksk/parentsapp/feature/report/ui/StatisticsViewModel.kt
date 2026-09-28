package ru.nksk.parentsapp.feature.report.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nksk.parentsapp.feature.report.R
import ru.nksk.parentsapp.core.report.data.ParentReportResponse
import ru.nksk.parentsapp.core.report.data.ParentReportRepository
import ru.nksk.parentsapp.core.report.data.PetSessionStore

data class StatisticsUiState(
    val loading: Boolean = false,
    val report: ParentReportResponse? = null,
    @StringRes val errorRes: Int? = null,
    /** True after the user asked to forget this pet; the entry then shows the empty chooser. */
    val resetDone: Boolean = false,
)

sealed interface StatisticsAction {
    data object Reload : StatisticsAction
    data object ChangePet : StatisticsAction
}

/** Backing screen of the "Статистика" tab; shows the remembered pet's report or an empty state. */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: ParentReportRepository,
    private val sessionStore: PetSessionStore,
) : ViewModel() {
    private val state = MutableStateFlow(StatisticsUiState())
    val uiState = state.asStateFlow()

    private var loadedPetId: String? = null

    /**
     * Called every time the tab becomes visible. A non-null [scannedPetId] (handed over by the
     * scanner) always triggers a fresh fetch; otherwise the remembered pet is re-read so external
     * changes to the session are picked up.
     */
    fun load(scannedPetId: String?) {
        viewModelScope.launch {
            val petId = scannedPetId ?: sessionStore.lastPetId()
            // Skip the refetch only for a known pet that is already on screen.
            if (petId != null && petId == loadedPetId && state.value.report != null) return@launch
            when (petId) {
                null -> state.update {
                    loadedPetId = null
                    it.copy(loading = false, report = null, errorRes = null)
                }
                // Only a fresh hand-off (scanner) may persist; re-writing the store on a
                // session reload would undo a reset that cleared it concurrently.
                else -> fetch(petId, persistOnSuccess = scannedPetId != null)
            }
        }
    }

    fun onAction(action: StatisticsAction) {
        when (action) {
            StatisticsAction.Reload -> viewModelScope.launch {
                sessionStore.lastPetId()?.let { fetch(it, persistOnSuccess = false) }
            }
            // Clears the remembered pet; the entry then returns to the empty chooser state.
            StatisticsAction.ChangePet -> viewModelScope.launch {
                sessionStore.clearPetId()
                loadedPetId = null
                state.update { it.copy(resetDone = true) }
            }
        }
    }

    /** Re-arms the one-shot [StatisticsUiState.resetDone] signal once the UI has consumed it. */
    fun consumeReset() {
        state.update { if (it.resetDone) it.copy(resetDone = false) else it }
    }

    private fun fetch(petId: String, persistOnSuccess: Boolean) {
        state.update { it.copy(loading = true, errorRes = null) }
        viewModelScope.launch {
            runCatching { repository.getReport(petId) }
                .onSuccess { report ->
                    if (persistOnSuccess) sessionStore.savePetId(petId)
                    loadedPetId = petId
                    state.update { it.copy(loading = false, report = report) }
                }
                .onFailure {
                    state.update { current ->
                        current.copy(loading = false, errorRes = R.string.report_load_error)
                    }
                }
        }
    }
}
