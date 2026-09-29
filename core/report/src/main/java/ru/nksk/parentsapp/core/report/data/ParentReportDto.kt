package ru.nksk.parentsapp.core.report.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SkillStatusDto {
    @SerialName("MASTERED") MASTERED,
    @SerialName("PRACTICING") PRACTICING,
    @SerialName("NO_DATA") NO_DATA,
    @SerialName("HAS_PROBLEM") HAS_PROBLEM,
}

@Serializable
data class ParentReportResponse(
    val pet: PetDto,
    val skills: List<SkillDto>,
    @SerialName("isDemo") val isDemo: Boolean,
    val assessmentsAvailable: Boolean = true,
    val assessmentsStale: Boolean = false,
    val materialsLoadFailed: Boolean = false,
)

@Serializable
data class PetDto(
    val id: String,
    val name: String,
    val temper: String,
    val balance: Long,
    @SerialName("selectedLookId") val selectedLookId: String,
    @SerialName("visualState") val visualState: String,
    val age: PetAgeDto = PetAgeDto.CUB,
    val color: PetColorDto = PetColorDto.COPPER,
    val availableBalance: Long = balance,
    val savingsBalance: Long = 0,
)

@Serializable
data class SkillDto(
    val id: String,
    val title: String,
    val status: SkillStatusDto,
    @SerialName("isMastered") val isMastered: Boolean? = null,
    val assessmentAvailable: Boolean = true,
    val materialsAvailable: Boolean = false,
    val learningGoal: String = "",
    val story: String = "",
    val replaceWithParentStory: String = "",
    val conversationStarters: List<String> = emptyList(),
    val parentTakeaway: String = "",
    val researchBasis: String = "",
    val researchSources: List<String> = emptyList(),
)
