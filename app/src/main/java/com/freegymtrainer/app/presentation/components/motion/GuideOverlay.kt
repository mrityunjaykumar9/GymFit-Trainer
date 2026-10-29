package com.freegymtrainer.app.presentation.components.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * Coaching guides drawn over the animation (see [Guide]): dashed reference lines and rings that show people WHAT to
 * watch - elbow under the wrist, knee over the toes, the straight line of the body, the incline of the back - so
 * the movement can be copied by looking, without reading. The geometry comes from a [GuideSpace], which exists
 * once for the flat 2D canvas and once for the perspective 3D view; this file only draws. Each mark carries a small
 * NUMBER badge; the words live in the legend under the animation (GuideLegend), so no text covers the athlete.
 */

/** Where the guide geometry lives on screen, supplied by the 2D canvas or the 3D stage. All results are in pixels. */
internal interface GuideSpace {
    fun joint(name: String): Offset?
    /** The point on the floor straight below the joint. */
    fun floorBelow(name: String): Offset?
    /** Both ends of a horizontal line through the joint, running along the direction the exercise moves in. */
    fun levelEnds(name: String): Pair<Offset, Offset>?
    fun ringRadius(name: String): Float
    /** The segment a -> b measured against the horizontal: an arc, the end of the horizontal reference and the angle. */
    fun tilt(a: String, b: String): Tilt?
}

internal class Tilt(val arc: List<Offset>, val horizontalEnd: Offset, val degrees: Float)

/** Remembers where this frame's badges already sit so a new one can slide out of the way instead of overlapping. */
internal class PillLayout {
    val rects = ArrayList<Rect>()
}

internal val GuideColor = Color(0xFF3DE0A4)
private val BadgeText = Color(0xFF06231A)

/** The angle (degrees, 0..90) of the segment a -> b against the horizontal in the flat side view. */
internal fun Skeleton.tiltDegrees(a: String, b: String): Float {
    val pa = point(a); val pb = point(b)
    return Math.toDegrees(atan2(abs(pb.y - pa.y), abs(pb.x - pa.x)).toDouble()).toFloat()
}

/**
 * Draws every guide of the motion; guide number n (1-based, in list order) gets badge n. [pulse] (0..1) gently
 * breathes the rings so they read as "watch this".
 */
internal fun DrawScope.drawGuides(
    guides: List<Guide>, space: GuideSpace, pulse: Float, measurer: TextMeasurer, style: TextStyle,
    layout: PillLayout = PillLayout()
) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx()))
    val width = 2.dp.toPx()
    val line = GuideColor.copy(alpha = 0.92f)
    guides.forEachIndexed { index, g ->
        val n = index + 1
        when (g.kind) {
            "plumb" -> {
                val top = space.joint(g.j) ?: return@forEachIndexed
                val floor = space.floorBelow(g.j) ?: return@forEachIndexed
                val from = Offset(top.x, top.y - 14.dp.toPx())
                drawLine(line, from, floor, width, StrokeCap.Round, dash)
                drawCircle(line, 3.5.dp.toPx(), floor)
                drawBadge(n, Offset(from.x, from.y - 6.dp.toPx()), measurer, style, layout)
            }
            "level" -> {
                val (l, r) = space.levelEnds(g.j) ?: return@forEachIndexed
                drawLine(line, l, r, width, StrokeCap.Round, dash)
                drawBadge(n, Offset(r.x + 4.dp.toPx(), r.y), measurer, style, layout)
            }
            "line" -> {
                val p = space.joint(g.j) ?: return@forEachIndexed
                val q = space.joint(g.a ?: return@forEachIndexed) ?: return@forEachIndexed
                val dx = (q.x - p.x) * 0.10f
                val dy = (q.y - p.y) * 0.10f
                val a = Offset(p.x - dx, p.y - dy)
                val b = Offset(q.x + dx, q.y + dy)
                drawLine(line, a, b, width, StrokeCap.Round, dash)
                drawBadge(n, Offset(b.x + (if (dx >= 0) 8 else -8).dp.toPx(), b.y - 6.dp.toPx()), measurer, style, layout)
            }
            "ring" -> {
                val c = space.joint(g.j) ?: return@forEachIndexed
                val r = space.ringRadius(g.j) * (1f + 0.10f * pulse)
                drawCircle(GuideColor.copy(alpha = 0.14f), r, c)
                drawCircle(line, r, c, style = Stroke(width * 1.2f))
                drawBadge(n, Offset(c.x + r * 0.75f, c.y - r * 0.75f), measurer, style, layout)
            }
            "tilt" -> {
                val t = space.tilt(g.j, g.a ?: return@forEachIndexed) ?: return@forEachIndexed
                val origin = space.joint(g.j) ?: return@forEachIndexed
                val tip = space.joint(g.a) ?: return@forEachIndexed
                drawLine(line.copy(alpha = 0.55f), origin, t.horizontalEnd, 1.5.dp.toPx(), StrokeCap.Round, dash)
                drawLine(line, origin, Offset(tip.x + (tip.x - origin.x) * 0.15f, tip.y + (tip.y - origin.y) * 0.15f), width, StrokeCap.Round, dash)
                for (i in 0 until t.arc.size - 1) drawLine(line, t.arc[i], t.arc[i + 1], width * 1.4f, StrokeCap.Round)
                val mid = t.arc.getOrNull(t.arc.size / 2) ?: origin
                drawBadge(n, Offset(mid.x + 14.dp.toPx(), mid.y - 4.dp.toPx()), measurer, style, layout)
            }
        }
    }
}

