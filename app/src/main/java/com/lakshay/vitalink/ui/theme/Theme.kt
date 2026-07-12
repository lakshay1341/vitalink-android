package com.lakshay.vitalink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ExperimentalTextApi
import com.lakshay.vitalink.R

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

// Inter is the design-system typeface. One variable font drives every weight via
// FontVariation (minSdk 26+ supports variable fonts), so the app matches the Stitch
// mockups instead of falling back to the platform default.
@OptIn(ExperimentalTextApi::class)
private fun interWeight(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Inter = FontFamily(
    interWeight(400),
    interWeight(500),
    interWeight(600),
    interWeight(700),
)

// Apply Inter to every text style in the default Material 3 type scale.
private val AppTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = Inter),
        displayMedium = displayMedium.copy(fontFamily = Inter),
        displaySmall = displaySmall.copy(fontFamily = Inter),
        headlineLarge = headlineLarge.copy(fontFamily = Inter),
        headlineMedium = headlineMedium.copy(fontFamily = Inter),
        headlineSmall = headlineSmall.copy(fontFamily = Inter),
        titleLarge = titleLarge.copy(fontFamily = Inter),
        titleMedium = titleMedium.copy(fontFamily = Inter),
        titleSmall = titleSmall.copy(fontFamily = Inter),
        bodyLarge = bodyLarge.copy(fontFamily = Inter),
        bodyMedium = bodyMedium.copy(fontFamily = Inter),
        bodySmall = bodySmall.copy(fontFamily = Inter),
        labelLarge = labelLarge.copy(fontFamily = Inter),
        labelMedium = labelMedium.copy(fontFamily = Inter),
        labelSmall = labelSmall.copy(fontFamily = Inter),
    )
}

@Composable
fun VitaLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, typography = AppTypography) {
        // Bare Text() calls default to LocalTextStyle (TextStyle.Default = platform font), not
        // the typography scale — so provide an Inter base style here to make the whole app Inter.
        // Explicit fontSize/weight/colour on individual Text() calls still win via style merge.
        ProvideTextStyle(value = AppTypography.bodyMedium) {
            content()
        }
    }
}
