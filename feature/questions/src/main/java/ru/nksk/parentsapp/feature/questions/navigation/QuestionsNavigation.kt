package ru.nksk.parentsapp.feature.questions.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.nksk.parentsapp.feature.questions.ui.QuestionTopicScreen
import ru.nksk.parentsapp.feature.questions.ui.QuestionTopicViewModel

/** Practice screen for one unmastered topic of the pet report; carries only the stable skill id. */
@Serializable
@SerialName("question_topic")
data class QuestionTopic(val skillId: String) : NavKey

fun EntryProviderScope<NavKey>.questionTopicEntry(
    onBack: (QuestionTopic) -> Unit,
) {
    entry<QuestionTopic> { source ->
        val viewModel: QuestionTopicViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(source, lifecycle) {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                viewModel.load(source.skillId)
                try { kotlinx.coroutines.awaitCancellation() } finally { viewModel.cancelRefresh() }
            }
        }
        QuestionTopicScreen(
            state = state,
            onAction = viewModel::onAction,
            onBack = dropUnlessResumed { onBack(source) },
        )
    }
}
