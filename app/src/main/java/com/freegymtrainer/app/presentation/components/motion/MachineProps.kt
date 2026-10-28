package com.freegymtrainer.app.presentation.components.motion

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.hypot
import kotlin.math.max

/*
 * Gym machinery drawn from simple shapes: a cable tower with a weight stack, a pulley wheel, the
 * cable itself and the attachment in the athlete's hands (rope, straight bar or handle).
 */

/** Colours the machine is painted with (derived from the current theme by the caller). */
internal class MachineColors(val frame: Color, val steel: Color, val accent: Color, val bg: Color)

private fun DrawScope.worldRect(
    s: (Pt) -> Offset, sc: Float, x0: Float, y0: Float, x1: Float, y1: Float, color: Color, radius: Float = 0.006f
) {
    val tl = s(Pt(x0, y1))
    val br = s(Pt(x1, y0))
    drawRoundRect(color, tl, Size(br.x - tl.x, br.y - tl.y), CornerRadius(radius * sc))
}

/** The tower, weight stack and pulley wheel. The cable and the grip are drawn separately. */
internal fun DrawScope.drawCableTower(pulley: Pt, hasTower: Boolean, s: (Pt) -> Offset, sc: Float, c: MachineColors) {
    val dir = if (pulley.x >= 0f) 1f else -1f
    if (hasTower) {
        val px = pulley.x + dir * 0.10f
        val top = max(pulley.y + 0.13f, 0.95f)
        // base foot + pillar + inner channel + top cap
        worldRect(s, sc, px - 0.13f, 0f, px + 0.13f, 0.035f, c.frame, 0.012f)
        worldRect(s, sc, px - 0.055f, 0.03f, px + 0.055f, top, c.frame, 0.014f)
        worldRect(s, sc, px - 0.040f, 0.06f, px + 0.040f, top - 0.04f, c.steel.copy(alpha = 0.35f), 0.008f)
        worldRect(s, sc, px - 0.062f, top - 0.035f, px + 0.062f, top, c.steel, 0.010f)
        // weight stack: stacked plates with a selector pin
        val plate = 0.034f
        val gap = 0.006f
        val n = ((top - 0.2f - 0.07f) / (plate + gap)).toInt().coerceIn(4, 12)
        for (i in 0 until n) {
            val y0 = 0.07f + i * (plate + gap)
            worldRect(s, sc, px - 0.036f, y0, px + 0.036f, y0 + plate, c.steel.copy(alpha = 0.92f), 0.004f)
            worldRect(s, sc, px - 0.036f, y0 + plate - 0.006f, px + 0.036f, y0 + plate, c.bg.copy(alpha = 0.30f), 0.002f)
        }
        val pinY = 0.07f + 3 * (plate + gap) + plate / 2f
        drawLine(c.accent, s(Pt(px - dir * 0.058f, pinY)), s(Pt(px + dir * 0.02f, pinY)), 0.012f * sc, StrokeCap.Round)
        // bracket holding the pulley
        drawLine(c.frame, s(Pt(px - dir * 0.04f, pulley.y)), s(pulley), 0.026f * sc, StrokeCap.Round)
    }
    // pulley wheel
    val pc = s(pulley)
    drawCircle(c.frame, 0.050f * sc, pc)
    drawCircle(c.steel, 0.040f * sc, pc)
    drawCircle(c.bg.copy(alpha = 0.55f), 0.028f * sc, pc, style = Stroke(0.006f * sc))
    drawCircle(c.accent, 0.012f * sc, pc)
}

/** The cable run from the pulley to the athlete's hand. */
internal fun DrawScope.drawCableLine(pulley: Pt, hand: Pt, s: (Pt) -> Offset, sc: Float, c: MachineColors) {
    drawLine(c.steel, s(pulley), s(hand), 0.009f * sc, StrokeCap.Round)
}

/** What the athlete is holding: a rope, a straight bar, or a handle - drawn in front of the hand. */
internal fun DrawScope.drawCableGrip(pulley: Pt, hand: Pt, grip: String, s: (Pt) -> Offset, sc: Float, c: MachineColors) {
    val dx = pulley.x - hand.x
    val dy = pulley.y - hand.y
    val l = hypot(dx, dy).coerceAtLeast(1e-4f)
    val d = Pt(dx / l, dy / l) // unit vector from the hand toward the pulley
    fun at(k: Float) = Pt(hand.x + d.x * k, hand.y + d.y * k)
    when (grip) {
        "rope" -> {
            // a thick rope held in the fists with a knot at each end
            drawLine(c.frame, s(at(0.12f)), s(at(-0.075f)), 0.020f * sc, StrokeCap.Round)
            drawLine(c.steel, s(at(0.12f)), s(at(-0.075f)), 0.013f * sc, StrokeCap.Round)
            drawCircle(c.accent, 0.016f * sc, s(at(-0.075f)))
            drawCircle(c.frame, 0.016f * sc, s(at(0.12f)))
            drawCircle(c.steel, 0.011f * sc, s(at(0.12f)))
        }
        "bar" -> {
            // a straight bar seen end-on, hung from the cable by a short link
            drawLine(c.steel, s(hand), s(at(0.07f)), 0.010f * sc, StrokeCap.Round)
            drawCircle(c.frame, 0.026f * sc, s(hand))
            drawCircle(c.steel, 0.019f * sc, s(hand))
            drawCircle(c.accent, 0.007f * sc, s(hand))
        }
        else -> {
            // a D / V handle: a small stirrup around the fist
            drawLine(c.steel, s(at(0.08f)), s(hand), 0.011f * sc, StrokeCap.Round)
            drawCircle(c.frame, 0.027f * sc, s(hand), style = Stroke(0.013f * sc))
            drawCircle(c.accent, 0.020f * sc, s(hand), style = Stroke(0.006f * sc))
        }
    }
}
