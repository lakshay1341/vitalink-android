package com.lakshay.vitalink.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lakshay.vitalink.data.AlertRule
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.ui.theme.Bg
import com.lakshay.vitalink.ui.theme.BpWhite
import com.lakshay.vitalink.ui.theme.OnMuted
import com.lakshay.vitalink.ui.theme.Primary
import com.lakshay.vitalink.ui.theme.RiskHigh
import com.lakshay.vitalink.ui.theme.Surface as SurfaceColor

private val SEVERITIES = listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmThresholdsScreen(
    enc: Encounter,
    onBack: () -> Unit,
    viewModel: AlarmThresholdsViewModel = hiltViewModel(),
) {
    LaunchedEffect(enc.id) { viewModel.start(enc.id) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val name = listOfNotNull(enc.patient?.firstName, enc.patient?.lastName).joinToString(" ").ifBlank { "Patient" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Alarm Thresholds", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                is ThresholdsUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
                is ThresholdsUiState.Error -> Text("Error: ${s.message}", color = RiskHigh, modifier = Modifier.padding(16.dp))
                is ThresholdsUiState.Content -> {
                    s.message?.let {
                        Text(it, color = Primary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    if (s.rules.isEmpty()) {
                        Text(
                            "No encounter-specific alarm rules.",
                            color = OnMuted, fontSize = 13.sp, modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(s.rules, key = { it.id }) { rule ->
                                RuleCard(rule, saving = s.savingId == rule.id, onSave = viewModel::save)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleCard(rule: AlertRule, saving: Boolean, onSave: (AlertRule) -> Unit) {
    var min by remember(rule.id) { mutableStateOf(rule.minValue?.let { numText(it) } ?: "") }
    var max by remember(rule.id) { mutableStateOf(rule.maxValue?.let { numText(it) } ?: "") }
    var sev by remember(rule.id) { mutableStateOf(rule.severity) }
    var enabled by remember(rule.id) { mutableStateOf(rule.enabled) }
    var expanded by remember { mutableStateOf(false) }

    Surface(color = SurfaceColor, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(rule.vitalType, color = BpWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = min, onValueChange = { min = it },
                    label = { Text("Min") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = max, onValueChange = { max = it },
                    label = { Text("Max") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Severity", color = OnMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(sev) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        SEVERITIES.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { sev = option; expanded = false })
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled", color = OnMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onSave(
                        rule.copy(
                            minValue = min.toDoubleOrNull(),
                            maxValue = max.toDoubleOrNull(),
                            severity = sev,
                            enabled = enabled,
                        ),
                    )
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Bg)
                } else {
                    Text("Save")
                }
            }
        }
    }
}

private fun numText(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
