package ru.nksk.parentsapp.feature.report.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ru.nksk.parentsapp.core.report.data.LinkedPet
import ru.nksk.parentsapp.core.report.data.ParentReportResponse
import ru.nksk.parentsapp.core.report.data.ParentReportRepository
import ru.nksk.parentsapp.core.report.data.PetDto
import ru.nksk.parentsapp.core.report.data.PetSessionStore
import ru.nksk.parentsapp.core.report.data.SkillDto
import ru.nksk.parentsapp.core.report.data.SkillStatusDto

internal class FakePetSessionStore : PetSessionStore {
    private val petIdFlow = MutableStateFlow<String?>(null)
    private val petsFlow = MutableStateFlow<List<LinkedPet>>(emptyList())

    /** Non-suspend mirror for assertions and seeding in tests. */
    var current: String?
        get() = petIdFlow.value
        set(value) {
            petIdFlow.value = value
            if (value == null) petsFlow.value = emptyList()
            else petsFlow.value = listOf(LinkedPet(value)) + petsFlow.value.filterNot { it.petId == value }
        }

    var saves: Int = 0

    override suspend fun lastPetId(): String? = petIdFlow.value
    override fun observePetId(): Flow<String?> = petIdFlow
    override suspend fun savePetId(petId: String) {
        saves += 1
        current = petId
    }
    override suspend fun clearPetId() {
        current = null
    }
    override fun observePets(): Flow<List<LinkedPet>> = petsFlow
    override suspend fun removePet(petId: String) {
        petsFlow.value = petsFlow.value.filterNot { it.petId == petId }
        if (petIdFlow.value == petId) petIdFlow.value = petsFlow.value.firstOrNull()?.petId
    }
    override suspend fun renamePet(petId: String, name: String) {
        petsFlow.value = petsFlow.value.map { if (it.petId == petId) it.copy(name = name) else it }
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
