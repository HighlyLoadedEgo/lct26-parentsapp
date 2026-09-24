package ru.nksk.parentsapp.feature.pin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.nksk.parentsapp.feature.pin.R

@Composable
fun PinSetupScreen(
    state: PinSetupUiState,
    onAction: (PinSetupAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    PinScreenColumn(modifier) {
        PinHeader(
            title = stringResource(R.string.pin_setup_title),
            hint = stringResource(
                if (state.stage == PinSetupStage.Enter) {
                    R.string.pin_setup_hint_enter
                } else {
                    R.string.pin_setup_hint_confirm
                }
            ),
        )
        PinDots(entered = state.digits.length, modifier = Modifier.padding(top = 32.dp))
        PinError(errorRes = state.errorRes)
        PinPad(
            enabled = !state.saving,
            onDigit = { onAction(PinSetupAction.Digit(it)) },
            onDelete = { onAction(PinSetupAction.Delete) },
            modifier = Modifier.padding(top = 24.dp),
        )
        if (state.saving) {
            PinSpinner(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
