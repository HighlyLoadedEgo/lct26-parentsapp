package ru.nksk.parentsapp.core.report.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** Shared parent copy for observed episodes, not a local mastery assessment. */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class SkillExplanationDto(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val version: Int = 1,
    val description: String,
    val summary: String,
    val evidence: List<SkillExplanationEvidenceDto>,
)

@Serializable
data class SkillExplanationEvidenceDto(
    val episodeId: String,
    val sourceEventIds: List<String>,
    val text: String,
)

/** Read-side validation shared by local and remote parent reports. */
fun SkillExplanationDto.isSupported(): Boolean =
    version == 1 && description.isExplanationText() && description.length in 1..400 &&
        summary.isExplanationText() && summary.length in 1..600 && evidence.size <= 2 &&
        evidence.map { it.episodeId }.distinct().size == evidence.size &&
        evidence.all { item ->
            item.episodeId.isExplanationText() && item.text.isExplanationText() && item.text.length in 1..400 &&
                item.sourceEventIds.isNotEmpty() && item.sourceEventIds.all(String::isExplanationText) &&
                item.sourceEventIds.distinct().size == item.sourceEventIds.size
        }

private fun String.isExplanationText(): Boolean = isNotBlank() && '\u0000' !in this

