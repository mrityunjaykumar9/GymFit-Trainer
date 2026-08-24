package com.freegymtrainer.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * A dependency-free line chart (Canvas-based) for weight/volume/strength trends.
 * Kept intentionally simple for the MVP - swap for Vico/MPAndroidChart later if richer
 * interactions (tooltips, zoom) are needed.
 */
@Composable
fun SimpleLineChart(
    values: List<Float>,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    unitLabel: String = ""
) {
    val lineColor = MaterialTheme.colorScheme.primary
    Column(modifier) {
        if (values.size < 2) {
            Text("Log a few entries to see your trend here.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(8.dp)
        ) {
            val minVal = values.min()
            val maxVal = values.max()
            val range = (maxVal - minVal).takeIf { it > 0f } ?: 1f
            val stepX = size.width / (values.size - 1)

            val points = values.mapIndexed { index, v ->
                val x = index * stepX
                val y = size.height - ((v - minVal) / range) * size.height
                Offset(x, y)
            }

            for (i in 0 until points.size - 1) {
                drawLine(
                    color = lineColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
            points.forEach { p ->
                drawCircle(color = lineColor, radius = 8f, center = p)
            }
        }
        if (values.isNotEmpty()) {
            Text(
                "${values.first()} $unitLabel  →  ${values.last()} $unitLabel",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
