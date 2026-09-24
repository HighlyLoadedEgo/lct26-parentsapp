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

    fun load(skillId: String) {
        loadedSkillId = skillId
        viewModelScope.launch {
            val petId = sessionStore.lastPetId()
            if (petId == null) {
                state.update {
                    it.copy(loading = false, topic = null, errorRes = R.string.questions_load_error)
                }
            } else {
                fetch(petId, skillId)
            }
        }
    }

    fun onAction(action: QuestionTopicAction) {
        when (action) {
            QuestionTopicAction.Reload -> viewModelScope.launch {
                val skillId = loadedSkillId
                val petId = sessionStore.lastPetId()
                if (skillId != null && petId != null) fetch(petId, skillId)
            }
        }
    }

    private fun fetch(petId: String, skillId: String) {
        state.update { it.copy(loading = true, errorRes = null) }
        viewModelScope.launch {
            runCatching { repository.getReport(petId).skills.find { it.id == skillId } }
                .onSuccess { found ->
                    state.update {
                        it.copy(
                            loading = false,
                            topic = found,
                            errorRes = if (found == null) R.string.questions_load_error else null,
                        )
                    }
                }
                .onFailure {
                    state.update { current ->
                        current.copy(loading = false, errorRes = R.string.questions_load_error)
                    }
                }
        }
    }
}
