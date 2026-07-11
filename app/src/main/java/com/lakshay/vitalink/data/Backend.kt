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
}

/**
 * App-wide backend holder. Base URL and JWT are set at login; no DI framework — a
 * three-screen demo doesn't need one.
 * ponytail: the Retrofit instance is built lazily and captures baseUrl at first use,
 * so changing the server after the first request has no effect until app restart.
 */
object Backend {
    @Volatile var baseUrl: String = "http://10.0.2.2:8080/"
    @Volatile var token: String? = null

    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    val http: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val b = chain.request().newBuilder()
            token?.let { b.header("Authorization", "Bearer $it") }
            chain.proceed(b.build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    val api: VitaLinkApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(http)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(VitaLinkApi::class.java)
    }

    /** ws://.../websocket/app derived from the REST base URL. */
    fun wsUrl(): String = baseUrl.replaceFirst("http", "ws").trimEnd('/') + "/websocket/app"
}
