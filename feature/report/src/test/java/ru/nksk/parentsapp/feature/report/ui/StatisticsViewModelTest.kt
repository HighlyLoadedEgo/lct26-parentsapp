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
    @Test
    fun `retry after first failed scan retries scanned id and remembers success`() {
        val session = FakePetSessionStore()
        var fails = true
        val repository = object : ru.nksk.parentsapp.core.report.data.ParentReportRepository {
            override suspend fun getReport(petId: String): ru.nksk.parentsapp.core.report.data.ParentReportResponse {
                if (fails) throw java.io.IOException("offline")
                return FakeReportRepository().getReport(petId)
            }
        }
        val model = StatisticsViewModel(repository, session)
        model.load("9f1c2d3e4a5b6078")
        assertNull(session.current)
        fails = false
        model.onAction(StatisticsAction.Reload)
        assertEquals("9f1c2d3e4a5b6078", model.uiState.value.report?.pet?.id)
        assertEquals("9f1c2d3e4a5b6078", session.current)
    }

    @Test
    fun `late scanned response cannot undo a reset`() = kotlinx.coroutines.test.runTest {
        val response = kotlinx.coroutines.CompletableDeferred<Unit>()
        val session = FakePetSessionStore()
        val repository = object : ru.nksk.parentsapp.core.report.data.ParentReportRepository {
            override suspend fun getReport(petId: String): ru.nksk.parentsapp.core.report.data.ParentReportResponse {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { response.await() }
                return FakeReportRepository().getReport(petId)
            }
        }
        val model = StatisticsViewModel(repository, session)
        model.load("9f1c2d3e4a5b6078")
        model.onAction(StatisticsAction.ChangePet)
        response.complete(Unit)
        assertNull(session.current)
        assertNull(model.uiState.value.report)
    }

    @Test
    fun `refresh failure retains last report and exposes error`() {
        var fails = false
        val session = FakePetSessionStore().apply { current = "9f1c2d3e4a5b6078" }
        val repository = object : ru.nksk.parentsapp.core.report.data.ParentReportRepository {
            override suspend fun getReport(petId: String): ru.nksk.parentsapp.core.report.data.ParentReportResponse {
                if (fails) throw java.io.IOException("offline")
                return FakeReportRepository().getReport(petId)
            }
        }
        val model = StatisticsViewModel(repository, session)
        model.load(null)
        fails = true
        model.onAction(StatisticsAction.Reload)
        assertEquals("9f1c2d3e4a5b6078", model.uiState.value.report?.pet?.id)
        assertEquals(R.string.report_load_error, model.uiState.value.errorRes)
    }
    @Test
    fun `returning to report observes cleared or changed session`() {
        val session = FakePetSessionStore().apply { current = "first" }
        val model = StatisticsViewModel(FakeReportRepository(), session)
        model.load(null)
        session.current = "second"
        model.load(null)
        assertEquals("second", model.uiState.value.report?.pet?.id)
        session.current = null
        model.load(null)
        assertNull(model.uiState.value.report)
    }
    @Test
    fun `background cancellation of refresh does not cancel a confirmed reset`() = kotlinx.coroutines.test.runTest {
        val completion = kotlinx.coroutines.CompletableDeferred<Unit>()
        val backing = FakePetSessionStore().apply { current = "device" }
        val session = object : ru.nksk.parentsapp.core.report.data.PetSessionStore by backing {
            override suspend fun clearPetId() {
                completion.await()
                backing.clearPetId()
            }
        }
        val model = StatisticsViewModel(FakeReportRepository(), session)
        model.load(null)
        model.onAction(StatisticsAction.ChangePet)
        assertTrue(model.uiState.value.resetting)
        model.cancelRefresh()
        model.onAction(StatisticsAction.Reload)
        completion.complete(Unit)
        assertNull(backing.current)
        assertNull(model.uiState.value.report)
        assertTrue(model.uiState.value.resetDone)
    }
}
