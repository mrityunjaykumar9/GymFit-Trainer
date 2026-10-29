package com.freegymtrainer.app.presentation.components.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * The words that go with the numbered badges drawn on the animation (see [drawGuides]): "1  Wrist over elbow",
 * "2  Back angle 36°" ... plus the live angle of the main joint. Kept under the picture so no text covers the athlete.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GuideLegend(motion: MotionDef, progress: () -> Float, modifier: Modifier = Modifier) {
    val labelled = remember(motion) { motion.guides.withIndex().filter { it.value.label.isNotBlank() } }
    val measure = remember(motion) { motion.measures.firstOrNull { !it.isLean } }
    // Whole degrees only, so the legend recomposes when a number changes, not on every animation frame.
    val live by remember(motion) {
        derivedStateOf {
            val sk = motion.skeletonAt(progress())
            val tilts = labelled.map { (_, g) -> if (g.kind == "tilt" && g.a != null) sk.tiltDegrees(g.j, g.a).roundToInt() else -1 }
            tilts to (measure?.let { sk.measure(it).roundToInt() } ?: -1)
        }
    }
    if (labelled.isEmpty() && measure == null) return
    FlowRow(
        modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labelled.forEachIndexed { i, (index, g) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(20.dp).background(GuideColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = androidx.compose.ui.graphics.Color(0xFF06231A)
                    )
                }
                Spacer(Modifier.width(6.dp))
                val deg = live.first[i]
                Text(
                    if (deg >= 0) "${g.label} $deg°" else g.label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (measure != null && live.second >= 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Straighten, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${measure.name} ${live.second}°",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
