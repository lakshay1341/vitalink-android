package com.lakshay.vitalink.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface VitaLinkApi {
    @POST("api/authenticate")
    suspend fun authenticate(@Body body: LoginRequest): JwtResponse

    @GET("api/encounters")
    suspend fun encounters(): List<Encounter>

    @GET("api/encounters/{id}/news2")
    suspend fun news2(@Path("id") id: Long): News2Result

    @GET("api/encounters/{id}/vitals/latest")
    suspend fun latestVitals(@Path("id") id: Long): List<LatestVital>

    @GET("api/alerts")
    suspend fun alerts(
        @Query("encounterId.equals") encounterId: Long,
        @Query("sort") sort: String = "triggeredAt,desc",
        @Query("size") size: Int = 20,
    ): List<Alert>

    @GET("api/alert-rules")
    suspend fun alertRules(@Query("encounterId") encounterId: Long? = null): List<AlertRule>

    @PUT("api/alert-rules/{id}")
    suspend fun updateAlertRule(@Path("id") id: Long, @Body rule: AlertRule): AlertRule
}
