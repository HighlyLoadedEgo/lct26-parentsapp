package ru.nksk.parentsapp.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.nksk.parentsapp.feature.pin.data.PinRepository

/** Startup gate: first run asks for a PIN, later runs ask for it before the tab shell. */
sealed interface AppStartupState {
    data object Loading : AppStartupState
    data class Ready(val hasPin: Boolean) : AppStartupState
}

@HiltViewModel
class AppStartupViewModel @Inject constructor(
    pinRepository: PinRepository,
) : ViewModel() {
    private val state = MutableStateFlow<AppStartupState>(AppStartupState.Loading)
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            val hasPin = pinRepository.hasPin()
            state.value = AppStartupState.Ready(hasPin)
        }
    }
}
