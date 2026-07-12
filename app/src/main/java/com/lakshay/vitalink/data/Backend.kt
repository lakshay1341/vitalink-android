package com.lakshay.vitalink.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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
    suspend fun alertRules(): List<AlertRule>

    @PUT("api/alert-rules/{id}")
    suspend fun updateAlertRule(@Path("id") id: Long, @Body rule: AlertRule): AlertRule
}

/**
 * Legacy static network holder, kept only until the screens finish migrating to the
 * Hilt-injected repository. baseUrl/token now delegate to SessionManager so there is a
 * single source of truth shared with the DI network stack.
 * ponytail: transitional — deleted once no screen references Backend directly.
 */
object Backend {
    var baseUrl: String
        get() = SessionManager.baseUrl
        set(v) {
            SessionManager.baseUrl = v
        }

    var token: String?
        get() = SessionManager.token
        set(v) {
            SessionManager.token = v
        }

    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    val http: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val b = chain.request().newBuilder()
            SessionManager.token?.let { b.header("Authorization", "Bearer $it") }
            chain.proceed(b.build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    val api: VitaLinkApi by lazy {
        Retrofit.Builder()
            .baseUrl(SessionManager.baseUrl)
            .client(http)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(VitaLinkApi::class.java)
    }

    /** ws://.../websocket/app derived from the REST base URL. */
    fun wsUrl(): String = SessionManager.wsUrl()
}
