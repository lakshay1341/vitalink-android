package com.lakshay.vitalink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.data.LatestVital
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
import com.lakshay.vitalink.ui.theme.Surface as SurfaceColor

@Composable
fun DashboardScreen(
    onOpen: (Encounter) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardContent(state = state, onRefresh = viewModel::load, onOpen = onOpen)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardContent(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onOpen: (Encounter) -> Unit,
) {
    val rows = (state as? DashboardUiState.Content)?.rows.orEmpty()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VitaLink", fontWeight = FontWeight.Bold) },
                actions = {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Primary))
                    Spacer(Modifier.width(6.dp))
                    Text("Connected", color = OnMuted, fontSize = 12.sp)
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Refresh", tint = OnMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg, titleContentColor = BpWhite),
            )
        },
        containerColor = Bg,
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Text(
                "Ward · ${rows.size} monitored",
                color = OnMuted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (state) {
                is DashboardUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
                is DashboardUiState.Error -> Text("Error: ${state.message}", color = RiskHigh, modifier = Modifier.padding(16.dp))
                is DashboardUiState.Content ->
                    if (rows.isEmpty()) {
                        EmptyWard(onRefresh = onRefresh)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(rows, key = { it.encounter.id }) { row ->
                                PatientCard(row.encounter, row.news2, row.vitals) { onOpen(row.encounter) }
                            }
                        }
                    }
            }
        }
    }
}

@Composable
internal fun EmptyWard(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = Primary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text("No patients monitored", color = BpWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Admit an encounter and bind a device to start streaming vitals.",
            color = OnMuted, fontSize = 13.sp,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onRefresh) { Text("Refresh") }
    }
}

@Composable
internal fun PatientCard(enc: Encounter, news2: News2Result?, vitals: List<LatestVital>?, onClick: () -> Unit) {
    val name = listOfNotNull(enc.patient?.firstName, enc.patient?.lastName).joinToString(" ").ifBlank { "Unknown" }
    val bed = listOfNotNull(enc.wardLabel, enc.bedLabel).joinToString("-").ifBlank { "—" }
    val critical = news2?.risk == "HIGH"
    val map = vitals?.associateBy { it.type } ?: emptyMap()
    Surface(
        color = SurfaceColor, shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            if (critical) Box(Modifier.width(4.dp).fillMaxHeight().background(RiskHigh))
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = BpWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("MRN ${enc.patient?.mrn ?: "—"} · Bed $bed", color = OnMuted, fontSize = 13.sp)
                    }
                    News2Badge(news2)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Mini("HR", map["HEART_RATE"]?.value, HrGreen)
                    Mini("SpO₂", map["SPO2"]?.value, Spo2Cyan)
                    Mini("RR", map["RESPIRATORY_RATE"]?.value, RespYellow)
                }
            }
        }
    }
}

@Composable
private fun Mini(label: String, value: Double?, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("$label ", color = OnMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
        Text(value?.let { it.toInt().toString() } ?: "--", color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun News2Badge(news2: News2Result?) {
    val color = when (news2?.risk) {
        "HIGH" -> RiskHigh
        "MEDIUM" -> RiskMedium
        "LOW_MEDIUM" -> RiskMedium
        "LOW" -> RiskLow
        else -> OnMuted
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.clip(RoundedCornerShape(8.dp)).background(color).padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(news2?.total?.toString() ?: "–", color = Bg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text("NEWS2", color = OnMuted, fontSize = 10.sp)
    }
}
