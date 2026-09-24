package ru.nksk.parentsapp.feature.report.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ru.nksk.parentsapp.core.report.data.ParentReportResponse
import ru.nksk.parentsapp.core.report.data.ParentReportRepository
import ru.nksk.parentsapp.core.report.data.PetDto
import ru.nksk.parentsapp.core.report.data.PetSessionStore
import ru.nksk.parentsapp.core.report.data.SkillDto
import ru.nksk.parentsapp.core.report.data.SkillStatusDto

internal class FakePetSessionStore : PetSessionStore {
    private val petIdFlow = MutableStateFlow<String?>(null)

    /** Non-suspend mirror for assertions and seeding in tests. */
    var current: String?
        get() = petIdFlow.value
        set(value) {
            petIdFlow.value = value
        }

    override suspend fun lastPetId(): String? = petIdFlow.value
    override fun observePetId(): Flow<String?> = petIdFlow
    override suspend fun savePetId(petId: String) {
        petIdFlow.value = petId
    }
    override suspend fun clearPetId() {
        petIdFlow.value = null
    }
}

internal class FakeReportRepository(private val fail: Boolean = false) : ParentReportRepository {
    var requests: Int = 0
    var requestedPetId: String? = null

    override suspend fun getReport(petId: String): ParentReportResponse {
        requests += 1
        requestedPetId = petId
        if (fail) throw java.io.IOException("network down")
        return ParentReportResponse(
            pet = PetDto(
                id = petId,
                name = "Рыжик",
                temper = "playful",
                balance = 100,
                selectedLookId = "BACKPACK",
                visualState = "NORMAL",
            ),
            skills = listOf(
                SkillDto("FIN-01", "Сравнивает суммы", SkillStatusDto.MASTERED, true),
                SkillDto("FIN-03", "Нужды перед желаниями", SkillStatusDto.PRACTICING, false),
                SkillDto("FIN-04", "Хватает до дохода", SkillStatusDto.NO_DATA, null),
            ),
            isDemo = true,
        )
    }
}
