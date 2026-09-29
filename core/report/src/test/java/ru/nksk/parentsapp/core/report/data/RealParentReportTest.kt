package ru.nksk.parentsapp.core.report.data

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RealParentReportTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private var runId = "run-1"
    private var snapshotStatus = 200
    private var skillsStatus = 200
    private var materialsStatus = 200
    private var sequence = 7L
    private var payloadKind: String? = "CURRENT_WORLD"
    private val requests = mutableListOf<String>()
    private var world = """{"worldFormatVersion":1,"runId":"run-1","historySequence":7,"state":{"pet":{"name":"Лисёнок","age":"TEEN","color":"SAND","temperament":"Confident","selectedLookId":"BACKPACK","visualState":"HAPPY"},"economy":{"availableBalance":3000000000,"savingsBalance":25}}}"""
    private val repository: ParentReportRepository

    init {
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            requests += path
            val body = exchange.requestBody.bufferedReader().readText()
            val (code, response) = when (path) {
                "/v1/profiles/snapshot/download" -> {
                    check(body.contains("device-1"))
                    snapshotStatus to """{"gameRunId":"run-1","serverRevision":2,"currentContentFingerprint":"content","snapshotJson":${json.encodeToString(world)},"schemaVersion":1,"payloadKind":${payloadKind?.let { "\"$it\"" } ?: "null"}}"""
                }
                "/v1/profiles/skills/query" -> {
                    check(body.contains("device-1") && body.contains("run-1"))
                    skillsStatus to """{"gameRunId":"$runId","basedOnHistorySequence":$sequence,"schemaVersion":1,"skills":[${(1..12).joinToString { """{"skillId":"FIN-${it.toString().padStart(2, '0')}","status":"HAS_PROBLEM","policyVersion":"real-v1"}""" }}]}"""
                }
                "/v1/parent-materials" -> materialsStatus to """{"schemaVersion":1,"publicationStatus":"PUBLISHED","contentVersion":"v1","skills":[{"skillId":"FIN-01","learningGoal":"Цель","story":"История","replaceWithParentStory":"Пример","conversationStarters":["Вопрос"],"parentTakeaway":"Вывод","researchBasis":"Основание","researchSources":["https://example.org"]}]}"""
                else -> 200 to """{"pet":{"id":"device-1","name":"Демо","temper":"Демо","balance":10,"selectedLookId":"PLAIN","visualState":"NORMAL"},"skills":[],"isDemo":true}"""
            }
            val bytes = response.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(code, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val api = Retrofit.Builder().baseUrl("http://127.0.0.1:${server.address.port}/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(ParentReportApi::class.java)
        repository = ParentReportRepositoryImpl(api)
    }

    @After fun close() = server.stop(0)

    @Test fun `report uses saved pet money and real server assessments`() = runTest {
        val report = repository.getReport("device-1")
        assertEquals("Лисёнок", report.pet.name)
        assertEquals(3000000025L, report.pet.balance)
        assertEquals("BACKPACK", report.pet.selectedLookId)
        assertEquals("HAPPY", report.pet.visualState)
        assertFalse(report.isDemo)
        assertEquals(12, report.skills.size)
        assertEquals(SkillStatusDto.HAS_PROBLEM, report.skills.first().status)
        assertEquals("История", report.skills.first().story)
        assertTrue(report.skills.first().materialsAvailable)
        assertFalse(requests.any { it.startsWith("/api/parents/") })
    }

    @Test fun `missing cloud save never falls back to a demo pet`() = runTest {
        snapshotStatus = 404
        assertTrue(runCatching { repository.getReport("device-1") }.isFailure)
    }
    @Test fun `assessment failure keeps real pet without inventing scores`() = runTest {
        skillsStatus = 503
        val report = repository.getReport("device-1")
        assertEquals("Лисёнок", report.pet.name)
        assertFalse(report.assessmentsAvailable)
        assertTrue(report.skills.all { !it.assessmentAvailable })
    }

    @Test fun `assessments from another run are not shown`() = runTest {
        runId = "another-run"
        assertFalse(repository.getReport("device-1").assessmentsAvailable)
    }

    @Test fun `assessments ahead of snapshot are not combined with old world`() = runTest {
        sequence = 8
        assertFalse(repository.getReport("device-1").assessmentsAvailable)
    }

    @Test fun `older assessments are marked stale`() = runTest {
        sequence = 6
        assertTrue(repository.getReport("device-1").assessmentsStale)
    }

    @Test fun `material failure does not hide real pet or assessments`() = runTest {
        materialsStatus = 503
        val report = repository.getReport("device-1")
        assertEquals("Лисёнок", report.pet.name)
        assertTrue(report.assessmentsAvailable)
        assertTrue(report.materialsLoadFailed)
        assertTrue(report.skills.none { it.materialsAvailable })
    }

    @Test fun `legacy format five saves also provide real data`() = runTest {
        payloadKind = null
        world = world.replace("\"worldFormatVersion\":1", "\"formatVersion\":5")
        assertEquals("Лисёнок", repository.getReport("device-1").pet.name)
    }

}
