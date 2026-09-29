package ru.nksk.parentsapp.feature.quests.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nksk.parentsapp.core.report.rewards.ParentQuestRewardsRepository
import ru.nksk.parentsapp.core.report.rewards.ParentQuestRewardException
import ru.nksk.parentsapp.core.report.rewards.PendingParentQuestReward
import ru.nksk.parentsapp.core.report.rewards.ParentRewardCaps

@Immutable
data class ParentQuestUiState(
    val checkedSteps: List<Boolean> = List(4) { false },
    val completed: Boolean = false,
    val selectedItemId: String = ParentRewardCaps.all.first().itemId,
    val ownedItemIds: Set<String> = emptySet(),
    val deviceId: String? = null,
    val gameRunId: String? = null,
    val pending: PendingParentQuestReward? = null,
    val ready: Boolean = false,
    val busy: Boolean = false,
    val targetChanged: Boolean = false,
    val error: String? = null,
) {
    val completedSteps: Int get() = checkedSteps.count { it }
    val selectionLocked: Boolean get() = !ready || busy || completed || pending != null || targetChanged
    val selectedOwned: Boolean get() = selectedItemId in ownedItemIds
    val canComplete: Boolean get() = ready && !busy && !completed && !targetChanged &&
        deviceId != null && gameRunId != null && checkedSteps.all { it } && (!selectedOwned || pending != null)
}

@HiltViewModel
class ParentQuestViewModel @Inject constructor(private val rewards: ParentQuestRewardsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ParentQuestUiState())
    val uiState = mutableState.asStateFlow()
    private var quest: ParentQuest? = null
    private var observer: Job? = null

    fun load(value: ParentQuest) {
        if (quest == value && observer?.isActive == true) return
        if (quest != null && quest != value) return // A navigation entry owns exactly one quest.
        quest = value
        observer?.cancel()
        mutableState.update { it.copy(ready = false, error = null) }
        observer = viewModelScope.launch {
            try {
                val loadingDuringIssue = mutableState.value.busy
                val pending = rewards.pending(value.name)
                mutableState.update { state ->
                    // A lookup started during issuance may return an already-cleared request.
                    // Reconcile only idle loads, including clearing an uncertain in-memory fallback.
                    if (loadingDuringIssue || state.busy || state.completed) state
                    else if (pending == null) state.copy(pending = null)
                    else {
                        val changed = state.deviceId != null &&
                            (state.deviceId != pending.deviceId || state.gameRunId != pending.gameRunId)
                        if (changed) state.copy(targetChanged = true, error = TARGET_CHANGED)
                        else state.copy(pending = pending, selectedItemId = pending.itemId,
                            deviceId = pending.deviceId, gameRunId = pending.gameRunId, checkedSteps = List(4) { true })
                    }
                }
                rewards.inventory().collect { inventory ->
                    mutableState.update { state ->
                        // A null emission means availability is still unknown, not a new game.
                        val changed = state.targetChanged || (inventory != null && state.deviceId != null &&
                            (state.deviceId != inventory.deviceId || state.gameRunId != inventory.gameRunId))
                        val firstChoice = if (state.deviceId == null && inventory != null)
                            ParentRewardCaps.all.firstOrNull { it.itemId !in inventory.ownedItemIds }?.itemId else null
                        state.copy(ready = inventory != null, ownedItemIds = inventory?.ownedItemIds.orEmpty(),
                            deviceId = state.deviceId ?: inventory?.deviceId,
                            gameRunId = state.gameRunId ?: inventory?.gameRunId,
                            selectedItemId = firstChoice ?: state.selectedItemId, targetChanged = changed,
                            error = when {
                                changed -> TARGET_CHANGED
                                inventory == null -> null
                                else -> state.error
                            })
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.update { it.copy(ready = false,
                    error = (failure as? ParentQuestRewardException)?.message ?:
                        "Не удалось загрузить инвентарь. Проверьте интернет и попробуйте ещё раз.") }
            }
        }
    }

    fun stopObserving() {
        observer?.cancel()
        observer = null
        mutableState.update { it.copy(ready = false) }
    }

    fun selectItem(itemId: String) {
        if (ParentRewardCaps.forItem(itemId) == null) return
        mutableState.update { if (it.selectionLocked) it else it.copy(selectedItemId = itemId, error = null) }
    }

    fun setStepChecked(index: Int, checked: Boolean) {
        mutableState.update { state ->
            if (state.selectionLocked) state else state.copy(
                checkedSteps = state.checkedSteps.mapIndexed { step, value -> if (step == index) checked else value })
        }
    }

    fun complete() {
        val state = mutableState.value
        val currentQuest = quest ?: return
        if (!state.canComplete) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                rewards.issue(currentQuest.name, state.selectedItemId,
                    checkNotNull(state.deviceId), checkNotNull(state.gameRunId))
                // Only server acceptance is confirmed here. The game owns inventory delivery.
                mutableState.update { it.copy(completed = true, busy = false, pending = null,
                    error = if (it.targetChanged) TARGET_CHANGED else null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val fallback = PendingParentQuestReward(checkNotNull(state.deviceId),
                    checkNotNull(state.gameRunId), state.selectedItemId)
                val pending = try {
                    rewards.pending(currentQuest.name)?.let {
                        if (it.deviceId == state.deviceId && it.gameRunId == state.gameRunId) it else fallback
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) { fallback }
                mutableState.update { it.copy(busy = false, pending = pending,
                    error = if (it.targetChanged) TARGET_CHANGED else
                        (failure as? ParentQuestRewardException)?.message ?:
                        "Не удалось подтвердить выдачу. Проверьте интернет и повторите — вторая награда не создастся.") }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        const val TARGET_CHANGED = "Профиль или прохождение изменились. Вернитесь к списку квестов."
    }
}
