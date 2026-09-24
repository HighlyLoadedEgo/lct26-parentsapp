package ru.nksk.parentsapp.feature.quests.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nksk.parentsapp.feature.quests.R
import ru.nksk.parentsapp.feature.quests.data.ActiveQuest
import ru.nksk.parentsapp.feature.quests.data.QuestStore

data class QuestsUiState(
    val loading: Boolean = true,
    val activeQuest: ActiveQuest? = null,
    val title: String = "",
    val reward: String = "",
    @StringRes val errorRes: Int? = null,
)

sealed interface QuestsAction {
    data class TitleChanged(val value: String) : QuestsAction
    data class RewardChanged(val value: String) : QuestsAction
    data object Create : QuestsAction
    data object Complete : QuestsAction
}

/** Backing screen of the quests section: create a real-life quest and track the active one. */
@HiltViewModel
class QuestsViewModel @Inject constructor(
    private val questStore: QuestStore,
) : ViewModel() {
    private val state = MutableStateFlow(QuestsUiState())
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            questStore.observeActiveQuest().collect { quest ->
                state.update { it.copy(loading = false, activeQuest = quest) }
            }
        }
    }

    fun onAction(action: QuestsAction) {
        when (action) {
            is QuestsAction.TitleChanged -> state.update {
                it.copy(title = action.value, errorRes = null)
            }
            is QuestsAction.RewardChanged -> state.update {
                it.copy(reward = action.value.filter { char -> char.isDigit() }, errorRes = null)
            }
            QuestsAction.Create -> create()
            QuestsAction.Complete -> viewModelScope.launch {
                questStore.clearActiveQuest()
            }
        }
    }

    private fun create() {
        val title = state.value.title.trim()
        val reward = state.value.reward.toIntOrNull()
        if (title.isEmpty() || reward == null || reward <= 0) {
            state.update { it.copy(errorRes = R.string.quests_invalid_input) }
            return
        }
        viewModelScope.launch {
            questStore.setActiveQuest(ActiveQuest(title = title, rewardCoins = reward))
            state.update { it.copy(title = "", reward = "", errorRes = null) }
        }
    }
}
