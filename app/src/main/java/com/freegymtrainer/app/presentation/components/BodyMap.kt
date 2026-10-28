package com.freegymtrainer.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.min

/*
 * A clean front / back body diagram with the worked muscles lit up. Everything is drawn on a 100 x 200 grid and
 * scaled to fit, so it is crisp at any size and adds nothing to the APK.
 *
 * Region ids: chest, shoulders, rear_delts, biceps, triceps, forearms, abs, quads, hamstrings, calves, glutes,
 * lats, traps.
 */

/** Maps the muscle names used in the exercise library onto body-diagram regions. */
object MuscleRegions {
    fun of(muscle: String): Set<String> = when (muscle.trim().lowercase()) {
        "chest" -> setOf("chest")
        "back" -> setOf("lats", "traps")
        "shoulders" -> setOf("shoulders")
        "rear delts" -> setOf("rear_delts")
        "biceps" -> setOf("biceps")
        "triceps" -> setOf("triceps")
        "forearms" -> setOf("forearms")
        "abs", "core" -> setOf("abs")
        "legs" -> setOf("quads", "hamstrings", "calves")
        "quads" -> setOf("quads")
        "hamstrings" -> setOf("hamstrings")
        "calves" -> setOf("calves")
        "glutes" -> setOf("glutes")
        "traps" -> setOf("traps")
        "full body", "cardio" -> setOf("chest", "shoulders", "abs", "quads", "calves", "lats", "glutes", "hamstrings")
        "flexibility", "balance" -> setOf("abs", "quads", "hamstrings", "glutes", "lats")
        else -> emptySet()
    }

    fun of(muscles: Collection<String>): Set<String> = muscles.flatMapTo(HashSet()) { of(it) }
}

/** One figure, [front] or back, with [primary] regions in full colour and [secondary] ones softer. */
@Composable
fun BodyMap(
    front: Boolean, primary: Set<String>, secondary: Set<String> = emptySet(), modifier: Modifier = Modifier,
    highlight: Color = MaterialTheme.colorScheme.primary, skinColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val skinTop = skinColor.copy(alpha = 0.20f)
    val skinBottom = skinColor.copy(alpha = 0.09f)
    val edge = skinColor.copy(alpha = 0.22f)
    val hi = highlight
    val hiSoft = highlight.copy(alpha = 0.40f)
    Canvas(modifier) {
        val sc = min(size.width / 100f, size.height / 200f)
        val ox = (size.width - 100f * sc) / 2f
        val oy = (size.height - 200f * sc) / 2f
        val p: (Float, Float) -> Offset = { x, y -> Offset(ox + x * sc, oy + y * sc) }
        val skin = Brush.verticalGradient(listOf(skinTop, skinBottom), startY = oy, endY = oy + 200f * sc)

        // silhouette
        silhouette(p).let { path ->
            drawPath(path, skin)
            drawPath(path, edge, style = Stroke(0.8f * sc, join = StrokeJoin.Round))
        }

        // muscles: soft ones first, then the main ones on top with a glow
        for ((ids, color, glow) in listOf(Triple(secondary - primary, hiSoft, false), Triple(primary, hi, true))) {
            for (id in ids) for (path in region(id, front, p, sc)) {
                if (glow) drawPath(path, hi.copy(alpha = 0.30f), style = Stroke(3.6f * sc, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, color)
                if (glow) drawPath(path, Color.White.copy(alpha = 0.16f), style = Stroke(0.8f * sc, join = StrokeJoin.Round))
            }
        }
    }
}

/** Front and back figures side by side, with a legend line underneath handled by the caller. */
@Composable
fun BodyMapPair(primary: Set<String>, secondary: Set<String> = emptySet(), modifier: Modifier = Modifier, figureHeight: androidx.compose.ui.unit.Dp = 190.dp) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        BodyMap(true, primary, secondary, Modifier.weight(1f).height(figureHeight))
        BodyMap(false, primary, secondary, Modifier.weight(1f).height(figureHeight))
    }
}

// ------------------------------------------------------------------------------------------------ geometry

private fun oval(p: (Float, Float) -> Offset, cx: Float, cy: Float, rx: Float, ry: Float) =
    Path().apply { addOval(Rect(p(cx - rx, cy - ry), p(cx + rx, cy + ry))) }

private fun poly(p: (Float, Float) -> Offset, vararg pts: Float) = Path().apply {
    val first = p(pts[0], pts[1]); moveTo(first.x, first.y)
    for (i in 2 until pts.size step 2) p(pts[i], pts[i + 1]).let { lineTo(it.x, it.y) }
    close()
}

/** A limb segment that is [wa] wide at (ax, ay) and [wb] wide at (bx, by), with rounded ends. */
private fun taper(p: (Float, Float) -> Offset, ax: Float, ay: Float, bx: Float, by: Float, wa: Float, wb: Float): Path {
    val dx = bx - ax; val dy = by - ay
    val len = hypot(dx, dy).coerceAtLeast(0.001f)
    val nx = -dy / len; val ny = dx / len
    val path = Path()
    val a1 = p(ax + nx * wa / 2, ay + ny * wa / 2); val a2 = p(ax - nx * wa / 2, ay - ny * wa / 2)
    val b1 = p(bx + nx * wb / 2, by + ny * wb / 2); val b2 = p(bx - nx * wb / 2, by - ny * wb / 2)
    path.moveTo(a1.x, a1.y); path.lineTo(b1.x, b1.y); path.lineTo(b2.x, b2.y); path.lineTo(a2.x, a2.y); path.close()
    val ca = p(ax, ay); val cb = p(bx, by)
    val unit = p(1f, 0f).x - p(0f, 0f).x
    path.addOval(Rect(ca.x - wa / 2 * unit, ca.y - wa / 2 * unit, ca.x + wa / 2 * unit, ca.y + wa / 2 * unit))
    path.addOval(Rect(cb.x - wb / 2 * unit, cb.y - wb / 2 * unit, cb.x + wb / 2 * unit, cb.y + wb / 2 * unit))
    return path
}

