package com.lakshay.vitalink.di

import com.lakshay.vitalink.data.SessionManager
import com.lakshay.vitalink.data.VitaLinkApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

/**
 * Provides the network stack. The base URL is dynamic (entered at login), so Retrofit
 * is built with a placeholder and an interceptor rewrites each request's scheme/host/port
 * from SessionManager — this also fixes the old "baseUrl captured at first use" limitation.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun moshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request()
            val configured = SessionManager.baseUrl.toHttpUrlOrNull()
            val url = if (configured != null) {
                req.url.newBuilder()
                    .scheme(configured.scheme)
                    .host(configured.host)
                    .port(configured.port)
                    .build()
            } else {
                req.url
            }
            val b = req.newBuilder().url(url)
            SessionManager.token?.let { b.header("Authorization", "Bearer $it") }
            chain.proceed(b.build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl("http://localhost:8080/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun api(retrofit: Retrofit): VitaLinkApi = retrofit.create(VitaLinkApi::class.java)
}
