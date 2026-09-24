package ru.nksk.parentsapp.feature.pin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.nksk.parentsapp.feature.pin.R

@Composable
fun PinLockScreen(
    state: PinLockUiState,
    onAction: (PinLockAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    PinScreenColumn(modifier) {
        PinHeader(
            title = stringResource(R.string.pin_lock_title),
            hint = stringResource(R.string.pin_lock_hint),
        )
        PinDots(entered = state.digits.length, modifier = Modifier.padding(top = 32.dp))
        PinError(errorRes = state.errorRes)
        PinPad(
            enabled = !state.verifying,
            onDigit = { onAction(PinLockAction.Digit(it)) },
            onDelete = { onAction(PinLockAction.Delete) },
            modifier = Modifier.padding(top = 24.dp),
        )
        if (state.verifying) {
            PinSpinner(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
