package com.lakshay.vitalink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Clinical dark palette (from the Stitch "VitaLink Clinical Dark" design system).
val Bg = Color(0xFF0B1120)
val Surface = Color(0xFF1E293B)
val Primary = Color(0xFF2DD4BF)
val OnMuted = Color(0xFF94A3B8)

// Clinical vital colours (medical convention).
val HrGreen = Color(0xFF34D399)
val Spo2Cyan = Color(0xFF22D3EE)
val RespYellow = Color(0xFFFDE047)
val BpWhite = Color(0xFFE2E8F0)
val TempOrange = Color(0xFFFB923C)

// NEWS2 / alarm severity.
val RiskLow = Color(0xFF34D399)
val RiskMedium = Color(0xFFF59E0B)
val RiskHigh = Color(0xFFEF4444)

private val DarkColors = darkColorScheme(
    primary = Primary,
    background = Bg,
    surface = Surface,
    onPrimary = Bg,
    onBackground = BpWhite,
    onSurface = BpWhite,
)

@Composable
fun VitaLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
