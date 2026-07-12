package com.lakshay.vitalink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lakshay.vitalink.data.Alert
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.ui.theme.Bg
import com.lakshay.vitalink.ui.theme.BpWhite
import com.lakshay.vitalink.ui.theme.OnMuted
import com.lakshay.vitalink.ui.theme.Primary
import com.lakshay.vitalink.ui.theme.RespYellow
import com.lakshay.vitalink.ui.theme.RiskHigh
import com.lakshay.vitalink.ui.theme.RiskMedium
import com.lakshay.vitalink.ui.theme.Surface as SurfaceColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmHistoryScreen(
    enc: Encounter,
    onBack: () -> Unit,
    viewModel: AlarmHistoryViewModel = hiltViewModel(),
) {
    LaunchedEffect(enc.id) { viewModel.start(enc.id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val name = listOfNotNull(enc.patient?.firstName, enc.patient?.lastName).joinToString(" ").ifBlank { "Patient" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Alarm History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("$name · MRN ${enc.patient?.mrn ?: "—"}", color = OnMuted, fontSize = 12.sp)
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
        Column(Modifier.padding(pad).fillMaxSize()) {
            when (val s = state) {
                is HistoryUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
                is HistoryUiState.Error -> Text("Error: ${s.message}", color = RiskHigh, modifier = Modifier.padding(16.dp))
                is HistoryUiState.Content ->
                    if (s.alerts.isEmpty()) {
                        Text("No alarms recorded.", color = OnMuted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(s.alerts, key = { it.id }) { HistoryRow(it) }
                        }
                    }
            }
        }
    }
}

@Composable
private fun HistoryRow(a: Alert) {
    val resolved = a.status == "RESOLVED"
    val bar = if (resolved) {
        OnMuted
    } else {
        when (a.severity) {
            "CRITICAL", "HIGH" -> RiskHigh
            "MEDIUM" -> RiskMedium
            else -> RespYellow
        }
    }
    Surface(color = SurfaceColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(bar))
            Column(Modifier.padding(14.dp)) {
                Text(
                    a.message ?: (a.severity ?: "Alarm"),
                    color = if (resolved) OnMuted else BpWhite,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                )
                Text(
                    listOfNotNull(a.severity, a.status, shortTime(a.triggeredAt)).joinToString(" · "),
                    color = OnMuted, fontSize = 12.sp,
                )
            }
        }
    }
}

private fun shortTime(iso: String?): String {
    if (iso == null) return ""
    val t = iso.substringAfter('T', "")
    return if (t.length >= 5) t.substring(0, 5) else iso
}
