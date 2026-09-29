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
import kotlinx.coroutines.sync.withLock
import ru.nksk.parentsapp.feature.report.R
import ru.nksk.parentsapp.core.report.data.ParentReportResponse
import ru.nksk.parentsapp.core.report.data.ParentReportRepository
import ru.nksk.parentsapp.core.report.data.PetSessionStore

data class StatisticsUiState(
    val loading: Boolean = false,
    val resetting: Boolean = false,
    val report: ParentReportResponse? = null,
    @StringRes val errorRes: Int? = null,
    /** True after the user asked to forget this pet; the entry then shows the empty chooser. */
    val resetDone: Boolean = false,
)

sealed interface StatisticsAction {
    data object Reload : StatisticsAction
    data object ChangePet : StatisticsAction
}

/** Owns one selected profile request; reset invalidates even a late non-cancellable response. */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: ParentReportRepository,
    private val sessionStore: PetSessionStore,
) : ViewModel() {
    private val state = MutableStateFlow(StatisticsUiState())
    val uiState = state.asStateFlow()
    private var requestedPetId: String? = null
    private var persistOnSuccess = false
    private var generation = 0L
    private var request: kotlinx.coroutines.Job? = null
    private val sessionWrites = kotlinx.coroutines.sync.Mutex()

    fun load(scannedPetId: String?) {
        if (state.value.resetting) return
        requestedPetId = scannedPetId
        persistOnSuccess = scannedPetId != null
        refresh()
    }

    fun onAction(action: StatisticsAction) {
        when (action) {
            StatisticsAction.Reload -> refresh()
            StatisticsAction.ChangePet -> {
                cancelRefresh()
                requestedPetId = null
                persistOnSuccess = false
                if (state.value.resetting) return
                state.update { it.copy(resetting = true, errorRes = null) }
                viewModelScope.launch {
                    try {
                        sessionWrites.withLock { sessionStore.clearPetId() }
                        state.value = StatisticsUiState(resetDone = true)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        state.update { it.copy(resetting = false, errorRes = R.string.report_reset_error) }
                    }
                }
            }
        }
    }

    fun consumeReset() { state.update { it.copy(resetDone = false) } }

    fun cancelRefresh() {
        generation++
        request?.cancel()
        state.update { it.copy(loading = false) }
    }

    private fun refresh() {
        if (state.value.resetting || state.value.resetDone) return
        cancelRefresh()
        val token = generation
        request = viewModelScope.launch {
            try {
                val petId = requestedPetId ?: sessionStore.lastPetId()
                if (token != generation) return@launch
                if (petId == null) {
                    state.value = StatisticsUiState()
                    return@launch
                }
                requestedPetId = petId
                state.update { it.copy(loading = true, errorRes = null,
                    report = it.report?.takeIf { report -> report.pet.id == petId }) }
                val report = repository.getReport(petId)
                if (token != generation) return@launch
                require(report.pet.id == petId) { "Report belongs to another device" }
                sessionWrites.withLock {
                    if (token == generation && persistOnSuccess) sessionStore.savePetId(petId)
                }
                if (token != generation) return@launch
                persistOnSuccess = false
                state.value = StatisticsUiState(report = report)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (token == generation) state.update {
                    it.copy(loading = false, errorRes = R.string.report_load_error)
                }
            }
        }
    }
}
