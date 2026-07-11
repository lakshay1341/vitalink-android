package com.lakshay.vitalink.data

import com.squareup.moshi.Json

// Wire models — plain data classes decoded by Moshi's reflective Kotlin adapter.

data class LoginRequest(val username: String, val password: String, val rememberMe: Boolean = true)

data class JwtResponse(@Json(name = "id_token") val idToken: String)

data class Patient(val id: Long?, val mrn: String?, val firstName: String?, val lastName: String?)

data class Encounter(
    val id: Long,
    val status: String?,
    val wardLabel: String?,
    val bedLabel: String?,
    val admittedAt: String?,
    val patient: Patient?,
)

data class News2Result(
    val total: Int,
    val breakdown: Map<String, Int>?,
    val anyParamScored3: Boolean,
    val risk: String?,
    val complete: Boolean,
)

data class LatestVital(val type: String, val value: Double?, val unit: String?, val measuredAt: String?)

data class Alert(
    val id: Long,
    val status: String?,
    val severity: String?,
    val message: String?,
    val triggeredAt: String?,
)

data class WaveformFrame(
    val encounterId: Long?,
    val channel: String?,
    val tMillis: DoubleArray,
    val values: DoubleArray,
)
