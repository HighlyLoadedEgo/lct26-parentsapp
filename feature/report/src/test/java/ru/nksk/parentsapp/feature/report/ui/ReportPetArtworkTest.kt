package ru.nksk.parentsapp.feature.report.ui

import org.junit.Assert.*
import org.junit.Test
import ru.nksk.parentsapp.core.report.data.PetDto
import ru.nksk.parentsapp.core.report.data.PetAgeDto
import ru.nksk.parentsapp.core.report.data.PetColorDto
import ru.nksk.parentsapp.feature.report.R

class ReportPetArtworkTest {
    private val pet = PetDto("id", "Лис", "", 0, "CAP_LCT2026_EMERALD", "NORMAL")

    @Test fun `saved cap is displayed instead of static backpack`() {
        assertEquals(R.drawable.ryzhik_cub_body_accessory_cap_lct2026_emerald, reportPetArtwork(pet))
        assertEquals(R.drawable.ryzhik_adult_body_accessory_cap_lct2026_emerald_sand,
            reportPetArtwork(pet.copy(age = PetAgeDto.ADULT, color = PetColorDto.SAND)))
    }

    @Test fun `special state replaces saved accessory as in the game`() {
        assertEquals(R.drawable.ryzhik_cub_state_hungry_copper, reportPetArtwork(pet.copy(visualState = "HUNGRY")))
        assertNull(reportPetArtwork(pet.copy(visualState = "WORRIED")))
        assertNull(reportPetArtwork(pet.copy(selectedLookId = "FUTURE_LOOK")))
    }
}
