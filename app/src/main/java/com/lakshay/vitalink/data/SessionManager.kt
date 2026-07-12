package com.lakshay.vitalink.data

/**
 * Single source of truth for the backend base URL and JWT, both set at login.
 * A plain object so it can be shared by the Hilt-provided network stack and any
 * remaining static callers during the DI migration.
 */
object SessionManager {
    @Volatile
    var baseUrl: String = "http://10.0.2.2:8080/"

    @Volatile
    var token: String? = null

    /** ws://.../websocket/app derived from the REST base URL. */
    fun wsUrl(): String = baseUrl.replaceFirst("http", "ws").trimEnd('/') + "/websocket/app"
}
