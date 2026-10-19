package ru.nksk.parentsapp.feature.questions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import ru.nksk.parentsapp.core.report.data.SkillDto
import ru.nksk.parentsapp.core.report.data.SkillStatusDto
import ru.nksk.parentsapp.feature.questions.R

/** Skill meaning, synchronized evidence and published conversation materials. */
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
            .safeDrawingPadding(),
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
            state.loading && state.topic == null -> Column(
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
            state.topic != null -> Column {
                if (state.loading) CircularProgressIndicator(Modifier.padding(12.dp))
                state.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp)) }
                TextButton(onClick = { onAction(QuestionTopicAction.Reload) }, enabled = !state.loading) {
                    Text(stringResource(R.string.questions_refresh))
                }
                TopicBody(state.topic, state.assessmentsStale, state.snapshotStale)
            }
            state.errorRes != null -> ErrorBody(
                text = stringResource(state.errorRes),
                onRetry = { onAction(QuestionTopicAction.Reload) },
            )
        }
    }
}

@Composable
private fun TopicBody(topic: SkillDto, assessmentsStale: Boolean, snapshotStale: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        val statusLabel = stringResource(
            if (!topic.assessmentAvailable) R.string.questions_assessment_unavailable else when (topic.status) {
                SkillStatusDto.MASTERED -> R.string.questions_status_mastered
                SkillStatusDto.PRACTICING -> R.string.questions_status_practicing
                SkillStatusDto.NO_DATA -> R.string.questions_status_no_data
                SkillStatusDto.HAS_PROBLEM -> R.string.questions_status_has_problem
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
        if (assessmentsStale) {
            Text(stringResource(R.string.questions_assessment_stale),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
        }
        if (snapshotStale) {
            Text(stringResource(R.string.questions_snapshot_stale),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
        }
        val explanation = topic.explanation.takeIf { topic.assessmentAvailable }
        val description = explanation?.description ?: topic.learningGoal
        if (description.isNotBlank()) MaterialCard(stringResource(R.string.questions_meaning), description)
        ObservationCountsCard(topic.observationCounts.takeIf { topic.assessmentAvailable })
        val paragraphs = explanation?.let { explanationParagraphs(it.summary, it.evidence.map { item -> item.text }) }
        MaterialCard(stringResource(R.string.questions_observed),
            paragraphs?.joinToString("\n\n") ?: stringResource(R.string.questions_explanation_missing))
        MaterialCard(stringResource(R.string.questions_assessment_reason),
            topic.assessmentReason.takeIf { topic.assessmentAvailable }
                ?: stringResource(R.string.questions_reason_missing))
        if (topic.materialsAvailable) {
            MaterialCard(stringResource(R.string.questions_story), topic.story)
            MaterialCard(stringResource(R.string.questions_own_story), topic.replaceWithParentStory)
            MaterialCard(stringResource(R.string.questions_conversation), topic.conversationStarters
                .mapIndexed { index, question -> "${index + 1}. $question" }.joinToString("\n\n"))
            MaterialCard(stringResource(R.string.questions_takeaway), topic.parentTakeaway)
            MaterialCard(stringResource(R.string.questions_research), topic.researchBasis)
            MaterialCard(stringResource(R.string.questions_sources), researchSourceLinks(topic.researchSources))
        } else {
            MaterialCard(stringResource(R.string.questions_conversation), stringResource(R.string.questions_topic_placeholder))
        }
    }
}

@Composable
private fun ObservationCountsCard(counts: ru.nksk.parentsapp.core.report.data.ObservationCountsDto?) {
    val body = when {
        counts == null -> stringResource(R.string.questions_observations_unavailable)
        counts.observedEpisodes == 0 -> stringResource(R.string.questions_observations_empty)
        else -> buildList {
            add(stringResource(R.string.questions_count_supported, counts.supportedEpisodes))
            add(stringResource(R.string.questions_count_difficulty, counts.difficultyEpisodes))
            add(stringResource(R.string.questions_count_neutral, counts.neutralEpisodes))
            add(stringResource(R.string.questions_count_incomplete, counts.incompleteEpisodes))
            if (counts.pendingEpisodes > 0) add(stringResource(R.string.questions_count_pending, counts.pendingEpisodes))
            add(stringResource(R.string.questions_count_without_hints, counts.supportedWithoutGameHints))
            add(stringResource(R.string.questions_count_assisted, counts.assistedEpisodes))
        }.joinToString("\n\n")
    }
    MaterialCard(stringResource(R.string.questions_observations_title), body)
}

@Composable
private fun MaterialCard(title: String, body: String) {
    MaterialCard(title, AnnotatedString(body))
}

@Composable
private fun MaterialCard(title: String, body: AnnotatedString) {
    SelectionContainer {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun researchSourceLinks(sources: List<String>): AnnotatedString {
    val styles = TextLinkStyles(SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
    ))
    return buildAnnotatedString {
        sources.forEachIndexed { index, source ->
            if (index > 0) append("\n\n")
            withLink(LinkAnnotation.Url(source, styles)) { append(source) }
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

/** Only literal repetition is removed; different recorded events remain distinct. */
internal fun explanationParagraphs(summary: String, evidence: List<String>): List<String> =
    (listOf(summary) + evidence).map(String::trim).filter(String::isNotBlank)
        .distinctBy { it.replace(Regex("\\s+"), " ") }.take(3)
