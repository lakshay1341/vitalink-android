package com.lakshay.vitalink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lakshay.vitalink.data.Alert
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.data.LatestVital
import com.lakshay.vitalink.data.News2Result
import com.lakshay.vitalink.data.Patient
import com.lakshay.vitalink.ui.theme.Bg
import com.lakshay.vitalink.ui.theme.HrGreen
import com.lakshay.vitalink.ui.theme.Spo2Cyan
import com.lakshay.vitalink.ui.theme.VitaLinkTheme
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

// Android Studio Compose previews — render each screen/component in isolation with
// fake clinical data (no backend). Open any file in the Split/Design view to see them.

private const val BG = 0xFF0B1120L

private fun demoEncounter(name: String, mrn: String, bed: String) = Encounter(
    id = 1,
    status = "ADMITTED",
    wardLabel = "A",
    bedLabel = bed,
    admittedAt = null,
    patient = Patient(1, mrn, name.substringBefore(' '), name.substringAfter(' ')),
)

private fun demoNews2(total: Int, risk: String, breakdown: Map<String, Int>, complete: Boolean = true) =
    News2Result(total, breakdown, breakdown.values.any { it == 3 }, risk, complete)

private fun demoVital(type: String, value: Double, unit: String) =
    LatestVital(type, value, unit, "2026-07-12T10:42:00Z")

// A physiological-ish ECG stub so the waveform preview shows real PQRST beats.
private fun demoEcg(n: Int): DoubleArray = DoubleArray(n) { i ->
    val t = i / 25.0
    val phase = t - t.roundToInt().toDouble()
    1.1 * exp(-(phase * phase) / (2 * 0.01 * 0.01)) +
        0.35 * exp(-((phase - 0.16) * (phase - 0.16)) / (2 * 0.04 * 0.04)) +
        0.05 * sin(t)
}

@Composable
private fun Frame(content: @Composable () -> Unit) {
    VitaLinkTheme {
        Box(Modifier.background(Bg).padding(16.dp)) { content() }
    }
}

@Preview(name = "Login", showBackground = true, backgroundColor = BG, widthDp = 360, heightDp = 760)
@Composable
private fun LoginPreview() {
    VitaLinkTheme { LoginContent(LoginUiState(), {}, {}, {}, {}, {}) }
}

@Preview(name = "Ward — empty", showBackground = true, backgroundColor = BG, widthDp = 360, heightDp = 520)
@Composable
private fun EmptyWardPreview() {
    Frame { EmptyWard(onRefresh = {}) }
}

@Preview(name = "Patient card — normal", showBackground = true, backgroundColor = BG, widthDp = 360)
@Composable
private fun PatientCardNormalPreview() {
    Frame {
        PatientCard(
            demoEncounter("Eleanor Shaw", "4471", "12"),
            demoNews2(2, "LOW", mapOf("respiratoryRate" to 0, "spo2" to 1, "heartRate" to 1)),
            listOf(
                demoVital("HEART_RATE", 78.0, "bpm"),
                demoVital("SPO2", 97.0, "%"),
                demoVital("RESPIRATORY_RATE", 16.0, "/min"),
            ),
        ) {}
    }
}

@Preview(name = "Patient card — critical", showBackground = true, backgroundColor = BG, widthDp = 360)
@Composable
private fun PatientCardCriticalPreview() {
    Frame {
        PatientCard(
            demoEncounter("Sarah Chen", "1205", "08"),
            demoNews2(8, "HIGH", mapOf("respiratoryRate" to 3, "spo2" to 3, "heartRate" to 2)),
            listOf(
                demoVital("HEART_RATE", 125.0, "bpm"),
                demoVital("SPO2", 88.0, "%"),
                demoVital("RESPIRATORY_RATE", 28.0, "/min"),
            ),
        ) {}
    }
}

@Preview(name = "NEWS2 badges", showBackground = true, backgroundColor = BG, widthDp = 320)
@Composable
private fun News2BadgePreview() {
    Frame {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            News2Badge(demoNews2(2, "LOW", mapOf("spo2" to 1)))
            News2Badge(demoNews2(5, "MEDIUM", mapOf("spo2" to 2, "respiratoryRate" to 2)))
            News2Badge(demoNews2(8, "HIGH", mapOf("spo2" to 3, "respiratoryRate" to 3)))
        }
    }
}

@Preview(name = "Vital tiles", showBackground = true, backgroundColor = BG, widthDp = 360)
@Composable
private fun VitalTilesPreview() {
    Frame {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                VitalTile("HR", 125.0, "bpm", HrGreen, 2, Modifier.weight(1f))
                VitalTile("SpO₂", 88.0, "%", Spo2Cyan, 3, Modifier.weight(1f))
            }
            BpTile(148.0, 95.0, 1)
        }
    }
}

@Preview(name = "NEWS2 panel", showBackground = true, backgroundColor = BG, widthDp = 360)
@Composable
private fun News2PanelPreview() {
    Frame {
        News2Panel(demoNews2(8, "HIGH", mapOf("respiratoryRate" to 3, "spo2" to 3, "heartRate" to 2)))
    }
}

@Preview(name = "Alarm rows", showBackground = true, backgroundColor = BG, widthDp = 360)
@Composable
private fun AlarmRowPreview() {
    Frame {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AlarmRow(Alert(1, "NEW", "CRITICAL", "SpO2 low", "2026-07-12T10:42:00Z"))
            AlarmRow(Alert(2, "NEW", "MEDIUM", "High respiratory rate", "2026-07-12T10:44:00Z"))
        }
    }
}

@Preview(name = "ECG waveform", showBackground = true, backgroundColor = BG, widthDp = 360, heightDp = 170)
@Composable
private fun WaveformPreview() {
    Frame { WaveformView(demoEcg(500), HrGreen, Modifier.fillMaxWidth().height(130.dp)) }
}
