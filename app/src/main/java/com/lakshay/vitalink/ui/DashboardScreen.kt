package com.lakshay.vitalink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakshay.vitalink.data.Backend
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.data.News2Result
import com.lakshay.vitalink.ui.theme.Bg
import com.lakshay.vitalink.ui.theme.BpWhite
import com.lakshay.vitalink.ui.theme.OnMuted
import com.lakshay.vitalink.ui.theme.Primary
import com.lakshay.vitalink.ui.theme.RiskHigh
import com.lakshay.vitalink.ui.theme.RiskLow
import com.lakshay.vitalink.ui.theme.RiskMedium
import com.lakshay.vitalink.ui.theme.Surface as SurfaceColor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val ACTIVE = setOf("PRE_ADMIT", "ADMITTED", "DISCHARGE_PENDING")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onOpen: (Encounter) -> Unit) {
    var encounters by remember { mutableStateOf<List<Encounter>>(emptyList()) }
    var scores by remember { mutableStateOf<Map<Long, News2Result>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val list = Backend.api.encounters().filter { it.status in ACTIVE }
            encounters = list
            val map = mutableMapOf<Long, News2Result>()
            coroutineScope {
                list.map { enc -> async { enc.id to runCatching { Backend.api.news2(enc.id) }.getOrNull() } }
                    .forEach { d ->
                        val (id, r) = d.await()
                        if (r != null) map[id] = r
                    }
            }
            scores = map
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VitaLink", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg, titleContentColor = BpWhite),
            )
        },
        containerColor = Bg,
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Text(
                "Ward · ${encounters.size} monitored",
                color = OnMuted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
            error?.let { Text("Error: $it", color = RiskHigh, modifier = Modifier.padding(16.dp)) }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(encounters, key = { it.id }) { enc ->
                    PatientCard(enc, scores[enc.id]) { onOpen(enc) }
                }
            }
        }
    }
}

@Composable
private fun PatientCard(enc: Encounter, news2: News2Result?, onClick: () -> Unit) {
    val name = listOfNotNull(enc.patient?.firstName, enc.patient?.lastName).joinToString(" ").ifBlank { "Unknown" }
    val bed = listOfNotNull(enc.wardLabel, enc.bedLabel).joinToString("-").ifBlank { "—" }
    Surface(
        color = SurfaceColor, shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, color = BpWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("MRN ${enc.patient?.mrn ?: "—"} · Bed $bed", color = OnMuted, fontSize = 13.sp)
            }
            News2Badge(news2)
        }
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
