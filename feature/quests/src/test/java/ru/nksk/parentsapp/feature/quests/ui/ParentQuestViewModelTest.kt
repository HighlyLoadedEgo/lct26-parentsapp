package ru.nksk.parentsapp.feature.quests.ui

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.nksk.parentsapp.core.report.rewards.*

@OptIn(ExperimentalCoroutinesApi::class)
class ParentQuestViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test fun availabilityAndFourChecksAreRequiredAndOwnedCapCannotBeIssued() = runTest(main.dispatcher) {
        val repo = FakeRewards()
        val model = ParentQuestViewModel(repo)
        model.load(ParentQuest.SHOPPING)
        repeat(4) { model.setStepChecked(it, true) }
        assertEquals(0, model.uiState.value.completedSteps)
        runCurrent()
        repeat(3) { model.setStepChecked(it, true) }
        model.complete(); runCurrent()
        assertEquals(0, repo.calls)
        model.setStepChecked(3, true)
        assertTrue(model.uiState.value.canComplete)
        repo.inventory.value = ParentRewardInventory("device", "run", setOf(cap)); runCurrent()
        assertTrue(model.uiState.value.selectedOwned)
        model.complete(); runCurrent()
        assertEquals(0, repo.calls)
    }

    @Test fun uncertainIssueLocksSelectionAndReopenRetriesSameCapOnlyOnClick() = runTest(main.dispatcher) {
        val repo = FakeRewards().apply { fail = true }
        val model = loaded(repo)
        repeat(4) { model.setStepChecked(it, true) }
        model.complete(); model.complete(); runCurrent()
        assertEquals(1, repo.calls)
        assertFalse(model.uiState.value.completed)
        model.selectItem(ParentRewardCaps.all[1].itemId)
        model.setStepChecked(0, false)
        assertEquals(cap, model.uiState.value.selectedItemId)
        assertEquals(4, model.uiState.value.completedSteps)
        val reopened = loaded(repo)
        assertEquals(1, repo.calls)
        assertTrue(reopened.uiState.value.canComplete)
        repo.fail = false
        reopened.complete(); runCurrent()
        assertTrue(reopened.uiState.value.completed)
        assertEquals(listOf(cap, cap), repo.items)
        // Standalone success does not mutate the child's inventory.
        assertTrue(repo.inventory.value!!.ownedItemIds.isEmpty())
        reopened.selectItem(ParentRewardCaps.all[1].itemId)
        reopened.complete(); runCurrent()
        assertEquals(2, repo.calls)
        assertEquals(cap, reopened.uiState.value.selectedItemId)
    }

    @Test fun deviceChangeWithSameRunCannotReceiveOldCompletion() = runTest(main.dispatcher) {
        val repo = FakeRewards()
        val model = loaded(repo)
        repeat(4) { model.setStepChecked(it, true) }
        repo.inventory.value = null; runCurrent()
        assertFalse(model.uiState.value.canComplete)
        repo.inventory.value = ParentRewardInventory("other", "run", emptySet()); runCurrent()
        assertTrue(model.uiState.value.targetChanged)
        model.complete(); runCurrent()
        assertEquals(0, repo.calls)
    }

    @Test fun runChangeCannotReceiveOldCompletionEvenAfterReturnToOriginalTarget() = runTest(main.dispatcher) {
        val repo = FakeRewards()
        val model = loaded(repo)
        repeat(4) { model.setStepChecked(it, true) }
        repo.inventory.value = ParentRewardInventory("device", "new-run", emptySet()); runCurrent()
        repo.inventory.value = ParentRewardInventory("device", "run", emptySet()); runCurrent()
        assertTrue(model.uiState.value.targetChanged)
        model.complete(); runCurrent()
        assertEquals(0, repo.calls)
    }

    @Test fun pendingLoadFailureCanBeRetriedAndInitialNullDoesNotInvalidatePendingTarget() = runTest(main.dispatcher) {
        val repo = FakeRewards().apply {
            pendingFails = true
            frozen = PendingParentQuestReward("device", "run", cap)
            emitInitialNull = true
        }
        val model = loaded(repo)
        assertFalse(model.uiState.value.ready)
        assertNotNull(model.uiState.value.error)
        repo.pendingFails = false
        model.load(ParentQuest.SHOPPING); runCurrent()
        assertTrue(model.uiState.value.canComplete)
        assertFalse(model.uiState.value.targetChanged)
        assertNull(model.uiState.value.error)
        assertEquals(0, repo.calls)
    }

    @Test fun busyStateFreezesChecklistAndPreventsDuplicateRequest() = runTest(main.dispatcher) {
        val repo = FakeRewards().apply { gate = CompletableDeferred() }
        val model = loaded(repo)
        repeat(4) { model.setStepChecked(it, true) }
        model.complete(); runCurrent()
        model.complete()
        model.setStepChecked(0, false)
        model.selectItem(ParentRewardCaps.all[1].itemId)
        assertEquals(4, model.uiState.value.completedSteps)
        assertEquals(cap, model.uiState.value.selectedItemId)
        assertEquals(1, repo.calls)
        repo.gate!!.complete(Unit); runCurrent()
        assertTrue(model.uiState.value.completed)
    }

    @Test fun firstAvailableCapIsSelectedAndAllOwnedInventoryBlocksFreshIssuance() = runTest(main.dispatcher) {
        val repo = FakeRewards().apply {
            inventory.value = ParentRewardInventory("device", "run", setOf(cap))
        }
        val model = loaded(repo)
        assertEquals(ParentRewardCaps.all[1].itemId, model.uiState.value.selectedItemId)
        model.selectItem("unknown-cap")
        assertEquals(ParentRewardCaps.all[1].itemId, model.uiState.value.selectedItemId)
        repeat(4) { model.setStepChecked(it, true) }
        repo.inventory.value = ParentRewardInventory("device", "run", ParentRewardCaps.all.map { it.itemId }.toSet())
        runCurrent()
        assertFalse(model.uiState.value.canComplete)
        model.complete(); runCurrent()
        assertEquals(0, repo.calls)
    }

    @Test fun pendingRewardCanRetryWhenItsGrantAlreadyAppearsInRemoteInventory() = runTest(main.dispatcher) {
        val repo = FakeRewards().apply {
            frozen = PendingParentQuestReward("device", "run", cap)
            inventory.value = ParentRewardInventory("device", "run", setOf(cap))
        }
        val model = loaded(repo)
        assertTrue(model.uiState.value.selectedOwned)
        assertTrue(model.uiState.value.canComplete)
        assertEquals(0, repo.calls)
        model.complete(); runCurrent()
        assertEquals(listOf(cap), repo.items)
        assertTrue(model.uiState.value.completed)
    }

    @Test fun pauseBlocksIssuanceAndResumePreservesChecksButRevalidatesTarget() = runTest(main.dispatcher) {
        val repo = FakeRewards()
        val model = loaded(repo)
        repeat(4) { model.setStepChecked(it, true) }
        model.stopObserving()
        model.complete(); runCurrent()
        assertFalse(model.uiState.value.ready)
        assertEquals(0, repo.calls)
        repo.inventory.value = ParentRewardInventory("other", "run", emptySet())
        model.load(ParentQuest.SHOPPING); runCurrent()
        assertEquals(4, model.uiState.value.completedSteps)
        assertTrue(model.uiState.value.targetChanged)
        assertFalse(model.uiState.value.canComplete)
    }

    @Test fun reloadClearsUncertainFallbackWhenRepositoryConfirmsNoPendingRequest() = runTest(main.dispatcher) {
        val repo = FakeRewards()
        val model = loaded(repo)
        repo.fail = true
        repo.pendingFails = true
        repeat(4) { model.setStepChecked(it, true) }
        model.complete(); runCurrent()
        assertNotNull(model.uiState.value.pending)
        repo.frozen = null
        repo.pendingFails = false
        model.stopObserving()
        model.load(ParentQuest.SHOPPING); runCurrent()
        assertNull(model.uiState.value.pending)
        assertFalse(model.uiState.value.selectionLocked)
    }

    @Test fun pendingLookupStartedDuringIssuanceCannotRestoreRequestAfterSuccess() = runTest(main.dispatcher) {
        val repo = FakeRewards().apply { gate = CompletableDeferred() }
        val model = loaded(repo)
        repeat(4) { model.setStepChecked(it, true) }
        model.complete(); runCurrent()
        model.stopObserving()
        repo.pendingGate = CompletableDeferred()
        model.load(ParentQuest.SHOPPING); runCurrent()
        repo.gate!!.complete(Unit); runCurrent()
        assertTrue(model.uiState.value.completed)
        repo.pendingGate!!.complete(Unit); runCurrent()
        assertTrue(model.uiState.value.completed)
        assertNull(model.uiState.value.pending)
        assertEquals(1, repo.calls)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.loaded(repo: FakeRewards): ParentQuestViewModel =
        ParentQuestViewModel(repo).also { it.load(ParentQuest.SHOPPING); runCurrent() }

    private class FakeRewards : ParentQuestRewardsRepository {
        val inventory = MutableStateFlow<ParentRewardInventory?>(ParentRewardInventory("device", "run", emptySet()))
        var frozen: PendingParentQuestReward? = null
        var calls = 0
        var fail = false
        var pendingFails = false
        var emitInitialNull = false
        var gate: CompletableDeferred<Unit>? = null
        var pendingGate: CompletableDeferred<Unit>? = null
        val items = mutableListOf<String>()
        override fun inventory() = flow {
            if (emitInitialNull) emit(null)
            inventory.collect { emit(it) }
        }
        override suspend fun pending(questId: String): PendingParentQuestReward? {
            if (pendingFails) throw IOException("Unavailable")
            val captured = frozen
            pendingGate?.await()
            return captured
        }
        override suspend fun issue(questId: String, itemId: String, expectedDeviceId: String, expectedRunId: String) {
            calls++; items += itemId
            frozen = PendingParentQuestReward(expectedDeviceId, expectedRunId, itemId)
            gate?.await()
            if (fail) throw IOException("Lost response")
            frozen = null
        }
    }
    companion object { private val cap = ParentRewardCaps.all.first().itemId }
}
