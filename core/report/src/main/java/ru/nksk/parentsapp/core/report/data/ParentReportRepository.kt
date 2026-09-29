package ru.nksk.parentsapp.core.report.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class ReportSnapshotUnavailableException : Exception("No synchronized game snapshot")

interface ParentReportRepository {
    suspend fun getReport(petId: String): ParentReportResponse
}

@Singleton
class ParentReportRepositoryImpl @Inject constructor(
    private val api: ParentReportApi,
) : ParentReportRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getReport(petId: String): ParentReportResponse = coroutineScope {
        require(petId.isNotBlank())
        val envelope = try {
            api.downloadSnapshot(ReportSnapshotRequest(petId))
        } catch (error: retrofit2.HttpException) {
            if (error.code() == 404) throw ReportSnapshotUnavailableException()
            throw error
        }
        require(envelope.schemaVersion == 1 && envelope.serverRevision >= 1)
        require(envelope.payloadKind == null || envelope.payloadKind == "CURRENT_WORLD")
        val world = json.decodeFromString<ReportWorld>(envelope.snapshotJson)
        require(world.runId == envelope.gameRunId && world.historySequence >= 0)
        require(world.worldFormatVersion == 1 ||
            (envelope.payloadKind == null && world.worldFormatVersion == null && world.formatVersion in 1..5))
        val pet = world.state.pet
        val money = world.state.economy
        require(pet.name.isNotBlank() && money.availableBalance >= 0 && money.savingsBalance >= 0)
        val skillsRequest = async {
            optional {
                api.getSkills(ReportSkillsRequest(petId, world.runId)).also {
                    require(it.schemaVersion == 1 && it.gameRunId == world.runId)
                    require(it.basedOnHistorySequence in 0..world.historySequence)
                    require(it.skills.size == titles.size && it.skills.map { skill -> skill.skillId }.toSet() == titles.keys)
                    require(it.skills.all { skill -> skill.policyVersion.isNotBlank() })
                }
            }
        }
        val materials = optional { api.getMaterials().also {
            require(it.schemaVersion == 1 && it.contentVersion.isNotBlank())
            require(it.publicationStatus in setOf("PUBLISHED", "UNPUBLISHED"))
            require(it.skills.map { skill -> skill.skillId }.distinct().size == it.skills.size)
        } }
        val assessments = skillsRequest.await()
        val bySkill = assessments?.skills?.associateBy { it.skillId }.orEmpty()
        val topics = materials?.takeIf { it.publicationStatus == "PUBLISHED" }?.skills?.associateBy { it.skillId }.orEmpty()
        ParentReportResponse(
            pet = PetDto(petId, pet.name, when (pet.temperament) {
                "Curious" -> "Любознательный"
                "Confident" -> "Уверенный"
                "Joyful" -> "Жизнерадостный"
                else -> ""
            }, Math.addExact(money.availableBalance, money.savingsBalance), pet.selectedLookId,
                pet.visualState, pet.age, pet.color, money.availableBalance, money.savingsBalance),
            skills = titles.map { (id, title) ->
                val material = topics[id]
                SkillDto(id, title, bySkill[id]?.status ?: SkillStatusDto.NO_DATA,
                    assessmentAvailable = bySkill[id] != null,
                    materialsAvailable = material != null,
                    learningGoal = material?.learningGoal.orEmpty(), story = material?.story.orEmpty(),
                    replaceWithParentStory = material?.replaceWithParentStory.orEmpty(),
                    conversationStarters = material?.conversationStarters.orEmpty(),
                    parentTakeaway = material?.parentTakeaway.orEmpty(),
                    researchBasis = material?.researchBasis.orEmpty(), researchSources = material?.researchSources.orEmpty())
            },
            isDemo = false,
            assessmentsAvailable = assessments != null,
            assessmentsStale = assessments != null && assessments.basedOnHistorySequence < world.historySequence,
            materialsLoadFailed = materials == null,
        )
    }
}

private suspend fun <T> optional(block: suspend () -> T): T? = try {
    block()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

/** Same parent-facing skill names as the main application; assessment policy belongs to the server. */
private val titles = linkedMapOf(
    "FIN-01" to "Сравнивает денежные суммы",
    "FIN-02" to "Планирует бюджет на период",
    "FIN-03" to "Учитывает обязательные нужды перед желаниями",
    "FIN-04" to "Следит, чтобы денег хватало до следующего дохода",
    "FIN-05" to "Последовательно собирает на выбранную цель",
    "FIN-06" to "Откладывает желанную покупку ради приоритета",
    "FIN-07" to "Создаёт запас на непредвиденные расходы",
    "FIN-08" to "Перестраивает действия после неожиданной траты",
    "FIN-09" to "Сопоставляет денежные и другие затраты",
    "FIN-10" to "Планирует дополнительный заработок",
    "FIN-11" to "Разбирает финансовые последствия и меняет решение",
    "FIN-12" to "Понимает свои доходы и расходы",
)
