package ru.nksk.parentsapp.feature.report.ui

import androidx.annotation.DrawableRes
import ru.nksk.parentsapp.core.report.data.PetDto

/** Mirrors the game's saved appearance, including special states that replace accessories. */
@DrawableRes
internal fun reportPetArtwork(pet: PetDto): Int? {
    val art = petArtwork(pet.age, pet.color)
    return when (pet.visualState) {
        "NEEDS_HELP" -> art.sick
        "HUNGRY" -> art.hungry
        "TIRED" -> art.tired
        "THINKING" -> art.thinking
        "UPSET" -> art.upset
        "HAPPY" -> art.happy
        "NORMAL" -> when (pet.selectedLookId) {
            "PLAIN" -> art.plain
            "BANDANA" -> art.bandana
            "BACKPACK" -> art.backpack
            "GLASSES" -> art.glasses
            "HAT" -> art.hat
            "ROUTE_PATCH" -> art.routePatch
            "COMPASS" -> art.compass
            "BINOCULARS" -> art.binoculars
            else -> rewardCapArtwork(pet.selectedLookId, pet.age, pet.color)
        }
        // The main app has no verified image for WORRIED or unknown future states either.
        else -> null
    }
}
