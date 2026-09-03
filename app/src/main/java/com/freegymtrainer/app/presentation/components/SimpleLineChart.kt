package com.freegymtrainer.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * A dependency-free line chart (Canvas-based) for weight/volume/strength trends: a smoothed,
 * gradient-filled area line with a highlighted latest point, axis end-labels, and a trend
 * delta - the shape modern fitness-app charts (Strong, Apple Health, etc.) use, without
 * pulling in a full charting library.
 */
@Composable
fun SimpleLineChart(
    values: List<Float>,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    unitLabel: String = ""
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val haloColor = MaterialTheme.colorScheme.surface

    Column(modifier) {
        if (values.isEmpty()) {
            Text("Log a few entries to see your trend here.", style = MaterialTheme.typography.bodyMedium, color = mutedColor)
            return@Column
        }
        if (values.size == 1) {
            Text("${formatChartValue(values[0])} $unitLabel", style = MaterialTheme.typography.headlineSmall)
            Text("Log one more entry to see a trend.", style = MaterialTheme.typography.bodyMedium, color = mutedColor)
            return@Column
        }

        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).takeIf { it > 0f } ?: 1f

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val topPad = 16f
            val bottomPad = 16f
            val chartHeight = size.height - topPad - bottomPad
            val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f

            val points = values.mapIndexed { index, v ->
                val x = index * stepX
                val y = topPad + chartHeight - ((v - minVal) / range) * chartHeight
                Offset(x, y)
            }

            // Smoothed line: each intermediate point is used as the control point of a
            // quadratic curve ending at the midpoint to its next neighbor, so the curve flows
            // continuously midpoint-to-midpoint instead of joining as sharp straight segments.
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                if (points.size == 2) {
                    lineTo(points[1].x, points[1].y)
                } else {
                    for (i in 1 until points.size - 1) {
                        val mid = Offset((points[i].x + points[i + 1].x) / 2f, (points[i].y + points[i + 1].y) / 2f)
                        quadraticBezierTo(points[i].x, points[i].y, mid.x, mid.y)
                    }
                    lineTo(points.last().x, points.last().y)
                }
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0f)))
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Highlight the latest point so the chart reads as "here's where you are now".
            val last = points.last()
            drawCircle(color = haloColor, radius = 11f, center = last)
            drawCircle(color = lineColor, radius = 7f, center = last)
        }

        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(labels.first(), style = MaterialTheme.typography.bodySmall, color = mutedColor)
                Text(labels.last(), style = MaterialTheme.typography.bodySmall, color = mutedColor)
            }
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(4.dp))
        }

        val latest = values.last()
        val delta = latest - values.first()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${formatChartValue(latest)} $unitLabel", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            val trendIcon = when {
                delta > 0.05f -> Icons.Filled.TrendingUp
                delta < -0.05f -> Icons.Filled.TrendingDown
                else -> Icons.Filled.TrendingFlat
            }
            Icon(trendIcon, contentDescription = null, tint = mutedColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(2.dp))
            Text(
                "${formatChartValue(abs(delta))} $unitLabel since first entry",
                style = MaterialTheme.typography.bodySmall,
                color = mutedColor
            )
        }
    }
}

private fun formatChartValue(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)
