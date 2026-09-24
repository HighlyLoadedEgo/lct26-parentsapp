package ru.nksk.parentsapp.feature.report.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.nksk.parentsapp.feature.report.R

@Composable
fun PetIdInputScreen(
    state: PetIdInputUiState,
    onAction: (PetIdInputAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.report_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.report_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.report_manual_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.report_manual_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        OutlinedTextField(
            value = state.text,
            onValueChange = { onAction(PetIdInputAction.TextChanged(it)) },
            label = { Text(stringResource(R.string.report_manual_field)) },
            isError = state.errorRes != null,
            supportingText = {
                Text(
                    text = state.errorRes?.let { stringResource(it) } ?: "",
                    color = MaterialTheme.colorScheme.error,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 24.dp),
        )
        Button(
            onClick = { onAction(PetIdInputAction.Submit) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(top = 8.dp)
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(R.string.report_manual_open))
        }
        Spacer(Modifier.weight(1.6f))
    }
}
