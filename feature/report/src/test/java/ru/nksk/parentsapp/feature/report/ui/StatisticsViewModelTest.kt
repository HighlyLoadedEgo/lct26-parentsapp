package ru.nksk.parentsapp.feature.report.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.nksk.parentsapp.feature.report.R

class StatisticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `no remembered pet shows empty state`() {
        val viewModel = StatisticsViewModel(FakeReportRepository(), FakePetSessionStore())
        viewModel.load(null)
        val state = viewModel.uiState.value
        assertNull(state.report)
        assertNull(state.errorRes)
        assertEquals(false, state.loading)
    }

    @Test
    fun `remembered pet is fetched and kept`() {
        val session = FakePetSessionStore().apply { current = "abc123" }
        val repository = FakeReportRepository()
        val viewModel = StatisticsViewModel(repository, session)
        viewModel.load(null)
        assertEquals("abc123", repository.requestedPetId)
        assertEquals("Рыжик", viewModel.uiState.value.report?.pet?.name)
    }

    @Test
    fun `scanned pet id overrides the remembered one`() {
        val session = FakePetSessionStore().apply { current = "abc123" }
        val repository = FakeReportRepository()
        val viewModel = StatisticsViewModel(repository, session)
        viewModel.load("def456")
        assertEquals("def456", repository.requestedPetId)
        assertEquals("def456", session.current)
        assertEquals(1, session.saves)
    }

    @Test
    fun `session reload never re-persists the remembered pet`() {
        val session = FakePetSessionStore().apply { current = "abc123" }
        val viewModel = StatisticsViewModel(FakeReportRepository(), session)
        viewModel.load(null)
        assertNotNull(viewModel.uiState.value.report)
        assertEquals(0, session.saves)
    }

    @Test
    fun `failed fetch shows error without dropping the saved pet`() {
        val session = FakePetSessionStore().apply { current = "abc123" }
        val viewModel = StatisticsViewModel(FakeReportRepository(fail = true), session)
        viewModel.load(null)
        assertEquals(R.string.report_load_error, viewModel.uiState.value.errorRes)
        assertEquals("abc123", session.current)
    }

    @Test
    fun `change pet clears the session and signals completion`() {
        val session = FakePetSessionStore().apply { current = "abc123" }
        val viewModel = StatisticsViewModel(FakeReportRepository(), session)
        viewModel.load(null)
        viewModel.onAction(StatisticsAction.ChangePet)
        assertNull(session.current)
        assertTrue(viewModel.uiState.value.resetDone)
    }

    @Test
    fun `reset signal can be consumed and re-armed for a second reset`() {
        val session = FakePetSessionStore().apply { current = "abc123" }
        val viewModel = StatisticsViewModel(FakeReportRepository(), session)
        viewModel.load(null)
        viewModel.onAction(StatisticsAction.ChangePet)
        viewModel.consumeReset()
        assertEquals(false, viewModel.uiState.value.resetDone)
        session.current = "def456"
        viewModel.onAction(StatisticsAction.ChangePet)
        assertTrue(viewModel.uiState.value.resetDone)
        assertNull(session.current)
    }

    @Test
    fun `reload after reset shows the empty state despite earlier load`() {
        val session = FakePetSessionStore().apply { current = "abc123" }
        val viewModel = StatisticsViewModel(FakeReportRepository(), session)
        viewModel.load(null)
        assertNotNull(viewModel.uiState.value.report)
        viewModel.onAction(StatisticsAction.ChangePet)
        viewModel.load(null)
        assertNull(viewModel.uiState.value.report)
        assertNull(viewModel.uiState.value.errorRes)
    }
}
