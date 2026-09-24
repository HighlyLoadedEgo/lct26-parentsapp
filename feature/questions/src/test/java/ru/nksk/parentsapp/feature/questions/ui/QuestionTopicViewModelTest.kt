package ru.nksk.parentsapp.feature.questions.ui

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import ru.nksk.parentsapp.feature.questions.R

class QuestionTopicViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun topicIsResolvedByItsStableId() = runTest {
        val repository = FakeReportRepository()
        val session = FakePetSessionStore().apply { current = "abc123" }
        val model = QuestionTopicViewModel(repository, session)

        model.load("FIN-03")
        advanceUntilIdle()

        assertEquals("FIN-03", model.uiState.value.topic?.id)
        assertNull(model.uiState.value.errorRes)
    }

    @Test fun unknownTopicIdShowsAnErrorInsteadOfABlankScreen() = runTest {
        val repository = FakeReportRepository()
        val session = FakePetSessionStore().apply { current = "abc123" }
        val model = QuestionTopicViewModel(repository, session)

        model.load("NOPE")
        advanceUntilIdle()

        assertNull(model.uiState.value.topic)
        assertEquals(R.string.questions_load_error, model.uiState.value.errorRes)
    }

    @Test fun noRememberedPetShowsAnError() = runTest {
        val repository = FakeReportRepository()
        val model = QuestionTopicViewModel(repository, FakePetSessionStore())

        model.load("FIN-03")
        advanceUntilIdle()

        assertNull(model.uiState.value.topic)
        assertEquals(R.string.questions_load_error, model.uiState.value.errorRes)
        assertEquals(0, repository.requests)
    }

    @Test fun retryRecoversAfterAFailedFetch() = runTest {
        val repository = FakeReportRepository(fail = true)
        val session = FakePetSessionStore().apply { current = "abc123" }
        val model = QuestionTopicViewModel(repository, session)
        model.load("FIN-04")
        advanceUntilIdle()
        assertNotNull(model.uiState.value.errorRes)

        repository.setFail(false)
        model.onAction(QuestionTopicAction.Reload)
        advanceUntilIdle()

        assertEquals("FIN-04", model.uiState.value.topic?.id)
        assertNull(model.uiState.value.errorRes)
    }
}
