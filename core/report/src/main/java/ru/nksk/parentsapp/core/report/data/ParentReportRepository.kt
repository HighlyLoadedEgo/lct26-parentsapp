package ru.nksk.parentsapp.core.report.data

import javax.inject.Inject
import javax.inject.Singleton

/** Parent report data access; the only door to the backend from the report feature. */
interface ParentReportRepository {
    suspend fun getReport(petId: String): ParentReportResponse
}

@Singleton
class ParentReportRepositoryImpl @Inject constructor(
    private val api: ParentReportApi,
) : ParentReportRepository {
    override suspend fun getReport(petId: String): ParentReportResponse =
        api.getParentReport(petId)
}
