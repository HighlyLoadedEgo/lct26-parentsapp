package ru.nksk.parentsapp.feature.pin.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nksk.parentsapp.feature.pin.R
import ru.nksk.parentsapp.feature.pin.data.PinRepository

enum class PinSetupStage { Enter, Confirm }

data class PinSetupUiState(
    val stage: PinSetupStage = PinSetupStage.Enter,
    val firstPin: String? = null,
    val digits: String = "",
    @StringRes val errorRes: Int? = null,
    val saving: Boolean = false,
    val completed: Boolean = false,
)

sealed interface PinSetupAction {
    data class Digit(val value: Char) : PinSetupAction
    data object Delete : PinSetupAction
}

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    private val pinRepository: PinRepository,
) : ViewModel() {
    private val state = MutableStateFlow(PinSetupUiState())
    val uiState = state.asStateFlow()

    fun onAction(action: PinSetupAction) {
        when (action) {
            is PinSetupAction.Digit -> appendDigit(action.value)
            PinSetupAction.Delete -> state.update {
                if (it.saving || it.completed) it else it.copy(digits = it.digits.dropLast(1))
            }
        }
    }

    private fun appendDigit(digit: Char) {
        val current = state.value
        if (current.saving || current.completed || current.digits.length >= PIN_LENGTH) return
        val digits = current.digits + digit
        state.update { it.copy(digits = digits, errorRes = null) }
        if (digits.length < PIN_LENGTH) return
        when (current.stage) {
            PinSetupStage.Enter -> state.update {
                it.copy(stage = PinSetupStage.Confirm, firstPin = digits, digits = "")
            }
            PinSetupStage.Confirm -> confirm(current.firstPin, digits)
        }
    }

    private fun confirm(firstPin: String?, candidate: String) {
        if (firstPin == null) return
        if (candidate != firstPin) {
            state.update {
                it.copy(
                    stage = PinSetupStage.Enter,
                    firstPin = null,
                    digits = "",
                    errorRes = R.string.pin_setup_mismatch,
                )
            }
            return
        }
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching { pinRepository.savePin(candidate) }
                .onSuccess {
                    state.update { current -> current.copy(saving = false, completed = true) }
                }
                .onFailure {
                    state.update { current ->
                        current.copy(
                            saving = false,
                            errorRes = R.string.pin_save_error,
                            stage = PinSetupStage.Enter,
                            firstPin = null,
                            digits = "",
                        )
                    }
                }
        }
    }

    private companion object {
        const val PIN_LENGTH = 4
    }
}
