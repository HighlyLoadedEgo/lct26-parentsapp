package ru.nksk.parentsapp.core.report.data

import retrofit2.http.GET
import retrofit2.http.Path

interface ParentReportApi {
    @GET("api/parents/{petId}")
    suspend fun getParentReport(@Path("petId") petId: String): ParentReportResponse
}
