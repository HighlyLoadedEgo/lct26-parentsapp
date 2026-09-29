package ru.nksk.parentsapp.feature.quests.ui

import org.junit.Assert.*
import org.junit.Test

class ParentQuestViewModelTest {
    @Test fun `completion requires all four checks and freezes the completed demo`() {
        val model = ParentQuestViewModel()
        repeat(3) { model.setStepChecked(it, true) }
        model.complete()
        assertFalse(model.uiState.value.completed)
        model.setStepChecked(3, true)
        assertTrue(model.uiState.value.canComplete)
        model.complete()
        model.setStepChecked(0, false)
        assertTrue(model.uiState.value.completed)
        assertEquals(4, model.uiState.value.completedSteps)
        assertFalse(model.uiState.value.canComplete)
    }
    @Test fun `separate quest entries and reopening start with no progress`() {
        val first = ParentQuestViewModel()
        repeat(4) { first.setStepChecked(it, true) }
        first.complete()
        val second = ParentQuestViewModel()
        assertEquals(0, second.uiState.value.completedSteps)
        assertFalse(second.uiState.value.completed)
        assertFalse(second.uiState.value.canComplete)
    }
}
