package com.lakshay.vitalink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lakshay.vitalink.data.Alert
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.data.News2Result
import com.lakshay.vitalink.ui.theme.Bg
import com.lakshay.vitalink.ui.theme.BpWhite
import com.lakshay.vitalink.ui.theme.HrGreen
import com.lakshay.vitalink.ui.theme.OnMuted
import com.lakshay.vitalink.ui.theme.Primary
import com.lakshay.vitalink.ui.theme.RespYellow
import com.lakshay.vitalink.ui.theme.RiskHigh
import com.lakshay.vitalink.ui.theme.RiskLow
import com.lakshay.vitalink.ui.theme.RiskMedium
import com.lakshay.vitalink.ui.theme.Spo2Cyan
import com.lakshay.vitalink.ui.theme.TempOrange
import com.lakshay.vitalink.ui.theme.Surface as SurfaceColor

@Composable
fun MonitorScreen(
    enc: Encounter,
    onBack: () -> Unit,
    viewModel: MonitorViewModel = hiltViewModel(),
) {
    LaunchedEffect(enc.id) { viewModel.start(enc.id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    MonitorContent(enc = enc, state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonitorContent(enc: Encounter, state: MonitorUiState, onBack: () -> Unit) {
    val name = listOfNotNull(enc.patient?.firstName, enc.patient?.lastName).joinToString(" ").ifBlank { "Patient" }
    val bed = listOfNotNull(enc.wardLabel, enc.bedLabel).joinToString("-")
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("MRN ${enc.patient?.mrn ?: "—"} · Bed $bed", color = OnMuted, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BpWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg, titleContentColor = BpWhite),
            )
        },
        containerColor = Bg,
    ) { pad ->
        val map = state.vitals.associateBy { it.type }
        val bd = state.news2?.breakdown ?: emptyMap()
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(color = Color(0xFF0F172A), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.fillMaxWidth().height(180.dp).padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ECG II · 250 Hz", color = OnMuted, fontSize = 11.sp)
                            Text(
                                if (state.streaming) "● Streaming" else "○ Waiting",
                                color = if (state.streaming) Primary else OnMuted, fontSize = 11.sp,
                            )
                        }
                        WaveformView(state.wave, HrGreen, Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    VitalTile("HR", map["HEART_RATE"]?.value, "bpm", HrGreen, bd["heartRate"], Modifier.weight(1f))
                    VitalTile("SpO₂", map["SPO2"]?.value, "%", Spo2Cyan, bd["spo2"], Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    VitalTile("RESP", map["RESPIRATORY_RATE"]?.value, "/min", RespYellow, bd["respiratoryRate"], Modifier.weight(1f))
                    VitalTile("TEMP", map["BODY_TEMPERATURE"]?.value, "°C", TempOrange, bd["temperature"], Modifier.weight(1f))
                }
            }
            item {
                BpTile(map["BLOOD_PRESSURE_SYSTOLIC"]?.value, map["BLOOD_PRESSURE_DIASTOLIC"]?.value, bd["systolicBp"])
            }
            item { News2Panel(state.news2) }
            item {
                Text("Active Alarms", color = BpWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            if (state.alerts.isEmpty()) {
                item { Text("No active alarms", color = OnMuted, fontSize = 13.sp) }
            } else {
                items(state.alerts, key = { it.id }) { AlarmRow(it) }
            }
        }
    }
}

@Composable
internal fun VitalTile(label: String, value: Double?, unit: String, color: Color, score: Int?, modifier: Modifier = Modifier) {
    val flag = when {
        score == null -> null
        score >= 3 -> RiskHigh
        score >= 1 -> RiskMedium
        else -> null
    }
    Surface(
        color = SurfaceColor, shape = RoundedCornerShape(8.dp),
        modifier = modifier.then(if (flag != null) Modifier.border(1.5.dp, flag, RoundedCornerShape(8.dp)) else Modifier),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label.uppercase(), color = OnMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (flag != null) Box(Modifier.size(8.dp).clip(CircleShape).background(flag))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value?.let { fmt(it) } ?: "--", color = color, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text(unit, color = OnMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp))
            }
        }
    }
}

@Composable
internal fun BpTile(sys: Double?, dia: Double?, score: Int?) {
    val flag = when {
        score == null -> null
        score >= 3 -> RiskHigh
        score >= 1 -> RiskMedium
        else -> null
    }
    Surface(
        color = SurfaceColor, shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().then(if (flag != null) Modifier.border(1.5.dp, flag, RoundedCornerShape(8.dp)) else Modifier),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BLOOD PRESSURE", color = OnMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (flag != null) Box(Modifier.size(8.dp).clip(CircleShape).background(flag))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    (sys?.let { fmt(it) } ?: "--") + "/" + (dia?.let { fmt(it) } ?: "--"),
                    color = BpWhite, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(4.dp))
                Text("mmHg", color = OnMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp))
            }
        }
    }
}

@Composable
internal fun News2Panel(news2: News2Result?) {
    val color: Color
    val label: String
    when (news2?.risk) {
        "HIGH" -> { color = RiskHigh; label = "High risk" }
        "MEDIUM" -> { color = RiskMedium; label = "Medium risk" }
        "LOW_MEDIUM" -> { color = RiskMedium; label = "Low-medium risk" }
        "LOW" -> { color = RiskLow; label = "Low risk" }
        else -> { color = OnMuted; label = "—" }
    }
    val scored = news2?.breakdown?.filterValues { it > 0 } ?: emptyMap()
    Surface(color = SurfaceColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(color).padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(news2?.total?.toString() ?: "–", color = Bg, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("NEWS2", color = BpWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        label + if (news2?.complete == false) " · incomplete" else "",
                        color = OnMuted, fontSize = 13.sp,
                    )
                }
            }
            if (scored.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    scored.entries.forEach { (k, v) ->
                        Text(
                            "${shortParam(k)} $v",
                            color = if (v >= 3) RiskHigh else RiskMedium,
                            fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AlarmRow(a: Alert) {
    val color = when (a.severity) {
        "CRITICAL", "HIGH" -> RiskHigh
        "MEDIUM" -> RiskMedium
        else -> RespYellow
    }
    Surface(color = SurfaceColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(color))
            Column(Modifier.padding(14.dp)) {
                Text(a.message ?: (a.severity ?: "Alarm"), color = BpWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("${a.severity ?: ""} · ${shortTime(a.triggeredAt)}", color = OnMuted, fontSize = 12.sp)
            }
        }
    }
}

private fun shortParam(k: String): String = when (k) {
    "respiratoryRate" -> "RR"
    "spo2" -> "SpO₂"
    "oxygen" -> "O₂"
    "systolicBp" -> "BP"
    "heartRate" -> "HR"
    "consciousness" -> "ACVPU"
    "temperature" -> "Temp"
    else -> k
}

private fun fmt(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

private fun shortTime(iso: String?): String {
    if (iso == null) return ""
    val t = iso.substringAfter('T', "")
    return if (t.length >= 5) t.substring(0, 5) else iso
}