/** A small filled circle with the guide's number, nudged away from badges that are already there. */
private fun DrawScope.drawBadge(n: Int, anchor: Offset, measurer: TextMeasurer, style: TextStyle, layout: PillLayout) {
    val r = 9.dp.toPx()
    val gap = 3.dp.toPx()
    val margin = 2.dp.toPx() + r
    var x = anchor.x.coerceIn(margin, (size.width - margin).coerceAtLeast(margin))
    var y = anchor.y.coerceIn(margin, (size.height - margin).coerceAtLeast(margin))
    var tries = 0
    while (tries < 8 && layout.rects.any { it.overlaps(Rect(x - r, y - r, x + r, y + r).inflate(gap)) }) {
        y += 2 * r + gap
        if (y > size.height - margin) y = (y - (2 * r + gap) * (tries + 2)).coerceAtLeast(margin)
        tries++
    }
    layout.rects += Rect(x - r, y - r, x + r, y + r)
    drawCircle(GuideColor, r, Offset(x, y))
    drawCircle(Color.White.copy(alpha = 0.9f), r, Offset(x, y), style = Stroke(1.2.dp.toPx()))
    val text = measurer.measure(AnnotatedString(n.toString()), style.copy(color = BadgeText))
    drawText(text, BadgeText, Offset(x - text.size.width / 2f, y - text.size.height / 2f))
}

/** Guide geometry for the flat side-view canvas: pose units (1.0 = standing height) mapped by [s]. */
internal class FlatGuideSpace(
    private val sk: Skeleton,
    private val sc: Float,
    private val s: (Pt) -> Offset
) : GuideSpace {
    override fun joint(name: String) = s(sk.point(name))
    override fun floorBelow(name: String) = s(Pt(sk.point(name).x, 0f))
    override fun levelEnds(name: String): Pair<Offset, Offset> {
        val p = sk.point(name)
        return s(Pt(p.x - 0.30f, p.y)) to s(Pt(p.x + 0.30f, p.y))
    }
    override fun ringRadius(name: String) = 0.055f * sc
    override fun tilt(a: String, b: String): Tilt? {
        val pa = sk.point(a); val pb = sk.point(b)
        val dx = pb.x - pa.x; val dy = pb.y - pa.y
        if (hypot(dx, dy) < 1e-3f) return null
        val sign = if (dx >= 0f) 1f else -1f
        val ang = atan2(abs(dy), abs(dx))
        val r = 0.14f
        val arc = (0..12).map { i ->
            val th = ang * i / 12f * (if (dy >= 0f) 1f else -1f)
            s(Pt(pa.x + sign * r * cos(th), pa.y + r * sin(th)))
        }
        return Tilt(arc, s(Pt(pa.x + sign * 0.22f, pa.y)), Math.toDegrees(ang.toDouble()).toFloat())
    }
}
