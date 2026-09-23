package ru.nksk.parentsapp.feature.home.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val sectionsReady: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val state = MutableStateFlow(HomeUiState())
    val uiState = state.asStateFlow()
}
