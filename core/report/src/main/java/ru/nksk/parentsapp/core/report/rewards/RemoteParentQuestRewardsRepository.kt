package ru.nksk.parentsapp.core.report.rewards

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import retrofit2.HttpException
import ru.nksk.parentsapp.core.report.data.PetSessionStore

@Singleton
internal class RemoteParentQuestRewardsRepository @Inject constructor(
    private val api: ParentQuestRewardsApi,
    private val session: PetSessionStore,
    private val store: QuestRewardRequestStore,
    private val json: Json,
) : ParentQuestRewardsRepository {
    private val mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun inventory(): Flow<ParentRewardInventory?> = session.observePetId().distinctUntilChanged()
        .flatMapLatest { deviceId -> flow {
            emit(null)
            if (!deviceId.isNullOrBlank()) {
                while (true) {
                    emit(loadInventory(deviceId))
                    delay(60_000)
                }
            }
        } }.flowOn(Dispatchers.IO)

    override suspend fun pending(questId: String): PendingParentQuestReward? = withContext(Dispatchers.IO) {
        validateQuest(questId)
        val deviceId = session.lastPetId()?.takeIf { it.isNotBlank() }
            ?: throw ParentQuestRewardException("Сначала привяжите игру ребёнка на экране статистики.")
        val snapshot = loadSnapshot(deviceId)
        ensureDevice(deviceId)
        store.pending(deviceId, snapshot.gameRunId, questId)?.let { frozen ->
            validateFrozen(frozen, questId, deviceId, snapshot.gameRunId)
            PendingParentQuestReward(deviceId, snapshot.gameRunId, frozen.request.reward.itemId)
        }
    }

    override suspend fun issue(questId: String, itemId: String, expectedDeviceId: String, expectedRunId: String) =
        mutex.withLock { withContext(Dispatchers.IO) {
            validateQuest(questId)
            require(ParentRewardCaps.forItem(itemId) != null)
            ensureDevice(expectedDeviceId)
            val previous = store.pending(expectedDeviceId, expectedRunId, questId)
            val frozen = previous ?: run {
                // Never treat a missing/unreadable snapshot as an empty inventory.
                val inventory = loadInventory(expectedDeviceId)
                if (inventory.gameRunId != expectedRunId) throw changedTarget()
                if (itemId in inventory.ownedItemIds) throw ParentQuestRewardException(
                    "Эта кепка уже есть у ребёнка или выдана ему. Выберите другую.")
                FrozenQuestReward(UUID.randomUUID().toString(), questId,
                    CreateQuestRewardRequest(expectedDeviceId, expectedRunId, AccessoryReward("ACCESSORY", itemId), 1)
                ).also { store.stage(it) }
            }
            validateFrozen(frozen, questId, expectedDeviceId, expectedRunId)
            if (frozen.request.reward.itemId != itemId) throw ParentQuestRewardException(
                "Сначала повторите выдачу ранее выбранной кепки.")
            // A previous request may already have succeeded. Retry its exact key/body;
            // the server enforces current-run identity even when its response was lost.
            ensureDevice(expectedDeviceId)
            val grant = try {
                network { api.createReward(frozen.requestId, frozen.request) }
            } catch (error: HttpException) {
                val code = errorCode(error)
                if (error.code() == 422 && code in setOf("UNKNOWN_ACCESSORY", "INVALID_REWARD")) {
                    store.clear(frozen)
                    throw ParentQuestRewardException(if (code == "UNKNOWN_ACCESSORY")
                        "Эта кепка пока недоступна на сервере. Попробуйте позже."
                    else "Сервер отклонил запрос выдачи награды. Попробуйте позже.")
                }
                if (error.code() == 409 && code == "GAME_RUN_NOT_REGISTERED") throw ParentQuestRewardException(
                    "Сначала откройте игру ребёнка и дождитесь синхронизации, затем повторите выдачу.")
                if (error.code() == 409 && code == "GAME_RUN_CONFLICT") throw changedTarget()
                throw error
            }
            validateGrant(grant, expectedDeviceId, expectedRunId)
            check(grant.reward.string("type") == "ACCESSORY" && grant.reward.string("itemId") == itemId) {
                "Ответ сервера не соответствует выбранной награде. Повторите запрос."
            }
            ensureDevice(expectedDeviceId)
            store.clear(frozen)
            // Do not ACK: only the game client's inventory transaction proves delivery.
        } }

    private suspend fun loadInventory(deviceId: String): ParentRewardInventory {
        val snapshot = loadSnapshot(deviceId)
        val document = json.parseToJsonElement(snapshot.snapshotJson).jsonObject
        val owned = document.getValue("state").jsonObject.getValue("ownedItems").jsonArray.map { item ->
            item.jsonObject.string("itemId").also { check(!it.isNullOrBlank()) { "Invalid owned item" } }!!
        }.toMutableSet()
        var cursor = 0L
        val rewardIds = mutableSetOf<String>()
        do {
            val page = network { api.pullRewards(QuestRewardsPullRequest(deviceId, snapshot.gameRunId, cursor, 50, 1)) }
            check(page.schemaVersion == 1 && page.profileId == deviceId && page.gameRunId == snapshot.gameRunId) {
                "Журнал наград относится к другому профилю или прохождению."
            }
            check(page.rewards.size <= 50 && (!page.hasMore || page.rewards.isNotEmpty())) { "Invalid rewards page" }
            page.rewards.forEach { grant ->
                validateGrant(grant, deviceId, snapshot.gameRunId)
                check(cursor < Long.MAX_VALUE && grant.sequence == cursor + 1 && rewardIds.add(grant.rewardId)) {
                    "Нарушен порядок журнала наград. Попробуйте обновить позже."
                }
                cursor = grant.sequence
                if (grant.reward.string("type") == "ACCESSORY") owned += grant.reward.string("itemId")!!
            }
            check(page.nextAfterSequence == cursor) { "Invalid rewards cursor" }
        } while (page.hasMore)
        ensureDevice(deviceId)
        return ParentRewardInventory(deviceId, snapshot.gameRunId, owned)
    }

    private suspend fun loadSnapshot(deviceId: String): RewardSnapshotResponse {
        val snapshot = try { network { api.downloadSnapshot(RewardSnapshotRequest(deviceId, 1)) } }
        catch (error: HttpException) {
            if (error.code() == 404) throw ParentQuestRewardException(
                "Сначала откройте игру ребёнка и дождитесь синхронизации.")
            throw error
        }
        check(snapshot.schemaVersion == 1 && snapshot.serverRevision >= 1 && snapshot.gameRunId.isNotBlank() &&
            snapshot.currentContentFingerprint.isNotBlank() && snapshot.payloadKind in setOf(null, "CURRENT_WORLD")) {
            "Снимок игры не поддерживается. Обновите приложения."
        }
        val document = json.parseToJsonElement(snapshot.snapshotJson).jsonObject
        check(document.string("runId") == snapshot.gameRunId) { "Snapshot run mismatch" }
        val worldVersion = document["worldFormatVersion"]?.jsonPrimitive?.intOrNull
        val legacyVersion = document["formatVersion"]?.jsonPrimitive?.intOrNull
        check(if (snapshot.payloadKind == "CURRENT_WORLD" || worldVersion != null) worldVersion == 1
            else legacyVersion in 1..5) { "Unsupported snapshot format" }
        // Validate the projection used for availability; do not deserialize/restore the game itself.
        document.getValue("state").jsonObject.getValue("ownedItems").jsonArray.forEach { item ->
            check(!item.jsonObject.string("itemId").isNullOrBlank()) { "Invalid owned item" }
        }
        return snapshot
    }

    private fun validateGrant(grant: QuestRewardGrant, deviceId: String, runId: String) {
        check(grant.profileId == deviceId && grant.gameRunId == runId && grant.sequence > 0 &&
            grant.rewardId.isNotBlank() && grant.createdAt.isNotBlank()) { "Invalid reward identity" }
        check(when (grant.reward.string("type")) {
            "ACCESSORY" -> !grant.reward.string("itemId").isNullOrBlank()
            "COINS" -> (grant.reward["amount"]?.jsonPrimitive?.longOrNull ?: 0) > 0
            else -> false
        }) { "Unsupported reward payload" }
    }

    private fun validateFrozen(value: FrozenQuestReward, questId: String, deviceId: String, runId: String) {
        check(value.requestId.isNotBlank() && value.questId == questId && value.request.deviceId == deviceId &&
            value.request.gameRunId == runId && value.request.schemaVersion == 1 &&
            value.request.reward.type == "ACCESSORY" && ParentRewardCaps.forItem(value.request.reward.itemId) != null) {
            "Сохранённый запрос выдачи требует проверки."
        }
    }

    private suspend fun ensureDevice(expected: String) {
        if (expected.isBlank() || session.lastPetId() != expected) throw changedTarget()
    }
    private fun validateQuest(questId: String) { require(questId in setOf("SHOPPING", "WEEKEND", "SECOND_LIFE")) }
    private fun changedTarget() = ParentQuestRewardException("Профиль или прохождение изменились. Откройте квест заново.")
    private fun JsonObject.string(key: String): String? = get(key)?.jsonPrimitive?.takeIf { it.isString }?.content
    private fun errorCode(error: HttpException): String? = runCatching {
        error.response()?.errorBody()?.string()?.let { json.parseToJsonElement(it).jsonObject.string("code") }
    }.getOrNull()

    private suspend fun <T> network(block: suspend () -> T): T {
        if (store.blockedUntil() > System.currentTimeMillis()) throw ParentQuestRewardException(
            "Сервер просит подождать. Повторите попытку чуть позже.")
        try { return block() }
        catch (error: HttpException) {
            if (error.code() != 429) throw error
            val now = System.currentTimeMillis()
            val header = error.response()?.headers()?.get("Retry-After")
            val seconds = header?.toLongOrNull()?.coerceIn(0, (Long.MAX_VALUE - now) / 1000)
            val deadline = seconds?.let { now + it * 1000 } ?: runCatching {
                SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("GMT"); isLenient = false
                }.parse(header.orEmpty())?.time
            }.getOrNull() ?: (now + 60_000)
            store.deferUntil(maxOf(deadline, now + 1_000))
            throw ParentQuestRewardException("Сервер просит подождать. Повторите попытку чуть позже.")
        }
    }
}
