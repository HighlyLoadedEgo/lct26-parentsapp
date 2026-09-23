package ru.nksk.parentsapp.feature.home.ui

import org.junit.Assert.assertFalse
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun `initial state is a pending stub`() {
        val viewModel = HomeViewModel()
        assertFalse(viewModel.uiState.value.sectionsReady)
    }
}
