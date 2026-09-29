package ru.nksk.parentsapp.core.report.rewards

import kotlinx.coroutines.flow.Flow

data class ParentRewardCap(val itemId: String, val lookId: String, val title: String)

/** Same reward IDs and order as LCTApp's PetCosmetics.parentRewards catalog. */
object ParentRewardCaps {
    val all = listOf(
        ParentRewardCap("cosmetic-cap-moscow-blue-v1", "CAP_MOSCOW_BLUE", "Кепка с гербом Москвы - синяя"),
        ParentRewardCap("cosmetic-cap-moscow-emerald-v1", "CAP_MOSCOW_EMERALD", "Кепка с гербом Москвы - изумрудная"),
        ParentRewardCap("cosmetic-cap-moscow-burgundy-v1", "CAP_MOSCOW_BURGUNDY", "Кепка с гербом Москвы - бордовая"),
        ParentRewardCap("cosmetic-cap-lct2026-blue-v1", "CAP_LCT2026_BLUE", "Кепка ЛЦТ 2026 - синяя"),
        ParentRewardCap("cosmetic-cap-lct2026-emerald-v1", "CAP_LCT2026_EMERALD", "Кепка ЛЦТ 2026 - изумрудная"),
        ParentRewardCap("cosmetic-cap-lct2026-burgundy-v1", "CAP_LCT2026_BURGUNDY", "Кепка ЛЦТ 2026 - бордовая"),
    )
    fun forItem(itemId: String): ParentRewardCap? = all.firstOrNull { it.itemId == itemId }
}

data class ParentRewardInventory(val deviceId: String, val gameRunId: String, val ownedItemIds: Set<String>)
data class PendingParentQuestReward(val deviceId: String, val gameRunId: String, val itemId: String)

interface ParentQuestRewardsRepository {
    fun inventory(): Flow<ParentRewardInventory?>
    suspend fun pending(questId: String): PendingParentQuestReward?
    /** Validated server issuance only. The game client alone applies and acknowledges delivery. */
    suspend fun issue(questId: String, itemId: String, expectedDeviceId: String, expectedRunId: String)
}

class ParentQuestRewardException(message: String) : IllegalStateException(message)
