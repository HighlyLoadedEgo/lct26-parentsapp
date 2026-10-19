package ru.nksk.parentsapp.core.report.data

import kotlinx.serialization.Serializable

/** Server episode totals; no local mastery calculation. All fields required when supplied. */
@Serializable
data class ObservationCountsDto(
    val observedEpisodes: Int,
    val completedEpisodes: Int,
    val supportedEpisodes: Int,
    val difficultyEpisodes: Int,
    val neutralEpisodes: Int,
    val pendingEpisodes: Int,
    val incompleteEpisodes: Int,
    val supportedWithoutGameHints: Int,
    val assistedEpisodes: Int,
) {
    internal fun isValid(): Boolean = listOf(observedEpisodes, completedEpisodes,
        supportedEpisodes, difficultyEpisodes, neutralEpisodes, pendingEpisodes,
        incompleteEpisodes, supportedWithoutGameHints, assistedEpisodes).all { it >= 0 }
}
