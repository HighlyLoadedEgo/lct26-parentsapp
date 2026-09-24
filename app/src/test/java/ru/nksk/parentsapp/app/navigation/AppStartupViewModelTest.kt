package ru.nksk.parentsapp.app.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.nksk.parentsapp.feature.pin.data.PinRepository

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test fun firstRunWithoutPinAsksForSetup() = runTest(mainDispatcherRule.dispatcher) {
        val model = AppStartupViewModel(FakePinRepository(hasPin = false))

        advanceUntilIdle()

        assertEquals(AppStartupState.Ready(hasPin = false), model.uiState.value)
    }

    @Test fun laterRunsWithPinAskForUnlock() = runTest(mainDispatcherRule.dispatcher) {
        val model = AppStartupViewModel(FakePinRepository(hasPin = true))

        advanceUntilIdle()

        assertEquals(AppStartupState.Ready(hasPin = true), model.uiState.value)
    }

    @Test fun stateStaysLoadingUntilThePinReadResolves() = runTest(mainDispatcherRule.dispatcher) {
        val model = AppStartupViewModel(FakePinRepository(hasPin = true))

        assertEquals(AppStartupState.Loading, model.uiState.value)

        advanceUntilIdle()

        assertEquals(AppStartupState.Ready(hasPin = true), model.uiState.value)
    }
}

private class FakePinRepository(private val hasPin: Boolean) : PinRepository {
    override suspend fun hasPin(): Boolean = hasPin
    override suspend fun savePin(pin: String) = Unit
    override suspend fun verifyPin(pin: String): Boolean = false
}
