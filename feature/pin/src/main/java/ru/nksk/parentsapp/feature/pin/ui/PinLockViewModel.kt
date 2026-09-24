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

data class PinLockUiState(
    val digits: String = "",
    val verifying: Boolean = false,
    val unlocked: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

sealed interface PinLockAction {
    data class Digit(val value: Char) : PinLockAction
    data object Delete : PinLockAction
}

@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val pinRepository: PinRepository,
) : ViewModel() {
    private val state = MutableStateFlow(PinLockUiState())
    val uiState = state.asStateFlow()

    fun onAction(action: PinLockAction) {
        when (action) {
            is PinLockAction.Digit -> appendDigit(action.value)
            PinLockAction.Delete -> state.update {
                if (it.verifying || it.unlocked) it else it.copy(digits = it.digits.dropLast(1))
            }
        }
    }

    private fun appendDigit(digit: Char) {
        val current = state.value
        if (current.verifying || current.unlocked || current.digits.length >= PIN_LENGTH) return
        val digits = current.digits + digit
        state.update { it.copy(digits = digits, errorRes = null) }
        if (digits.length < PIN_LENGTH) return
        state.update { it.copy(verifying = true) }
        viewModelScope.launch {
            val unlocked = runCatching { pinRepository.verifyPin(digits) }.getOrDefault(false)
            state.update {
                if (unlocked) {
                    it.copy(verifying = false, unlocked = true)
                } else {
                    it.copy(
                        verifying = false,
                        digits = "",
                        errorRes = R.string.pin_lock_wrong,
                    )
                }
            }
        }
    }

    private companion object {
        const val PIN_LENGTH = 4
    }
}
