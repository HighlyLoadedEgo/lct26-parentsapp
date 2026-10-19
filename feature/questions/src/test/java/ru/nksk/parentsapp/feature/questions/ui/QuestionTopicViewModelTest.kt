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

    @Test fun staleAssessmentAndExplanationSurviveFailedRefreshWithoutMaterials() = runTest {
        val skill = ru.nksk.parentsapp.core.report.data.SkillDto(
            "FIN-01", "Сравнивает суммы", ru.nksk.parentsapp.core.report.data.SkillStatusDto.MASTERED,
            explanation = ru.nksk.parentsapp.core.report.data.SkillExplanationDto(
                description = "Сравнивает суммы", summary = "Два успеха", evidence = emptyList()),
            assessmentReason = "Два завершённых успеха",
        )
        var fail = false
        val repository = object : ru.nksk.parentsapp.core.report.data.ParentReportRepository {
            override suspend fun getReport(petId: String): ru.nksk.parentsapp.core.report.data.ParentReportResponse {
                if (fail) throw java.io.IOException("offline")
                return ru.nksk.parentsapp.core.report.data.ParentReportResponse(
                    ru.nksk.parentsapp.core.report.data.PetDto(petId, "Лис", "", 0, "", ""),
                    listOf(skill), false, assessmentsStale = true,
                )
            }
        }
        val model = QuestionTopicViewModel(repository, FakePetSessionStore().apply { current = "abc123" })
        model.load("FIN-01")
        advanceUntilIdle()
        assertEquals(skill, model.uiState.value.topic)
        assertEquals(true, model.uiState.value.assessmentsStale)
        fail = true
        model.onAction(QuestionTopicAction.Reload)
        advanceUntilIdle()
        assertEquals(skill, model.uiState.value.topic)
        assertEquals(true, model.uiState.value.assessmentsStale)
        assertNotNull(model.uiState.value.errorRes)
    }

    @Test fun newerAssessmentSnapshotNoticeSurvivesFailedRefresh() = runTest {
        var fail = false
        val repository = object : ru.nksk.parentsapp.core.report.data.ParentReportRepository {
            override suspend fun getReport(petId: String): ru.nksk.parentsapp.core.report.data.ParentReportResponse {
                if (fail) throw java.io.IOException("offline")
                return ru.nksk.parentsapp.core.report.data.ParentReportResponse(
                    ru.nksk.parentsapp.core.report.data.PetDto(petId, "Лис", "", 0, "", ""),
                    listOf(ru.nksk.parentsapp.core.report.data.SkillDto("FIN-01", "Суммы",
                        ru.nksk.parentsapp.core.report.data.SkillStatusDto.PRACTICING)), false, snapshotStale = true)
            }
        }
        val model = QuestionTopicViewModel(repository, FakePetSessionStore().apply { current = "abc123" })
        model.load("FIN-01")
        advanceUntilIdle()
        assertEquals(true, model.uiState.value.snapshotStale)
        assertEquals(false, model.uiState.value.assessmentsStale)
        fail = true
        model.onAction(QuestionTopicAction.Reload)
        advanceUntilIdle()
        assertEquals(true, model.uiState.value.snapshotStale)
        assertNotNull(model.uiState.value.topic)
        assertNotNull(model.uiState.value.errorRes)
    }

    @Test fun paragraphsRemoveLiteralDuplicatesButKeepDifferentEvents() {
        assertEquals(listOf("Верно сравнил суммы.", "День 2: верно выбрал", "День 3: верно выбрал"),
            explanationParagraphs("Верно сравнил суммы.", listOf(
                "Верно  сравнил суммы.", "День 2: верно выбрал", "День 3: верно выбрал", " ")))
    }
}
