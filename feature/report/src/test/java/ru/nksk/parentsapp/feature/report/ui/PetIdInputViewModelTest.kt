package ru.nksk.parentsapp.feature.report.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import ru.nksk.parentsapp.feature.report.R

class PetIdInputViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `canonical uuid is persisted and opens report`() {
        val session = FakePetSessionStore()
        val viewModel = PetIdInputViewModel(session)
        viewModel.onAction(PetIdInputAction.TextChanged("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
        viewModel.onAction(PetIdInputAction.Submit)
        assertEquals(
            "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            viewModel.uiState.value.openId,
        )
        assertEquals(
            "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            session.current,
        )
        assertNull(viewModel.uiState.value.errorRes)
    }

    @Test
    fun `uuid without dashes opens report`() {
        val viewModel = PetIdInputViewModel(FakePetSessionStore())
        viewModel.onAction(PetIdInputAction.TextChanged("3fa85f6457174562b3fc2c963f66afa6"))
        viewModel.onAction(PetIdInputAction.Submit)
        assertEquals(
            "3fa85f6457174562b3fc2c963f66afa6",
            viewModel.uiState.value.openId,
        )
    }

    @Test
    fun `whitespace is ignored around and inside the id`() {
        val viewModel = PetIdInputViewModel(FakePetSessionStore())
        viewModel.onAction(PetIdInputAction.TextChanged(" 3fa85f64 5717-4562-b3fc-2c963f66afa6 "))
        viewModel.onAction(PetIdInputAction.Submit)
        assertEquals(
            "3fa85f645717-4562-b3fc-2c963f66afa6",
            viewModel.uiState.value.openId,
        )
    }

    @Test
    fun `non-uuid input shows validation error and is not persisted`() {
        val session = FakePetSessionStore()
        val viewModel = PetIdInputViewModel(session)
        viewModel.onAction(PetIdInputAction.TextChanged("https://example.com/qr"))
        viewModel.onAction(PetIdInputAction.Submit)
        assertEquals(R.string.report_invalid_pet_id, viewModel.uiState.value.errorRes)
        assertNull(viewModel.uiState.value.openId)
        assertNull(session.current)
    }
    @Test
    fun `android device id is accepted unchanged`() {
        val session = FakePetSessionStore()
        val model = PetIdInputViewModel(session)
        model.onAction(PetIdInputAction.TextChanged("9f1c2d3e4a5b6078"))
        model.onAction(PetIdInputAction.Submit)
        assertEquals("9f1c2d3e4a5b6078", model.uiState.value.openId)
        assertEquals("9f1c2d3e4a5b6078", session.current)
    }
}

