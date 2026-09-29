package ru.nksk.parentsapp.core.report.data

import kotlinx.serialization.Serializable

@Serializable
data class ReportSnapshotRequest(val deviceId: String, val schemaVersion: Int = 1)
@Serializable
data class ReportSnapshotResponse(
    val gameRunId: String, val serverRevision: Long, val currentContentFingerprint: String,
    val snapshotJson: String, val schemaVersion: Int = 1, val payloadKind: String? = null,
)
@Serializable
data class ReportSkillsRequest(val deviceId: String, val gameRunId: String, val schemaVersion: Int = 1)
@Serializable
data class ReportSkillsResponse(
    val gameRunId: String, val basedOnHistorySequence: Long, val skills: List<ReportAssessmentDto>,
    val schemaVersion: Int = 1,
)
@Serializable
data class ReportAssessmentDto(val skillId: String, val status: SkillStatusDto, val policyVersion: String)
@Serializable
data class ReportMaterialsResponse(
    val contentVersion: String, val skills: List<ReportMaterialDto>, val schemaVersion: Int = 1,
    val publicationStatus: String = "PUBLISHED",
)
@Serializable
data class ReportMaterialDto(
    val skillId: String, val learningGoal: String, val story: String,
    val replaceWithParentStory: String, val conversationStarters: List<String>,
    val parentTakeaway: String, val researchBasis: String, val researchSources: List<String>,
)

// A read-only projection: no game state is reconstructed or uploaded by the parent app.
@Serializable
internal data class ReportWorld(
    val runId: String, val historySequence: Long, val state: ReportGameState,
    val worldFormatVersion: Int? = null, val formatVersion: Int? = null,
)
@Serializable
internal data class ReportGameState(val pet: ReportSavedPet, val economy: ReportEconomy)
@Serializable
internal data class ReportSavedPet(
    val name: String, val selectedLookId: String, val visualState: String,
    val age: PetAgeDto = PetAgeDto.CUB, val color: PetColorDto = PetColorDto.COPPER,
    val temperament: String? = null,
)
@Serializable
internal data class ReportEconomy(val availableBalance: Long, val savingsBalance: Long)
@Serializable
enum class PetAgeDto { CUB, TEEN, ADULT, SENIOR }
@Serializable
enum class PetColorDto { COPPER, SAND, DARK_RUSSET }
