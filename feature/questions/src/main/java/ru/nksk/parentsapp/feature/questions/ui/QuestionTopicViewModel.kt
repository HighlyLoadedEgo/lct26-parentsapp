package ru.nksk.parentsapp.feature.questions.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nksk.parentsapp.core.report.data.ParentReportRepository
import ru.nksk.parentsapp.core.report.data.PetSessionStore
import ru.nksk.parentsapp.core.report.data.SkillDto
import ru.nksk.parentsapp.feature.questions.R

data class QuestionTopicUiState(
    val loading: Boolean = false,
    val topic: SkillDto? = null,
    @StringRes val errorRes: Int? = null,
)

sealed interface QuestionTopicAction {
    data object Reload : QuestionTopicAction
}

/** Backing screen of one topic's practice; resolves the topic by its stable id. */
@HiltViewModel
class QuestionTopicViewModel @Inject constructor(
    private val repository: ParentReportRepository,
    private val sessionStore: PetSessionStore,
) : ViewModel() {
    private val state = MutableStateFlow(QuestionTopicUiState())
    val uiState = state.asStateFlow()

    private var loadedSkillId: String? = null

    private var request: kotlinx.coroutines.Job? = null
    private var generation = 0L

    fun load(skillId: String) {
        if (loadedSkillId != skillId) state.value = QuestionTopicUiState()
        loadedSkillId = skillId
        refresh()
    }

    fun onAction(action: QuestionTopicAction) {
        when (action) { QuestionTopicAction.Reload -> refresh() }
    }

    fun cancelRefresh() {
        generation++
        request?.cancel()
        state.update { it.copy(loading = false) }
    }

    private fun refresh() {
        val skillId = loadedSkillId ?: return
        cancelRefresh()
        val token = generation
        state.update { it.copy(loading = true, errorRes = null) }
        request = viewModelScope.launch {
            try {
                val petId = sessionStore.lastPetId() ?: error("No selected profile")
                val report = repository.getReport(petId)
                if (token != generation) return@launch
                require(report.pet.id == petId) { "Report belongs to another device" }
                val found = report.skills.find { it.id == skillId }
                state.value = QuestionTopicUiState(topic = found,
                    errorRes = if (found == null) R.string.questions_load_error else null)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (token == generation) state.update {
                    it.copy(loading = false, errorRes = R.string.questions_load_error)
                }
            }
        }
    }
}
