package ru.nksk.parentsapp.feature.questions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.nksk.parentsapp.core.report.data.SkillDto
import ru.nksk.parentsapp.core.report.data.SkillStatusDto
import ru.nksk.parentsapp.feature.questions.R

/** Practice screen for one unmastered topic; content awaits a backend endpoint. */
@Composable
fun QuestionTopicScreen(
    state: QuestionTopicUiState,
    onAction: (QuestionTopicAction) -> Unit,
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
                    contentDescription = stringResource(R.string.questions_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = state.topic?.title ?: stringResource(R.string.questions_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        when {
            state.loading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            state.topic != null -> TopicBody(state.topic)
            state.errorRes != null -> ErrorBody(
                text = stringResource(state.errorRes),
                onRetry = { onAction(QuestionTopicAction.Reload) },
            )
        }
    }
}

@Composable
private fun TopicBody(topic: SkillDto) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        val statusLabel = stringResource(
            when (topic.status) {
                SkillStatusDto.MASTERED -> R.string.questions_status_no_data
                SkillStatusDto.PRACTICING -> R.string.questions_status_practicing
                SkillStatusDto.NO_DATA -> R.string.questions_status_no_data
            }
        )
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.questions_topic_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ErrorBody(text: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(R.string.questions_retry))
        }
    }
}
