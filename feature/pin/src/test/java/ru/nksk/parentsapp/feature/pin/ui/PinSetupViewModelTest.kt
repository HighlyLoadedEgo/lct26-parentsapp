package ru.nksk.parentsapp.feature.pin.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.nksk.parentsapp.feature.pin.R
import ru.nksk.parentsapp.feature.pin.data.PinRepository

private class PinSetupFakeRepository : PinRepository {
    var stored: String? = null
    var failSave = false

    override suspend fun hasPin(): Boolean = stored != null
    override suspend fun savePin(pin: String) {
        check(!failSave) { "save failed" }
        stored = pin
    }
    override suspend fun verifyPin(pin: String): Boolean = stored == pin
}

private fun PinSetupViewModel.type(pin: String) {
    pin.forEach { onAction(PinSetupAction.Digit(it)) }
}

class PinSetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `first entry moves to confirm stage`() {
        val viewModel = PinSetupViewModel(PinSetupFakeRepository())
        viewModel.type("12")
        assertEquals(PinSetupStage.Enter, viewModel.uiState.value.stage)
        viewModel.type("34")
        val state = viewModel.uiState.value
        assertEquals(PinSetupStage.Confirm, state.stage)
        assertEquals("1234", state.firstPin)
        assertEquals("", state.digits)
    }

    @Test
    fun `matching confirmation saves pin and completes`() {
        val repository = PinSetupFakeRepository()
        val viewModel = PinSetupViewModel(repository)
        viewModel.type("1234")
        viewModel.type("1234")
        val state = viewModel.uiState.value
        assertTrue(state.completed)
        assertFalse(state.saving)
        assertEquals("1234", repository.stored)
    }

    @Test
    fun `mismatched confirmation restarts with error`() {
        val viewModel = PinSetupViewModel(PinSetupFakeRepository())
        viewModel.type("1234")
        viewModel.type("5678")
        val state = viewModel.uiState.value
        assertEquals(PinSetupStage.Enter, state.stage)
        assertNull(state.firstPin)
        assertNotNull(state.errorRes)
        assertEquals(R.string.pin_setup_mismatch, state.errorRes)
    }

    @Test
    fun `save failure keeps setup available with error`() {
        val repository = PinSetupFakeRepository().apply { failSave = true }
        val viewModel = PinSetupViewModel(repository)
        viewModel.type("1234")
        viewModel.type("1234")
        val state = viewModel.uiState.value
        assertFalse(state.completed)
        assertEquals(R.string.pin_save_error, state.errorRes)
        assertNull(repository.stored)
    }
}
