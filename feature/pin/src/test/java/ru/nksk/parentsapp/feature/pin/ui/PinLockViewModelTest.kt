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

private class PinLockFakeRepository(private val stored: String?) : PinRepository {
    override suspend fun hasPin(): Boolean = stored != null
    override suspend fun savePin(pin: String) = Unit
    override suspend fun verifyPin(pin: String): Boolean = stored == pin
}

private fun PinLockViewModel.type(pin: String) {
    pin.forEach { onAction(PinLockAction.Digit(it)) }
}

class PinLockViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `correct pin unlocks`() {
        val viewModel = PinLockViewModel(PinLockFakeRepository("1234"))
        viewModel.type("1234")
        assertTrue(viewModel.uiState.value.unlocked)
        assertNull(viewModel.uiState.value.errorRes)
    }

    @Test
    fun `wrong pin shows error and clears input`() {
        val viewModel = PinLockViewModel(PinLockFakeRepository("1234"))
        viewModel.type("9999")
        val state = viewModel.uiState.value
        assertFalse(state.unlocked)
        assertEquals(R.string.pin_lock_wrong, state.errorRes)
        assertEquals("", state.digits)
    }

    @Test
    fun `missing pin never unlocks`() {
        val viewModel = PinLockViewModel(PinLockFakeRepository(null))
        viewModel.type("1234")
        assertFalse(viewModel.uiState.value.unlocked)
        assertNotNull(viewModel.uiState.value.errorRes)
    }
}
