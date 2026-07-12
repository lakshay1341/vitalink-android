package com.lakshay.vitalink.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lakshay.vitalink.data.Alert
import com.lakshay.vitalink.data.LatestVital
import com.lakshay.vitalink.data.News2Result
import com.lakshay.vitalink.domain.VitaLinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A technical alarm (IEC 60601-1-8): a device/sensor condition, not a patient vital. */
data class TechnicalAlarm(val label: String, val detail: String)

/**
 * Live snapshot of one patient's monitor. Not a sealed Loading/Content/Error — the monitor
 * fills in progressively (waveform streams, vitals poll), so a single mutable snapshot is the
 * honest model.
 * ponytail: DoubleArray in a data class trips the usual array-equals warning; harmless here —
 * each frame is a fresh array so StateFlow always emits the new waveform.
 */
data class MonitorUiState(
    val wave: DoubleArray = DoubleArray(0),
    val streaming: Boolean = false,
    val vitals: List<LatestVital> = emptyList(),
    val news2: News2Result? = null,
    val alerts: List<Alert> = emptyList(),
    val technical: List<TechnicalAlarm> = emptyList(),
)

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val repo: VitaLinkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MonitorUiState())
    val state: StateFlow<MonitorUiState> = _state.asStateFlow()

    private var started = false

    @Volatile
    private var lastFrameAt = 0L

    /**
     * Idempotent: starts the waveform subscription + 5s vitals/news2/alerts poll.
     * Backend alerts are all rule breaches, so they are physiological. Technical alarms
     * (IEC 60601-1-8) are synthesized from what the client observes: no waveform for
     * SIGNAL_TIMEOUT_MS means signal loss / lead off.
     * ponytail: single technical check (ECG signal). Add battery / sensor-off technical
     * alarms when the device reports status (e.g. via the MQTT last-will path).
     */
    fun start(encounterId: Long) {
        if (started) return
        started = true
        lastFrameAt = System.currentTimeMillis()

        viewModelScope.launch {
            repo.waveform(encounterId).collect { frame ->
                lastFrameAt = System.currentTimeMillis()
                _state.update { it.copy(wave = frame.values, streaming = true, technical = emptyList()) }
            }
        }

        viewModelScope.launch {
            while (true) {
                val v = repo.latestVitals(encounterId).getOrNull()
                val n = repo.news2(encounterId).getOrNull()
                val a = repo.alerts(encounterId).getOrNull()?.filter { it.status != "RESOLVED" }
                val stale = System.currentTimeMillis() - lastFrameAt > SIGNAL_TIMEOUT_MS
                val tech = if (stale) {
                    listOf(TechnicalAlarm("ECG signal loss", "No waveform received — check lead / sensor"))
                } else {
                    emptyList()
                }
                _state.update { cur ->
                    cur.copy(
                        vitals = v ?: cur.vitals,
                        news2 = n ?: cur.news2,
                        alerts = a ?: cur.alerts,
                        streaming = !stale,
                        technical = tech,
                    )
                }
                delay(5000)
            }
        }
    }

    companion object {
        private const val SIGNAL_TIMEOUT_MS = 8000L
    }
}
