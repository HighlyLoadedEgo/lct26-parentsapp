package ru.nksk.parentsapp.core.report.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ParentReportContractTest {
    @Test fun `backend problem status and int64 balance decode`() {
        val report = Json { ignoreUnknownKeys = true }.decodeFromString<ParentReportResponse>(
            """{"pet":{"id":"9f1c2d3e4a5b6078","name":"Рыжик","temper":"playful","balance":3000000000,"selectedLookId":"BACKPACK","visualState":"NORMAL"},"skills":[{"id":"FIN-01","title":"Сравнение","status":"HAS_PROBLEM","isMastered":false}],"isDemo":true}"""
        )
        assertEquals(3000000000L, report.pet.balance)
        assertEquals("HAS_PROBLEM", report.skills.single().status.name)
    }
    @Test fun `published server material is retained in full`() {
        val skill = Json { ignoreUnknownKeys = true }.decodeFromString<SkillDto>(
            """{"id":"FIN-01","title":"Сравнение","status":"MASTERED","materialsAvailable":true,"learningGoal":"goal","story":"story","replaceWithParentStory":"own story","conversationStarters":["one","two"],"parentTakeaway":"takeaway","researchBasis":"basis","researchSources":["https://example.org/source"]}"""
        )
        assertEquals(true, skill.materialsAvailable)
        assertEquals("goal", skill.learningGoal)
        assertEquals("story", skill.story)
        assertEquals("own story", skill.replaceWithParentStory)
        assertEquals(listOf("one", "two"), skill.conversationStarters)
        assertEquals("takeaway", skill.parentTakeaway)
        assertEquals("basis", skill.researchBasis)
        assertEquals(listOf("https://example.org/source"), skill.researchSources)
    }
    @Test fun `legacy response without materials stays unpublished`() {
        val skill = Json.decodeFromString<SkillDto>(
            """{"id":"FIN-01","title":"Сравнение","status":"NO_DATA"}"""
        )
        assertEquals(false, skill.materialsAvailable)
        assertEquals(emptyList<String>(), skill.conversationStarters)
    }

    private fun assessment(block: String = ""): ReportAssessmentDto =
        Json { ignoreUnknownKeys = true }.decodeFromString(
            """{"skillId":"FIN-01","status":"MASTERED","policyVersion":"skills-mvp-v1"$block}"""
        )

    private val explanationJson = """{"version":1,"description":"Сравнивает суммы","summary":"Верно сравнил суммы.","evidence":[{"episodeId":"one","sourceEventIds":["fact-1"],"text":"День 2: выбрал большую сумму."}]}"""

    @Test fun `optional explanation decodes without rewriting`() {
        val skill = assessment(",\"explanation\":$explanationJson")
        assertEquals("Сравнивает суммы", skill.explanation?.description)
        assertEquals("День 2: выбрал большую сумму.", skill.explanation?.evidence?.single()?.text)
        assertEquals(listOf("fact-1"), skill.explanation?.evidence?.single()?.sourceEventIds)
    }

    @Test fun `unsupported explanation never hides valid status`() {
        val invalid = listOf(
            "null", "[]", "42", "{}",
            explanationJson.replace("\"version\":1", "\"version\":2"),
            explanationJson.replace("\"version\":1", "\"version\":\"1\""),
            explanationJson.replace("Сравнивает суммы", " "),
            explanationJson.replace("День 2: выбрал большую сумму.", "x".repeat(401)),
            explanationJson.replace("\"episodeId\":\"one\"", "\"episodeId\":\" \""),
            explanationJson.replace("Сравнивает суммы", "x".repeat(401)),
            explanationJson.replace("Верно сравнил суммы.", "x".repeat(601)),
            explanationJson.replace("fact-1\"]", "fact-1\",\"fact-1\"]"),
            explanationJson.replace("fact-1", "\\u0000"),
            explanationJson.replace("\"sourceEventIds\":[\"fact-1\"]", "\"sourceEventIds\":[]"),
        )
        invalid.forEach { block ->
            val skill = assessment(",\"explanation\":$block")
            assertEquals(SkillStatusDto.MASTERED, skill.status)
            org.junit.Assert.assertNull("Block: $block", skill.explanation)
        }
        val block = Json.parseToJsonElement(explanationJson) as kotlinx.serialization.json.JsonObject
        val example = (block.getValue("evidence") as kotlinx.serialization.json.JsonArray).single()
        listOf(2, 3).forEach { count ->
            val repeated = kotlinx.serialization.json.JsonObject(block +
                ("evidence" to kotlinx.serialization.json.JsonArray(List(count) { example })))
            org.junit.Assert.assertNull(assessment(",\"explanation\":$repeated").explanation)
        }
        org.junit.Assert.assertNull(assessment().explanation)
    }

    @Test fun `reason is optional and malformed reason preserves status`() {
        assertEquals("Два успеха", assessment(",\"assessmentReason\":\"Два успеха\"").assessmentReason)
        listOf("null", "42", "[]", "{}", "\" \"", "\"" + "x".repeat(601) + "\"")
            .forEach { value ->
                val result = assessment(",\"assessmentReason\":$value")
                assertEquals(SkillStatusDto.MASTERED, result.status)
                org.junit.Assert.assertNull(result.assessmentReason)
            }
    }
    @Test fun `observation counts optional invalid blocks preserve assessment`() {
        val valid = """{"observedEpisodes":6,"completedEpisodes":6,"supportedEpisodes":5,"difficultyEpisodes":1,"neutralEpisodes":0,"pendingEpisodes":0,"incompleteEpisodes":0,"supportedWithoutGameHints":5,"assistedEpisodes":1}"""
        assertEquals(5, assessment(",\"observationCounts\":$valid").observationCounts?.supportedEpisodes)
        val invalid = listOf("null", "[]", "{}", "42", valid.replace(":6", ":-1"),
            valid.replace(":6", ":\"6\""), valid.replace(":6", ":6.5"),
            valid.replace(",\"assistedEpisodes\":1", ""))
        invalid.forEach { value ->
            val result = assessment(",\"observationCounts\":$value")
            assertEquals(SkillStatusDto.MASTERED, result.status)
            org.junit.Assert.assertNull(result.observationCounts)
        }
        org.junit.Assert.assertNull(assessment().observationCounts)
    }

}
