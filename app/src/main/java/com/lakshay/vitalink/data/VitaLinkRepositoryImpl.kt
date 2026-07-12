package com.lakshay.vitalink.data

import com.lakshay.vitalink.domain.VitaLinkRepository
import com.lakshay.vitalink.net.WaveformStomp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default repository over the Retrofit API + STOMP waveform stream. runCatching turns
 * network/serialization throwables into Result.failure so the ViewModels stay try/catch-free.
 * ponytail: waveform still delegates to the static WaveformStomp (its OkHttp stays on the
 * legacy Backend shim); inject the client into WaveformStomp when Backend is finally deleted.
 */
@Singleton
class VitaLinkRepositoryImpl @Inject constructor(
    private val api: VitaLinkApi,
) : VitaLinkRepository {

    override suspend fun login(serverUrl: String, username: String, password: String): Result<Unit> = runCatching {
        SessionManager.baseUrl = serverUrl.trim().trimEnd('/') + "/"
        val jwt = api.authenticate(LoginRequest(username.trim(), password))
        SessionManager.token = jwt.idToken
    }

    override suspend fun encounters(): Result<List<Encounter>> = runCatching { api.encounters() }

    override suspend fun news2(encounterId: Long): Result<News2Result> = runCatching { api.news2(encounterId) }

    override suspend fun latestVitals(encounterId: Long): Result<List<LatestVital>> =
        runCatching { api.latestVitals(encounterId) }

    override suspend fun alerts(encounterId: Long, size: Int): Result<List<Alert>> =
        runCatching { api.alerts(encounterId, size = size) }

    override suspend fun alertRules(encounterId: Long): Result<List<AlertRule>> =
        runCatching { api.alertRules().filter { it.encounter?.id == encounterId } }

    override suspend fun updateAlertRule(rule: AlertRule): Result<AlertRule> =
        runCatching { api.updateAlertRule(rule.id, rule) }

    override fun waveform(encounterId: Long): Flow<WaveformFrame> = WaveformStomp.frames(encounterId)
}
