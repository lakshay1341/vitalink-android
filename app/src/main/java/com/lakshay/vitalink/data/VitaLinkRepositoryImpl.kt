package com.lakshay.vitalink.data

import com.lakshay.vitalink.data.local.EncounterDao
import com.lakshay.vitalink.data.local.VitalDao
import com.lakshay.vitalink.data.local.toDomain
import com.lakshay.vitalink.data.local.toEntity
import com.lakshay.vitalink.domain.VitaLinkRepository
import com.lakshay.vitalink.net.WaveformStomp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default repository over the Retrofit API + STOMP waveform stream, with a Room cache
 * for encounters and latest vitals: successful fetches write through to the DB, and on
 * network failure the cached copy is returned so the dashboard/monitor aren't blank
 * offline. runCatching keeps the ViewModels try/catch-free.
 * The live waveform is provided by an injected WaveformStomp (its own OkHttp + auth interceptor).
 */
@Singleton
class VitaLinkRepositoryImpl @Inject constructor(
    private val api: VitaLinkApi,
    private val encounterDao: EncounterDao,
    private val vitalDao: VitalDao,
    private val waveformStomp: WaveformStomp,
) : VitaLinkRepository {

    override suspend fun login(serverUrl: String, username: String, password: String): Result<Unit> = runCatching {
        SessionManager.baseUrl = serverUrl.trim().trimEnd('/') + "/"
        val jwt = api.authenticate(LoginRequest(username.trim(), password))
        SessionManager.token = jwt.idToken
    }

    override suspend fun encounters(): Result<List<Encounter>> =
        runCatching { api.encounters() }
            .onSuccess { list -> runCatching { encounterDao.replaceAll(list.map { it.toEntity() }) } }
            .recoverCatching { e ->
                val cached = encounterDao.getAll().map { it.toDomain() }
                if (cached.isEmpty()) throw e else cached
            }

    override suspend fun news2(encounterId: Long): Result<News2Result> = runCatching { api.news2(encounterId) }

    override suspend fun latestVitals(encounterId: Long): Result<List<LatestVital>> =
        runCatching { api.latestVitals(encounterId) }
            .onSuccess { list -> runCatching { vitalDao.upsertAll(list.map { it.toEntity(encounterId) }) } }
            .recoverCatching { e ->
                val cached = vitalDao.getForEncounter(encounterId).map { it.toDomain() }
                if (cached.isEmpty()) throw e else cached
            }

    override suspend fun alerts(encounterId: Long, size: Int): Result<List<Alert>> =
        runCatching { api.alerts(encounterId, size = size) }

    override suspend fun alertRules(encounterId: Long): Result<List<AlertRule>> =
        runCatching { api.alertRules(encounterId) }

    override suspend fun updateAlertRule(rule: AlertRule): Result<AlertRule> =
        runCatching { api.updateAlertRule(rule.id, rule) }

    override fun waveform(encounterId: Long): Flow<WaveformFrame> = waveformStomp.frames(encounterId)
}
