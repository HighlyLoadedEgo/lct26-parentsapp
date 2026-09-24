package ru.nksk.parentsapp.feature.quests.ui

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import ru.nksk.parentsapp.feature.quests.R
import ru.nksk.parentsapp.feature.quests.data.ActiveQuest

class QuestsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun creationFillsTheStoreAndClearsTheForm() = runTest {
        val store = FakeQuestStore()
        val model = QuestsViewModel(store)
        advanceUntilIdle()

        model.onAction(QuestsAction.TitleChanged("Помыть посуду"))
        model.onAction(QuestsAction.RewardChanged("50"))
        model.onAction(QuestsAction.Create)
        advanceUntilIdle()

        assertEquals(ActiveQuest("Помыть посуду", 50), store.current)
        assertEquals("", model.uiState.value.title)
        assertEquals("", model.uiState.value.reward)
    }

    @Test fun invalidInputIsRejectedWithoutWritingTheStore() = runTest {
        val store = FakeQuestStore()
        val model = QuestsViewModel(store)
        advanceUntilIdle()

        model.onAction(QuestsAction.TitleChanged("  "))
        model.onAction(QuestsAction.RewardChanged("0"))
        model.onAction(QuestsAction.Create)
        advanceUntilIdle()

        assertEquals(R.string.quests_invalid_input, model.uiState.value.errorRes)
        assertNull(store.current)
    }

    @Test fun rewardKeepsDigitsOnly() = runTest {
        val model = QuestsViewModel(FakeQuestStore())
        advanceUntilIdle()

        model.onAction(QuestsAction.RewardChanged("12ab34"))
        assertEquals("1234", model.uiState.value.reward)
    }

    @Test fun activeQuestIsObservedFromTheStoreAndCompletable() = runTest {
        val store = FakeQuestStore()
        store.current = ActiveQuest("Вынести мусор", 20)
        val model = QuestsViewModel(store)
        advanceUntilIdle()

        assertNotNull(model.uiState.value.activeQuest)

        model.onAction(QuestsAction.Complete)
        advanceUntilIdle()

        assertNull(store.current)
        assertNull(model.uiState.value.activeQuest)
    }
}
