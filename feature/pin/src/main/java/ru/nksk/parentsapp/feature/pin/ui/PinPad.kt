package ru.nksk.parentsapp.feature.pin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.nksk.parentsapp.feature.pin.R

/** 3x4 numeric pad used by both PIN screens; disabled while a verification is in flight. */
@Composable
fun PinPad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9'),
            listOf(null, '0', DELETE),
        ).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { key ->
                    when (key) {
                        null -> Spacer(Modifier.size(72.dp))
                        DELETE -> IconButton(
                            onClick = onDelete,
                            enabled = enabled,
                            modifier = Modifier.size(72.dp),
                        ) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.pin_delete),
                            )
                        }
                        else -> OutlinedButton(
                            onClick = { onDigit(key) },
                            enabled = enabled,
                            modifier = Modifier.size(72.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(
                                text = key.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val DELETE: Char = '\u0000'
