package ru.nksk.parentsapp.core.report.rewards

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException
import retrofit2.Response
import ru.nksk.parentsapp.core.report.data.PetSessionStore

class RemoteParentQuestRewardsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val json = Json { ignoreUnknownKeys = true }
    private val cap = "cosmetic-cap-moscow-blue-v1"
    private val other = "cosmetic-cap-lct2026-blue-v1"

    @Test fun `owned snapshot items and undelivered journal gifts block a new issuance`() = runTest {
        val api = FakeApi().apply {
            snapshot = snapshot("run", """{"id":"owned","itemId":"$cap"}""")
            grants = listOf(grant(other))
        }
        val repo = repository(api, backgroundScope)
        assertEquals(setOf(cap, other), repo.inventory().filterNotNull().first().ownedItemIds)
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        assertFails { repo.issue("SHOPPING", other, "device", "run") }
        assertTrue(api.sent.isEmpty())
    }

    @Test fun `uncertain response survives process reopen and retries same key and body despite owned journal`() = runTest {
        val file = File(folder.root, "frozen.preferences_pb")
        val api = FakeApi().apply { failure = IOException("lost response") }
        suspend fun withRepository(block: suspend (RemoteParentQuestRewardsRepository) -> Unit) {
            val scope = CoroutineScope(coroutineContext + SupervisorJob())
            try { block(repository(api, scope, file)) }
            finally { scope.coroutineContext[Job]!!.cancel(); scope.coroutineContext[Job]!!.join() }
        }
        withRepository { repo ->
            assertFails { repo.issue("SHOPPING", cap, "device", "run") }
            assertEquals(cap, repo.pending("SHOPPING")?.itemId)
        }
        api.failure = null
        api.grants = listOf(grant(cap))
        withRepository { repo ->
            repo.issue("SHOPPING", cap, "device", "run")
            assertNull(repo.pending("SHOPPING"))
        }
        assertEquals(2, api.sent.size)
        assertEquals(api.sent[0], api.sent[1])
    }

    @Test fun `changing selection cannot replace a frozen ambiguous request`() = runTest {
        val api = FakeApi().apply { failure = IOException() }
        val repo = repository(api, backgroundScope)
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        assertFails { repo.issue("SHOPPING", other, "device", "run") }
        assertEquals(1, api.sent.size)
        assertEquals(cap, repo.pending("SHOPPING")?.itemId)
    }

    @Test fun `definite unknown cap rejection releases selection and new choice gets a new key`() = runTest {
        val api = FakeApi().apply { failure = httpError(422, "UNKNOWN_ACCESSORY") }
        val repo = repository(api, backgroundScope)
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        assertNull(repo.pending("SHOPPING"))
        api.failure = null
        repo.issue("SHOPPING", other, "device", "run")
        assertNotEquals(api.sent[0].first, api.sent[1].first)
        assertEquals(other, api.sent[1].second.reward.itemId)
    }

    @Test fun `mismatched grant does not clear a request`() = runTest {
        val api = FakeApi().apply { response = grant(other) }
        val repo = repository(api, backgroundScope)
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        assertEquals(cap, repo.pending("SHOPPING")?.itemId)
    }

    @Test fun `profile or run changes cannot retarget issuance`() = runTest {
        val api = FakeApi()
        val session = FakeSession()
        val repo = repository(api, backgroundScope, session = session)
        api.snapshot = snapshot("new-run")
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        api.snapshot = snapshot("run")
        session.id.value = "different-device"
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        assertTrue(api.sent.isEmpty())
    }

    @Test fun `profile switched during availability check cannot receive an old screen request`() = runTest {
        val api = FakeApi()
        val session = FakeSession()
        api.onPull = { session.id.value = "different-device" }
        val repo = repository(api, backgroundScope, session = session)
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        assertTrue(api.sent.isEmpty())
    }

    @Test fun `broken journal sequence blocks a new request`() = runTest {
        val api = FakeApi().apply { grants = listOf(grant(other).copy(sequence = 2)) }
        val repo = repository(api, backgroundScope)
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        assertTrue(api.sent.isEmpty())
    }

    @Test fun `malformed snapshot cannot be interpreted as empty inventory`() = runTest {
        val api = FakeApi().apply { snapshot = snapshot("run").copy(snapshotJson = """{"runId":"run","worldFormatVersion":1,"state":{}}""") }
        val repo = repository(api, backgroundScope)
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        assertTrue(api.sent.isEmpty())
    }

    @Test fun `all journal pages are read before offering an already issued cap`() = runTest {
        val api = FakeApi().apply {
            pages = listOf(
                QuestRewardsPage("device", "run", listOf(grant(other)), 1, true, 1),
                QuestRewardsPage("device", "run", listOf(grant(cap).copy(rewardId = "second", sequence = 2)), 2, false, 1),
            )
        }
        val repo = repository(api, backgroundScope)
        assertEquals(setOf(cap, other), repo.inventory().filterNotNull().first().ownedItemIds)
        assertEquals(listOf(0L, 1L), api.cursors)
    }

    @Test fun `retry after server throttling cannot send a second request early`() = runTest {
        val api = FakeApi().apply { failure = httpError(429, "RATE_LIMITED") }
        val repo = repository(api, backgroundScope)
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        api.failure = null
        assertFails { repo.issue("SHOPPING", cap, "device", "run") }
        assertEquals(1, api.sent.size)
    }

    private fun repository(api: FakeApi, scope: CoroutineScope,
        file: File = File(folder.root, "requests.preferences_pb"), session: FakeSession = FakeSession(),
    ) = RemoteParentQuestRewardsRepository(api, session,
        QuestRewardRequestStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }), json), json)

    private fun snapshot(run: String, owned: String = "") = RewardSnapshotResponse(run, 1, "fingerprint",
        """{"worldFormatVersion":1,"runId":"$run","state":{"ownedItems":[$owned]}}""", 1, "CURRENT_WORLD")
    private fun grant(item: String) = QuestRewardGrant("reward", "device", "run", 1,
        json.parseToJsonElement("""{"type":"ACCESSORY","itemId":"$item"}""").let { it as kotlinx.serialization.json.JsonObject }, "2026-09-29T00:00:00Z")

    private inner class FakeApi : ParentQuestRewardsApi {
        var snapshot = snapshot("run")
        var grants = emptyList<QuestRewardGrant>()
        var failure: Exception? = null
        var response: QuestRewardGrant? = null
        var onPull: () -> Unit = {}
        var pages: List<QuestRewardsPage>? = null
        val cursors = mutableListOf<Long>()
        val sent = mutableListOf<Pair<String, CreateQuestRewardRequest>>()
        override suspend fun downloadSnapshot(body: RewardSnapshotRequest) = snapshot
        override suspend fun pullRewards(body: QuestRewardsPullRequest): QuestRewardsPage {
            onPull()
            cursors += body.afterSequence
            pages?.let { return it[cursors.size - 1] }
            return QuestRewardsPage(body.deviceId, body.gameRunId, grants, grants.lastOrNull()?.sequence ?: body.afterSequence, false, 1)
        }
        override suspend fun createReward(requestId: String, body: CreateQuestRewardRequest): QuestRewardGrant {
            sent += requestId to body
            failure?.let { throw it }
            return response ?: grant(body.reward.itemId)
        }
    }

    private class FakeSession : PetSessionStore {
        val id = MutableStateFlow<String?>("device")
        override suspend fun lastPetId() = id.value
        override fun observePetId() = id
        override suspend fun savePetId(petId: String) { id.value = petId }
        override suspend fun clearPetId() { id.value = null }
    }

    private fun httpError(status: Int, code: String) = HttpException(Response.error<Any>(status,
        """{"code":"$code"}""".toResponseBody("application/json".toMediaType())))

    private suspend fun assertFails(block: suspend () -> Unit) {
        try { block() } catch (_: Exception) { return }
        fail("Expected operation to reject unsafe issuance")
    }
}