private fun mirrored(x: Float) = 100f - x

private fun silhouette(p: (Float, Float) -> Offset): Path {
    val parts = ArrayList<Path>()
    parts += oval(p, 50f, 14f, 8.4f, 10.4f)                                 // head
    parts += taper(p, 50f, 24f, 50f, 34f, 10f, 13f)                         // neck
    // torso: broad shoulders tapering to the waist, then the hips
    parts += Path().apply {
        fun m(x: Float, y: Float) = p(x, y).let { moveTo(it.x, it.y) }
        fun l(x: Float, y: Float) = p(x, y).let { lineTo(it.x, it.y) }
        fun q(cx: Float, cy: Float, x: Float, y: Float) { val c = p(cx, cy); val e = p(x, y); quadraticBezierTo(c.x, c.y, e.x, e.y) }
        m(22f, 44f); q(50f, 30f, 78f, 44f); l(74f, 66f); q(70f, 84f, 66f, 102f); q(69f, 114f, 72f, 126f)
        l(28f, 126f); q(31f, 114f, 34f, 102f); q(30f, 84f, 26f, 66f); close()
    }
    for (side in 0..1) {
        fun x(v: Float) = if (side == 0) v else mirrored(v)
        parts += taper(p, x(23f), 47f, x(17f), 82f, 14f, 11f)               // upper arm
        parts += taper(p, x(17f), 82f, x(13f), 114f, 11f, 7f)               // forearm
        parts += oval(p, x(12.5f), 120f, 4.2f, 5.6f)                        // hand
        parts += taper(p, x(41f), 124f, x(38f), 158f, 23f, 15f)             // thigh
        parts += taper(p, x(38f), 158f, x(38f), 193f, 15f, 8.5f)            // shin
        parts += oval(p, x(38f), 197f, 7f, 3.2f)                            // foot
    }
    // one clean outline: overlapping limbs must not double-darken where they meet
    var union = parts[0]
    for (i in 1 until parts.size) union = Path.combine(PathOperation.Union, union, parts[i])
    return union
}

private fun region(id: String, front: Boolean, p: (Float, Float) -> Offset, sc: Float): List<Path> {
    val out = ArrayList<Path>()
    fun both(block: (side: Int, x: (Float) -> Float) -> Unit) {
        for (side in 0..1) block(side) { v -> if (side == 0) v else mirrored(v) }
    }
    when (id) {
        "chest" -> if (front) both { _, x -> out += oval(p, x(38.5f), 58f, 12.5f, 9f) }
        "shoulders" -> both { _, x -> out += oval(p, x(23f), 49f, 9.2f, 9.4f) }
        "rear_delts" -> if (!front) both { _, x -> out += oval(p, x(23f), 49f, 9.2f, 9.4f) }
        "biceps" -> if (front) both { _, x -> out += taper(p, x(22.5f), 58f, x(18.8f), 79f, 9.8f, 8.2f) }
        "triceps" -> if (!front) both { _, x -> out += taper(p, x(22.5f), 58f, x(18.8f), 79f, 10.2f, 8.2f) }
        "forearms" -> both { _, x -> out += taper(p, x(16.5f), 88f, x(13.8f), 111f, 8.6f, 6f) }
        "abs" -> if (front) {
            for (r in 0..2) for (c in 0..1) {
                val rr = RoundRect(Rect(p(42f + c * 8.9f, 68f + r * 11f), p(42f + c * 8.9f + 7.9f, 68f + r * 11f + 9.6f)), CornerRadius(2.2f * sc))
                out += Path().apply { addRoundRect(rr) }
            }
        }
        "quads" -> if (front) both { _, x -> out += taper(p, x(41f), 130f, x(38.5f), 155f, 19.5f, 13f) }
        "hamstrings" -> if (!front) both { _, x -> out += taper(p, x(41f), 130f, x(38.5f), 155f, 19.5f, 13f) }
        "calves" -> both { _, x -> out += taper(p, x(38f), 164f, x(38f), 187f, if (front) 11f else 13f, 7.5f) }
        "glutes" -> if (!front) both { _, x -> out += oval(p, x(41f), 119f, 12f, 10f) }
        "traps" -> if (front) both { _, x -> out += taper(p, x(46.5f), 32f, x(33f), 40f, 6f, 5f) }
                   else out += poly(p, 50f, 30f, 67f, 42f, 50f, 74f, 33f, 42f)
        "lats" -> if (!front) both { _, x -> out += poly(p, x(35f), 50f, x(27f), 58f, x(29f), 80f, x(40f), 99f, x(48.5f), 92f, x(48.5f), 62f) }
    }
    return out
}
