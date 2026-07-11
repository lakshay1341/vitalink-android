package com.lakshay.vitalink.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draws one ECG frame as a single bright polyline on a faint grid, auto-scaled to fit.
 * ponytail: hand-rolled on Canvas — no chart dependency, and it is exactly the
 * bedside-monitor look we want.
 */
@Composable
fun WaveformView(values: DoubleArray, trace: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val grid = Color(0x22FFFFFF)
        val cols = 8
        val rows = 4
        for (i in 0..cols) {
            val x = w * i / cols
            drawLine(grid, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }
        for (i in 0..rows) {
            val y = h * i / rows
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        if (values.size < 2) return@Canvas
        var min = values[0]
        var max = values[0]
        for (v in values) {
            if (v < min) min = v
            if (v > max) max = v
        }
        val range = (max - min).let { if (it < 1e-6) 1.0 else it }
        val pad = h * 0.1f
        val path = Path()
        for (i in values.indices) {
            val x = w * i / (values.size - 1)
            val norm = ((values[i] - min) / range).toFloat()
            val y = h - pad - norm * (h - 2 * pad)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, trace, style = Stroke(width = 2.5f))
    }
}
