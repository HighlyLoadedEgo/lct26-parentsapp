package ru.nksk.parentsapp.core.report.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Read-only game endpoints, shared with the child's app. Never use the legacy demo report. */
interface ParentReportApi {
    @POST("v1/profiles/snapshot/download")
    suspend fun downloadSnapshot(@Body request: ReportSnapshotRequest): ReportSnapshotResponse

    @POST("v1/profiles/skills/query")
    suspend fun getSkills(@Body request: ReportSkillsRequest): ReportSkillsResponse

    @GET("v1/parent-materials")
    suspend fun getMaterials(): ReportMaterialsResponse
}
