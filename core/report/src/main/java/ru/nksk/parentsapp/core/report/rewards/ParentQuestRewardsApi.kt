package ru.nksk.parentsapp.core.report.rewards

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** This parent client never uploads game state or sends delivery acknowledgements. */
internal interface ParentQuestRewardsApi {
    @POST("v1/profiles/snapshot/download")
    suspend fun downloadSnapshot(@Body body: RewardSnapshotRequest): RewardSnapshotResponse

    @POST("v1/profiles/rewards/pull")
    suspend fun pullRewards(@Body body: QuestRewardsPullRequest): QuestRewardsPage

    @POST("v1/parent-profiles/rewards")
    suspend fun createReward(@Header("Idempotency-Key") requestId: String,
        @Body body: CreateQuestRewardRequest): QuestRewardGrant
}

@Serializable
internal data class RewardSnapshotRequest(val deviceId: String, val schemaVersion: Int)
@Serializable
internal data class RewardSnapshotResponse(val gameRunId: String, val serverRevision: Long,
    val currentContentFingerprint: String, val snapshotJson: String, val schemaVersion: Int = 1,
    val payloadKind: String? = null)
@Serializable
internal data class QuestRewardsPullRequest(val deviceId: String, val gameRunId: String,
    val afterSequence: Long, val limit: Int, val schemaVersion: Int)
@Serializable
internal data class QuestRewardsPage(val profileId: String, val gameRunId: String,
    val rewards: List<QuestRewardGrant>, val nextAfterSequence: Long, val hasMore: Boolean,
    val schemaVersion: Int = 1)
@Serializable
internal data class QuestRewardGrant(val rewardId: String, val profileId: String, val gameRunId: String,
    val sequence: Long, val reward: JsonObject, val createdAt: String)
@Serializable
internal data class AccessoryReward(val type: String, val itemId: String)
@Serializable
internal data class CreateQuestRewardRequest(val deviceId: String, val gameRunId: String,
    val reward: AccessoryReward, val schemaVersion: Int)
