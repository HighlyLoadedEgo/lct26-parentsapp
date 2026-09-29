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
}
