package ru.nksk.parentsapp.feature.questions.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.nksk.parentsapp.core.report.data.LinkedPet
import ru.nksk.parentsapp.core.report.data.ParentReportRepository
import ru.nksk.parentsapp.core.report.data.ParentReportResponse
import ru.nksk.parentsapp.core.report.data.PetDto
import ru.nksk.parentsapp.core.report.data.PetSessionStore
import ru.nksk.parentsapp.core.report.data.SkillDto
import ru.nksk.parentsapp.core.report.data.SkillStatusDto
import java.io.IOException

internal class FakePetSessionStore : PetSessionStore {
    private val store = MutableStateFlow<String?>(null)
    private val pets = MutableStateFlow<List<LinkedPet>>(emptyList())

    /** Non-suspend mirror for seeding and assertions. */
    var current: String?
        get() = store.value
        set(value) {
            store.value = value
            pets.value = if (value == null) emptyList()
            else listOf(LinkedPet(value)) + pets.value.filterNot { it.petId == value }
        }

    override suspend fun lastPetId(): String? = store.value
    override fun observePetId(): Flow<String?> = store.map { it }
    override suspend fun savePetId(petId: String) { current = petId }
    override suspend fun clearPetId() { current = null }
    override fun observePets(): Flow<List<LinkedPet>> = pets
    override suspend fun removePet(petId: String) {
        pets.value = pets.value.filterNot { it.petId == petId }
        if (store.value == petId) store.value = pets.value.firstOrNull()?.petId
    }
    override suspend fun renamePet(petId: String, name: String) {
        pets.value = pets.value.map { if (it.petId == petId) it.copy(name = name) else it }
    }
}

internal class FakeReportRepository(private var fail: Boolean = false) : ParentReportRepository {
    var requests: Int = 0
    var requestedPetId: String? = null

    fun setFail(value: Boolean) { fail = value }

    override suspend fun getReport(petId: String): ParentReportResponse {
        requests++
        requestedPetId = petId
        if (fail) throw IOException("network down")
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
