package ru.nksk.parentsapp.feature.report.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.nksk.parentsapp.feature.report.R
import ru.nksk.parentsapp.core.report.data.PetSessionStore

data class PetIdInputUiState(
    val text: String = "",
    @StringRes val errorRes: Int? = null,
    /** Set only after the valid id is persisted; the entry then navigates to Statistics. */
    val openId: String? = null,
)

sealed interface PetIdInputAction {
    data class TextChanged(val value: String) : PetIdInputAction
    data object Submit : PetIdInputAction
}

@HiltViewModel
class PetIdInputViewModel @Inject constructor(
    private val sessionStore: PetSessionStore,
) : ViewModel() {
    private val state = MutableStateFlow(PetIdInputUiState())
    val uiState = state.asStateFlow()

    fun onAction(action: PetIdInputAction) {
        when (action) {
            is PetIdInputAction.TextChanged -> state.update {
                it.copy(text = action.value.filter { ch -> !ch.isWhitespace() }, errorRes = null)
            }
            PetIdInputAction.Submit -> submit()
        }
    }

    private fun submit() {
        if (state.value.openId != null) return
        val petId = state.value.text.trim()
        if (!PET_ID_REGEX.matches(petId)) {
            state.update { it.copy(errorRes = R.string.report_invalid_pet_id) }
            return
        }
        viewModelScope.launch {
            // Persist first: Statistics reads the session when it becomes visible again.
            sessionStore.savePetId(petId)
            state.update { it.copy(openId = petId) }
        }
    }

    private companion object {
        // Canonical UUID with or without dashes, as the backend paths accept both.
        val PET_ID_REGEX = Regex(
            pattern = "^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$",
        )
    }
}
