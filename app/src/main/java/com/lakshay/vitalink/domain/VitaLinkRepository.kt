package com.lakshay.vitalink.domain

import com.lakshay.vitalink.data.Alert
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.data.LatestVital
import com.lakshay.vitalink.data.News2Result
import com.lakshay.vitalink.data.WaveformFrame
import kotlinx.coroutines.flow.Flow

/**
 * Single seam between the UI (ViewModels) and the network. Suspend calls return
 * Result<T> so callers map success/failure into UI state without try/catch; the live
 * waveform is a cold Flow that starts the STOMP subscription when collected.
 */
interface VitaLinkRepository {
    /** Sets the server URL + authenticates; on success the JWT is stored for later calls. */
    suspend fun login(serverUrl: String, username: String, password: String): Result<Unit>

    suspend fun encounters(): Result<List<Encounter>>

    suspend fun news2(encounterId: Long): Result<News2Result>

    suspend fun latestVitals(encounterId: Long): Result<List<LatestVital>>

    suspend fun alerts(encounterId: Long, size: Int = 20): Result<List<Alert>>

    fun waveform(encounterId: Long): Flow<WaveformFrame>
}
