package ru.nksk.parentsapp.core.report.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SkillStatusDto {
    @SerialName("MASTERED") MASTERED,
    @SerialName("PRACTICING") PRACTICING,
    @SerialName("NO_DATA") NO_DATA,
}

@Serializable
data class ParentReportResponse(
    val pet: PetDto,
    val skills: List<SkillDto>,
    @SerialName("isDemo") val isDemo: Boolean,
)

@Serializable
data class PetDto(
    val id: String,
    val name: String,
    val temper: String,
    val balance: Int,
    @SerialName("selectedLookId") val selectedLookId: String,
    @SerialName("visualState") val visualState: String,
)

@Serializable
data class SkillDto(
    val id: String,
    val title: String,
    val status: SkillStatusDto,
    @SerialName("isMastered") val isMastered: Boolean?,
)
