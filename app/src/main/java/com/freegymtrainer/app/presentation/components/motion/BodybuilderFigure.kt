package com.freegymtrainer.app.presentation.components.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/*
 * The muscular athlete drawn on top of the joint skeleton. Everything is plain vector paths:
 * anatomically shaped limbs (quads on the front of the thigh, calves at the back, biceps/triceps),
 * gradient shading from a fixed light at the upper left, soft cast shadows, a thin dark-brown
 * edge, veins and oily highlights, and a side-profile head with brow ridge, nose, lips and a strong
 * jaw. It is original artwork with nothing to license. The skeleton still drives every pose; this
 * file only decides how the body looks around it.
 */

internal object BodyPalette {
    val hot = Color(0xFFFF3B2F)
    val hotCore = Color(0xFFFFC857)
}

private class Tone(val light: Color, val base: Color, val dark: Color)

private val SKIN_N = Tone(Color(0xFFE2A27C), Color(0xFFB36E44), Color(0xFF63321A))
private val SKIN_F = Tone(Color(0xFFB87952), Color(0xFF8F5636), Color(0xFF4C2615))
private val SHORTS_N = Tone(Color(0xFF626C80), Color(0xFF353C4B), Color(0xFF1B2029))
private val SHORTS_F = Tone(Color(0xFF4F586B), Color(0xFF2A303D), Color(0xFF151921))
private val SHOE_N = Tone(Color(0xFFFFFFFF), Color(0xFFE6E8EE), Color(0xFFAEB3C0))
private val SHOE_F = Tone(Color(0xFFE2E4EA), Color(0xFFBCC0CB), Color(0xFF8A8F9C))
private val HAIR = Color(0xFF241610)
private val INK = Color(0xFF4A2A18)
private val CREASE = Color(0xFF4A220F)
private val VEIN = Color(0xFF7A3F3A)
private val BAND = Color(0xFFFF8F4D)
private val SOLE = Color(0xFF2A2E38)
private val ARM_FILL = Color(0xFF7A4A32)
private val ARM_FILL_FAR = Color(0xFF462A1E)
private val LEG_FILL = Color(0xFF5E3623)       // fills the gap at a bent knee between the two photo sprites
private val LEG_FILL_FAR = Color(0xFF38211A)
private const val HEAD_SPRITE_WORLD_PER_PX = 0.00105f // head photo scale: a smaller head on a big frame

/** Screen-space direction pointing toward the light (upper left). */
private val LIGHT = Offset(-0.55f, -0.83f)

/**
 * One tapered muscle/limb piece. Bulges are separate for the "front" side ([bulgeF]: quads, biceps,
 * shin) and the "back" side ([bulgeB]: hamstring, triceps, calf); [fs] says which screen normal
 * side is the front (+1 / -1).
 */
private class Seg(
    val a: Pt, val b: Pt, val ra: Float, val rb: Float,
    val tone: Tone, val bulgeF: Float = 0f, val bulgeB: Float = 0f, val at: Float = 0.5f,
    val k: Float = 1f, val fs: Float = 1f
)

private fun prof(t: Float, pts: Array<FloatArray>): Float {
    for (i in 0 until pts.size - 1) {
        val a = pts[i]
        val b = pts[i + 1]
        if (t <= b[0]) {
            var u = ((t - a[0]) / (b[0] - a[0])).coerceIn(0f, 1f)
            u = u * u * (3 - 2 * u)
            return a[1] + (b[1] - a[1]) * u
        }
    }
    return pts.last()[1]
}

/** Smooth closed curve through [pts] (Catmull-Rom converted to cubic Beziers). */
private fun smoothClosed(pts: List<Offset>): Path {
    val n = pts.size
    return Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 0 until n) {
            val p0 = pts[(i - 1 + n) % n]
            val p1 = pts[i]
            val p2 = pts[(i + 1) % n]
            val p3 = pts[(i + 2) % n]
            cubicTo(
                p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
                p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
                p2.x, p2.y
            )
        }
        close()
    }
}

// Torso half-thickness (world units) along the spine, t = 0 at the hip .. 1 at the shoulder.
private val FRONT = arrayOf(
    floatArrayOf(0f, 0.048f), floatArrayOf(0.15f, 0.044f), floatArrayOf(0.38f, 0.050f),
    floatArrayOf(0.60f, 0.078f), floatArrayOf(0.74f, 0.098f), floatArrayOf(0.88f, 0.078f), floatArrayOf(1f, 0.046f)
)
private val BACK = arrayOf(
    floatArrayOf(0f, 0.064f), floatArrayOf(0.06f, 0.070f), floatArrayOf(0.18f, 0.050f), floatArrayOf(0.40f, 0.066f),
    floatArrayOf(0.62f, 0.086f), floatArrayOf(0.84f, 0.092f), floatArrayOf(1f, 0.058f)
)

// Side profile of the head in head-local coordinates (f = forward, u = up), clockwise from the top-front.
private val HEAD_PROFILE = arrayOf(
    floatArrayOf(0.020f, 0.064f), floatArrayOf(0.044f, 0.051f), floatArrayOf(0.052f, 0.033f), floatArrayOf(0.058f, 0.019f),
    floatArrayOf(0.051f, 0.010f), floatArrayOf(0.054f, 0.000f), floatArrayOf(0.073f, -0.014f), floatArrayOf(0.060f, -0.020f),
    floatArrayOf(0.062f, -0.027f), floatArrayOf(0.056f, -0.032f), floatArrayOf(0.059f, -0.038f), floatArrayOf(0.050f, -0.046f),
    floatArrayOf(0.055f, -0.058f), floatArrayOf(0.030f, -0.067f), floatArrayOf(-0.005f, -0.061f), floatArrayOf(-0.030f, -0.048f),
    floatArrayOf(-0.055f, -0.020f), floatArrayOf(-0.062f, 0.015f), floatArrayOf(-0.050f, 0.048f), floatArrayOf(-0.020f, 0.063f)
)

// Short-cropped hair: outer skull edge then back along the hairline.
private val HAIR_SHAPE = arrayOf(
    floatArrayOf(0.042f, 0.059f), floatArrayOf(0.020f, 0.0685f), floatArrayOf(-0.020f, 0.067f), floatArrayOf(-0.053f, 0.051f),
    floatArrayOf(-0.066f, 0.015f), floatArrayOf(-0.059f, -0.022f), floatArrayOf(-0.042f, -0.036f),
    floatArrayOf(-0.038f, -0.012f), floatArrayOf(-0.022f, 0.012f), floatArrayOf(-0.006f, 0.027f), floatArrayOf(0.020f, 0.041f),
    floatArrayOf(0.040f, 0.047f)
)

/**
 * Draws the athlete. [focus] are the muscle tokens from the motion JSON; they glow red/gold and
 * "pump" with [pulse] (0..1) so it is obvious which muscles the exercise works.
 */
internal fun DrawScope.drawBodybuilder(
    sk: Skeleton,
    focus: List<String>,
    pulse: Float,
    s: (Pt) -> Offset,
    sc: Float,
    sprites: BodySprites? = null
) {
    val tk = 1.10f // overall muscularity
    val edge = 0.0045f // outline thickness (world units)

    fun unit(d: Pt): Pt {
        val l = hypot(d.x, d.y).coerceAtLeast(1e-4f)
        return Pt(d.x / l, d.y / l)
    }
    fun rot90(d: Pt) = Pt(-d.y, d.x)
    fun dot(a: Pt, b: Pt) = a.x * b.x + a.y * b.y

    /** Which screen-normal side of segment a->b points toward [frontWorld]. */
    fun fsFor(a: Pt, b: Pt, frontWorld: Pt): Float {
        val pa = s(a)
        val pb = s(b)
        val len = hypot(pb.x - pa.x, pb.y - pa.y).coerceAtLeast(1e-3f)
        val nx = -(pb.y - pa.y) / len
        val ny = (pb.x - pa.x) / len
        return if (nx * frontWorld.x + ny * (-frontWorld.y) >= 0f) 1f else -1f
    }

    // ---- shapes -------------------------------------------------------------------------------
    fun taperPath(a: Pt, b: Pt, ra: Float, rb: Float, bF: Float, bB: Float, at: Float, fs: Float): Path {
        val pa = s(a)
        val pb = s(b)
        val dx = pb.x - pa.x
        val dy = pb.y - pa.y
        val len = hypot(dx, dy).coerceAtLeast(1e-3f)
        val nx = -dy / len
        val ny = dx / len
        val steps = 20
        fun base(t: Float) = ra + (rb - ra) * t
        fun bump(t: Float): Float { val d = t - at; return exp(-(d * d) / 0.05f) }
        fun rPlus(t: Float) = (base(t) + (if (fs >= 0f) bF else bB) * bump(t)) * sc
        fun rMinus(t: Float) = (base(t) + (if (fs >= 0f) bB else bF) * bump(t)) * sc
        return Path().apply {
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val x = pa.x + dx * t + nx * rPlus(t)
                val y = pa.y + dy * t + ny * rPlus(t)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            for (i in steps downTo 0) {
                val t = i / steps.toFloat()
                lineTo(pa.x + dx * t - nx * rMinus(t), pa.y + dy * t - ny * rMinus(t))
            }
            close()
        }
    }

    fun segPath(g: Seg, grow: Float) = taperPath(
        g.a, g.b, g.ra * g.k * tk + grow, g.rb * g.k * tk + grow,
        g.bulgeF * g.k * tk, g.bulgeB * g.k * tk, g.at, g.fs
    )

    /** Gradient running across the segment, from the lit edge to the shadowed edge. */
    fun acrossBrush(g: Seg, stops: Array<Pair<Float, Color>>): Brush {
        val pa = s(g.a)
        val pb = s(g.b)
        val mid = Offset((pa.x + pb.x) / 2f, (pa.y + pb.y) / 2f)
        val len = hypot(pb.x - pa.x, pb.y - pa.y).coerceAtLeast(1e-3f)
        var nx = -(pb.y - pa.y) / len
        var ny = (pb.x - pa.x) / len
        if (nx * LIGHT.x + ny * LIGHT.y < 0f) { nx = -nx; ny = -ny }
        val r = (max(g.ra, g.rb) + max(g.bulgeF, g.bulgeB)) * g.k * tk * sc
        return Brush.linearGradient(
            colorStops = stops,
            start = Offset(mid.x + nx * r, mid.y + ny * r),
            end = Offset(mid.x - nx * r, mid.y - ny * r)
        )
    }

    fun shadeBrush(g: Seg) = acrossBrush(g, arrayOf(0f to g.tone.light, 0.36f to g.tone.base, 1f to g.tone.dark))

    /** Draws a chain of segments as ONE silhouette: all outlines first, then all fills (no seams inside a limb). */
    fun drawSegs(segs: List<Seg>) {
        for (g in segs) {
            drawPath(segPath(g, edge), INK)
            drawCircle(INK, (g.ra * g.k * tk + edge) * sc, s(g.a))
            drawCircle(INK, (g.rb * g.k * tk + edge) * sc, s(g.b))
        }
        for (g in segs) {
            val br = shadeBrush(g)
            drawPath(segPath(g, 0f), br)
            drawCircle(br, g.ra * g.k * tk * sc, s(g.a))
            drawCircle(br, g.rb * g.k * tk * sc, s(g.b))
        }
    }

    /** Soft contact shadow of a limb on whatever is behind it (torso, floor). */
    fun castShadow(segs: List<Seg>, alpha: Float = 0.20f) {
        translate(0.010f * sc, 0.014f * sc) {
            for (g in segs) {
                drawPath(segPath(g, edge), Color.Black.copy(alpha = alpha))
                drawCircle(Color.Black.copy(alpha = alpha), (g.ra * g.k * tk + edge) * sc, s(g.a))
                drawCircle(Color.Black.copy(alpha = alpha), (g.rb * g.k * tk + edge) * sc, s(g.b))
            }
        }
    }

    /** True when world-space normal [n] points toward the light (screen y is flipped vs world y). */
    fun facesLight(n: Pt) = n.x * LIGHT.x + (-n.y) * LIGHT.y > 0f

    /** A narrow oily highlight on the lit side of a muscle, fading out at both ends. */
    fun sheen(g: Seg, alpha: Float = 0.34f) {
        val d = unit(g.b - g.a)
        var n = rot90(d)
        if (!facesLight(n)) n = Pt(-n.x, -n.y)
        val off = max(g.ra, g.rb) * g.k * tk * 0.45f
        val a2 = g.a + n * off + d * ((g.b - g.a).let { hypot(it.x, it.y) } * 0.10f)
        val b2 = g.b + n * off - d * ((g.b - g.a).let { hypot(it.x, it.y) } * 0.10f)
        val br = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = alpha), Color.White.copy(alpha = 0f)),
            start = s(a2), end = s(b2)
        )
        drawPath(
            taperPath(a2, b2, g.ra * g.k * tk * 0.20f, g.rb * g.k * tk * 0.18f, g.bulgeF * g.k * tk * 0.30f, g.bulgeB * g.k * tk * 0.30f, g.at, g.fs),
            br
        )
    }

    /** A faint warm bounce light along the shadowed edge - separates the limb from the dark background. */
    fun rimLight(g: Seg, alpha: Float = 0.20f) {
        val d = unit(g.b - g.a)
        var n = rot90(d)
        if (facesLight(n)) n = Pt(-n.x, -n.y)
        val off = max(g.ra, g.rb) * g.k * tk * 0.86f
        drawPath(
            taperPath(g.a + n * off, g.b + n * off, g.ra * g.k * tk * 0.10f, g.rb * g.k * tk * 0.09f, g.bulgeF * g.k * tk * 0.12f, g.bulgeB * g.k * tk * 0.12f, g.at, g.fs),
            Color(0xFFFFB27A).copy(alpha = alpha)
        )
    }

    fun curve(a: Pt, c: Pt, b: Pt, w: Float, alpha: Float, color: Color = CREASE) {
        val pa = s(a)
        val pc = s(c)
        val pb = s(b)
        drawPath(
            Path().apply { moveTo(pa.x, pa.y); quadraticBezierTo(pc.x, pc.y, pb.x, pb.y) },
            color.copy(alpha = alpha), style = Stroke(w * sc, cap = StrokeCap.Round)
        )
    }

    /** A line running along a limb, offset toward the shadowed side (a muscle separation / vein). */
    fun limbLine(a: Pt, b: Pt, from: Float, to: Float, off: Float, alpha: Float = 0.52f, w: Float = 0.0050f, color: Color = CREASE) {
        val d = unit(b - a)
        var n = rot90(d)
        if (facesLight(n)) n = Pt(-n.x, -n.y)
        val p0 = a + (b - a) * from + n * off
        val p1 = a + (b - a) * to + n * off
        val mid = (p0 + p1) * 0.5f + n * (off * 0.35f)
        curve(p0, mid, p1, w, alpha, color)
    }

    // ---- torso geometry ----------------------------------------------------------------------
    val rad = Math.toRadians(sk.torso.toDouble())
    val nFront = Pt(cos(rad).toFloat(), -sin(rad).toFloat())
    val ctrl = Pt(
        (sk.hip.x + sk.shoulder.x) / 2f + nFront.x * 2f * sk.spine,
        (sk.hip.y + sk.shoulder.y) / 2f + nFront.y * 2f * sk.spine
    )
    fun spineAt(t: Float): Pt {
        val u = 1f - t
        return Pt(
            u * u * sk.hip.x + 2 * u * t * ctrl.x + t * t * sk.shoulder.x,
            u * u * sk.hip.y + 2 * u * t * ctrl.y + t * t * sk.shoulder.y
        )
    }
    fun frontAt(t: Float): Pt {
        val u = 1f - t
        val tx = 2 * u * (ctrl.x - sk.hip.x) + 2 * t * (sk.shoulder.x - ctrl.x)
        val ty = 2 * u * (ctrl.y - sk.hip.y) + 2 * t * (sk.shoulder.y - ctrl.y)
        val l = hypot(tx, ty).coerceAtLeast(1e-4f)
        return Pt(ty / l, -tx / l)
    }
    fun torsoPath(t0: Float, t1: Float, k: Float, grow: Float = 0f): Path {
        val steps = max(5, ((t1 - t0) * 26).toInt())
        return Path().apply {
            for (i in 0..steps) {
                val t = t0 + (t1 - t0) * i / steps
                val o = s(spineAt(t) + frontAt(t) * (prof(t, FRONT) * k + grow))
                if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
            }
            for (i in steps downTo 0) {
                val t = t0 + (t1 - t0) * i / steps
                val o = s(spineAt(t) - frontAt(t) * (prof(t, BACK) * k + grow))
                lineTo(o.x, o.y)
            }
            close()
        }
    }
    fun frontPt(t: Float, f: Float) = spineAt(t) + frontAt(t) * (prof(t, FRONT) * f)
    fun backPt(t: Float, f: Float) = spineAt(t) - frontAt(t) * (prof(t, BACK) * f)

    // ---- muscle glow --------------------------------------------------------------------------
    val glowA = 0.07f + 0.05f * pulse
    val coreA = 0.26f + 0.12f * pulse
    /** Soft additive glow for photographic skin: overlapping radial blobs along the muscle, no hard edges. */
    fun softGlow(a: Pt, b: Pt, ra: Float, rb: Float) {
        val amp = 0.30f + 0.24f * pulse
        val n = 4
        for (i in 0 until n) {
            val t = 0.12f + 0.76f * i / (n - 1)
            val c = a + (b - a) * t
            val r = ((ra + (rb - ra) * t) * tk * 1.45f + 0.008f) * sc
            val br = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to BodyPalette.hotCore.copy(alpha = amp * 0.35f),
                    0.45f to BodyPalette.hot.copy(alpha = amp),
                    1f to Color.Transparent
                ),
                center = s(c), radius = r
            )
            drawCircle(br, r, s(c), blendMode = BlendMode.Plus)
        }
    }
    fun hot(a: Pt, b: Pt, ra: Float, rb: Float, bulge: Float = 0f, at: Float = 0.5f) {
        if (sprites != null) { softGlow(a, b, ra, rb); return }
        val g = Seg(a, b, ra, rb, SKIN_N, bulgeF = bulge, bulgeB = bulge, at = at)
        val h = BodyPalette.hot
        drawPath(segPath(g, 0.014f), h.copy(alpha = glowA))
        drawCircle(h.copy(alpha = glowA), (ra * tk + 0.014f) * sc, s(a))
        drawCircle(h.copy(alpha = glowA), (rb * tk + 0.014f) * sc, s(b))
        val br = acrossBrush(g, arrayOf(0f to h.copy(alpha = coreA * 0.95f), 0.55f to h.copy(alpha = coreA), 1f to h.copy(alpha = coreA * 0.62f)))
        val bm = if (sprites != null) BlendMode.Overlay else BlendMode.SrcOver
        drawPath(segPath(g, 0f), br, blendMode = bm)
        drawCircle(br, ra * tk * sc, s(a), blendMode = bm)
        drawCircle(br, rb * tk * sc, s(b), blendMode = bm)
        drawPath(
            taperPath(a + (b - a) * 0.18f, b - (b - a) * 0.14f, ra * tk * 0.42f, rb * tk * 0.42f, bulge * tk * 0.4f, bulge * tk * 0.4f, at, 1f),
            BodyPalette.hotCore.copy(alpha = 0.06f + 0.10f * pulse)
        )
    }
    fun hotBlob(c: Pt, r: Float) {
        if (sprites != null) { softGlow(c, c, r, r); return }
        val h = BodyPalette.hot
        drawCircle(h.copy(alpha = glowA), (r * 1.4f) * sc, s(c))
        drawCircle(h.copy(alpha = coreA), r * sc, s(c))
        drawCircle(BodyPalette.hotCore.copy(alpha = 0.06f + 0.10f * pulse), r * 0.5f * sc, s(c))
    }

    fun limbGlow(near: Boolean) {
        val elbow = if (near) sk.elbowN else sk.elbowF
        val wrist = if (near) sk.wristN else sk.wristF
        val knee = if (near) sk.kneeN else sk.kneeF
        val ankle = if (near) sk.ankleN else sk.ankleF
        val q = if (near) 1f else 0.92f
        for (token in focus) when (token) {
            "uarm" -> hot(sk.shoulder + (elbow - sk.shoulder) * 0.10f, elbow, 0.040f * q, 0.030f * q, 0.016f * q, 0.45f)
            "farm" -> hot(elbow, wrist, 0.031f * q, 0.020f * q, 0.009f * q, 0.25f)
            "thigh", "hamstrings" -> hot(sk.hip + (knee - sk.hip) * 0.42f, knee, 0.056f * q, 0.040f * q, 0.012f * q, 0.35f)
            "shin" -> hot(knee, ankle, 0.038f * q, 0.023f * q, 0.016f * q, 0.28f)
            "shoulder" -> if (near) hotBlob(sk.shoulder + nFront * 0.004f, 0.050f)
        }
    }

    fun torsoGlow() {
        for (token in focus) when (token) {
            "chest" -> hot(spineAt(0.60f) + frontAt(0.60f) * 0.026f, spineAt(0.88f) + frontAt(0.88f) * 0.024f, 0.046f, 0.040f, 0.008f)
            "back" -> hot(spineAt(0.42f) - frontAt(0.42f) * 0.030f, spineAt(0.90f) - frontAt(0.90f) * 0.026f, 0.044f, 0.038f, 0.008f)
            "abs" -> hot(spineAt(0.07f) + frontAt(0.07f) * 0.032f, spineAt(0.50f) + frontAt(0.50f) * 0.032f, 0.032f, 0.032f)
            "glutes" -> hotBlob(sk.hip - nFront * 0.036f, 0.052f)
            "traps" -> hot(spineAt(0.90f) - frontAt(0.90f) * 0.020f, sk.shoulder + vec(sk.torso) * 0.030f, 0.042f, 0.032f)
        }
    }

    // ---- limbs --------------------------------------------------------------------------------
    fun leg(near: Boolean) {
        val knee = if (near) sk.kneeN else sk.kneeF
        val ankle = if (near) sk.ankleN else sk.ankleF
        val toe = if (near) sk.toeN else sk.toeF
        val q = if (near) 1f else 0.93f
        val skin = if (near) SKIN_N else SKIN_F
        // "front" of the leg is the toe side of the shin; the thigh keeps the same rotational sense
        val dS = unit(ankle - knee)
        val dT = unit(knee - sk.hip)
        val sgn = if (dot(rot90(dS), toe - ankle) >= 0f) 1f else -1f
        val fsShin = fsFor(knee, ankle, rot90(dS) * sgn)
        val fsThigh = fsFor(sk.hip, knee, rot90(dT) * sgn)
        val thigh = Seg(sk.hip, knee, 0.066f * q, 0.043f * q, skin, bulgeF = 0.030f * q, bulgeB = 0.012f * q, at = 0.48f, fs = fsThigh)
        val shin = Seg(knee, ankle, 0.043f * q, 0.024f * q, skin, bulgeF = 0.006f * q, bulgeB = 0.033f * q, at = 0.28f, fs = fsShin)
        val shoe = Seg(ankle, toe, 0.033f * q, 0.027f * q, if (near) SHOE_N else SHOE_F)
        if (sprites != null) {
            val f = if (near) null else FAR_LIMB_FILTER
            // filler so a bent knee never shows a gap between the thigh and shin photos
            drawCircle(if (near) LEG_FILL else LEG_FILL_FAR, 0.040f * q * sc, s(knee))
            val thighLen = hypot(knee.x - sk.hip.x, knee.y - sk.hip.y)
            val shinLen = hypot(ankle.x - knee.x, ankle.y - knee.y)
            drawSprite(sprites.thigh, s(sk.hip), s(knee), thighLen * sc / sprites.thigh.boneLenPx, f)
            drawSprite(sprites.shin, s(knee), s(ankle), shinLen * sc / sprites.shin.boneLenPx, f)
            val footLen = hypot(toe.x - ankle.x, toe.y - ankle.y)
            drawSprite(sprites.foot, s(ankle), s(toe), footLen * sc / sprites.foot.boneLenPx, f)
        } else {
            if (near) castShadow(listOf(thigh, shin, shoe), 0.22f)
            drawSegs(listOf(thigh, shin, shoe))
            if (near) { sheen(thigh); sheen(shin, 0.28f); rimLight(thigh); rimLight(shin) }
            // quad sweep, hamstring split, calf split, kneecap
            limbLine(sk.hip, knee, 0.42f, 0.90f, 0.018f * q)
            limbLine(knee, ankle, 0.12f, 0.60f, 0.012f * q, 0.34f)
            drawCircle(skin.light.copy(alpha = 0.30f), 0.014f * q * sc, s(knee + (ankle - knee) * 0.05f))
        }
        if (sprites == null) {
            // trainer: orange stripe, dark sole
            val d = unit(toe - ankle)
            var down = rot90(d)
            if (down.y > 0f) down = Pt(-down.x, -down.y)
            drawLine(
                SOLE.copy(alpha = if (near) 1f else 0.8f),
                s(ankle + down * (0.028f * q * tk) + d * 0.004f), s(toe + down * (0.021f * q * tk)),
                0.011f * sc, StrokeCap.Round
            )
            drawLine(
                BAND.copy(alpha = if (near) 1f else 0.75f), s(ankle + d * 0.012f + down * (0.004f * q)), s(toe - d * 0.006f + down * (0.002f * q)),
                0.0085f * sc, StrokeCap.Round
            )
        }
        // shorts: dark training shorts with an orange side stripe
        val tone = if (near) SHORTS_N else SHORTS_F
        val shortsLen = if (sprites != null) 0.38f else 0.32f
        drawSegs(listOf(Seg(sk.hip - nFront * 0.034f, sk.hip - nFront * 0.034f, 0.066f * q, 0.066f * q, tone))) // glute: a round rear like the reference
        val shorts = Seg(sk.hip - (knee - sk.hip) * 0.04f, sk.hip + (knee - sk.hip) * shortsLen, 0.064f * q, 0.056f * q, tone)
        drawSegs(listOf(shorts))
        var sn = rot90(unit(knee - sk.hip))
        if (!facesLight(sn)) sn = Pt(-sn.x, -sn.y)
        drawLine(
            (if (sprites != null) Color.White.copy(alpha = if (near) 0.28f else 0.16f) else BAND.copy(alpha = if (near) 0.95f else 0.7f)),
            s(sk.hip + (knee - sk.hip) * 0.02f + sn * (0.025f * q)), s(sk.hip + (knee - sk.hip) * (shortsLen - 0.02f) + sn * (0.021f * q)),
            0.0075f * sc, StrokeCap.Round
        )
        limbGlow(near)
    }

    fun arm(near: Boolean) {
        val elbow = if (near) sk.elbowN else sk.elbowF
        val wrist = if (near) sk.wristN else sk.wristF
        val q = if (near) 1f else 0.92f
        val skin = if (near) SKIN_N else SKIN_F
        val ua = unit(elbow - sk.shoulder)
        val fa = unit(wrist - elbow)
        // biceps sits on the side facing the chest; the forearm keeps the same rotational sense
        val sgn = if (dot(rot90(ua), nFront) >= 0f) 1f else -1f
        val fsUpper = fsFor(sk.shoulder, elbow, rot90(ua) * sgn)
        val fsFore = fsFor(elbow, wrist, rot90(fa) * sgn)
        val delt = Seg(sk.shoulder - ua * 0.012f + nFront * 0.004f, sk.shoulder + ua * 0.062f, 0.057f * q, 0.049f * q, skin)
        val upper = Seg(sk.shoulder + ua * 0.02f, elbow, 0.043f * q, 0.032f * q, skin, bulgeF = 0.025f * q, bulgeB = 0.019f * q, at = 0.40f, fs = fsUpper)
        val fore = Seg(elbow, wrist, 0.035f * q, 0.021f * q, skin, bulgeF = 0.009f * q, bulgeB = 0.015f * q, at = 0.20f, fs = fsFore)
        val fist = Seg(wrist, wrist + fa * 0.014f, 0.030f * q, 0.028f * q, skin)
        if (sprites != null) {
            val f = if (near) null else FAR_LIMB_FILTER
            val uLen = hypot(elbow.x - sk.shoulder.x, elbow.y - sk.shoulder.y)
            val fLen = hypot(wrist.x - elbow.x, wrist.y - elbow.y)
            drawCircle(if (near) ARM_FILL else ARM_FILL_FAR, 0.036f * q * sc, s(elbow))
            drawSprite(sprites.upperArm, s(sk.shoulder), s(elbow), uLen * sc / sprites.upperArm.boneLenPx, f)
            drawSprite(sprites.forearm, s(elbow), s(wrist), fLen * sc / sprites.forearm.boneLenPx, f)
        } else {
            if (near) castShadow(listOf(delt, upper, fore, fist), 0.20f)
            drawSegs(listOf(delt, upper, fore, fist))
            if (near) { sheen(upper); sheen(fore, 0.26f); sheen(delt, 0.28f); rimLight(upper); rimLight(fore) }
            limbLine(sk.shoulder, elbow, 0.22f, 0.76f, 0.017f * q)
            limbLine(elbow, wrist, 0.10f, 0.50f, 0.010f * q, 0.30f)
            // veins on the forearm and over the biceps
            limbLine(elbow, wrist, 0.18f, 0.88f, 0.004f * q, 0.28f, 0.0030f, VEIN)
            limbLine(sk.shoulder, elbow, 0.34f, 0.80f, 0.003f * q, 0.22f, 0.0028f, VEIN)
            // delt / arm separation
            val dn = rot90(ua)
            curve(
                sk.shoulder + ua * 0.054f + dn * (0.042f * q), sk.shoulder + ua * 0.066f, sk.shoulder + ua * 0.054f - dn * (0.042f * q),
                0.0055f, 0.32f
            )
            // fingers
            val fp = rot90(fa)
            for (o in floatArrayOf(-0.010f, 0.0f, 0.010f)) {
                drawLine(
                    CREASE.copy(alpha = 0.55f), s(wrist + fa * 0.006f + fp * o), s(wrist + fa * 0.026f + fp * o),
                    0.0042f * sc, StrokeCap.Round
                )
            }
        }
        limbGlow(near)
    }

    // ---- draw order: far limbs -> torso -> near leg -> head -> near arm ------------------------
    leg(false)
    arm(false)

    // Torso: outline, light-to-shadow gradient, glow, then the muscle-definition creases.
    if (sprites != null) {
        drawSprite(sprites.torso, s(sk.hip), s(sk.shoulder), TORSO * sc / sprites.torso.boneLenPx)
    } else run {
        val c = s(spineAt(0.55f))
        val hw = 0.12f * sc
        val bodyBrush = Brush.linearGradient(
            colorStops = arrayOf(0f to SKIN_N.light, 0.38f to SKIN_N.base, 1f to SKIN_N.dark),
            start = Offset(c.x + LIGHT.x * hw, c.y + LIGHT.y * hw),
            end = Offset(c.x - LIGHT.x * hw, c.y - LIGHT.y * hw)
        )
        drawPath(torsoPath(0f, 1f, tk * 1.04f, edge), INK)
        drawPath(torsoPath(0f, 1f, tk * 1.04f), bodyBrush)
    }
    torsoGlow()
    if (sprites == null) run {
        val k = tk * 1.04f
        // pec: a curved underline with a soft shadow beneath it
        curve(frontPt(0.665f, 0.98f * k), frontPt(0.700f, 0.55f * k), frontPt(0.735f, 0.14f * k), 0.012f, 0.16f)
        curve(frontPt(0.66f, 0.98f * k), frontPt(0.695f, 0.55f * k), frontPt(0.73f, 0.14f * k), 0.0070f, 0.52f)
        // upper-chest highlight
        drawLine(
            Color.White.copy(alpha = 0.24f), s(frontPt(0.72f, 0.78f * k)), s(frontPt(0.85f, 0.70f * k)),
            0.018f * sc, StrokeCap.Round
        )
        // abs: raised blocks (light cap + crease below)
        for (t in floatArrayOf(0.13f, 0.215f, 0.30f, 0.385f)) {
            drawLine(
                Color.White.copy(alpha = 0.13f), s(frontPt(t + 0.020f, 0.78f * k)), s(frontPt(t + 0.020f, 0.45f * k)),
                0.012f * sc, StrokeCap.Round
            )
            curve(frontPt(t, 0.97f * k), frontPt(t, 0.70f * k), frontPt(t + 0.005f, 0.40f * k), 0.0058f, 0.58f)
        }
        // lat sweep and trapezius slope on the back
        curve(backPt(0.80f, 0.30f * k), backPt(0.58f, 0.95f * k), backPt(0.38f, 0.80f * k), 0.0068f, 0.55f)
        curve(backPt(0.97f, 0.10f * k), backPt(0.90f, 0.80f * k), backPt(0.80f, 0.92f * k), 0.0060f, 0.32f)
    }

    // Shorts waistband over the pelvis, with an accent stripe.
    run {
        val k = tk * 1.06f
        val c = s(spineAt(0.07f))
        val hw = 0.08f * sc
        val br = Brush.linearGradient(
            colorStops = arrayOf(0f to SHORTS_N.light, 0.4f to SHORTS_N.base, 1f to SHORTS_N.dark),
            start = Offset(c.x + LIGHT.x * hw, c.y + LIGHT.y * hw), end = Offset(c.x - LIGHT.x * hw, c.y - LIGHT.y * hw)
        )
        drawPath(torsoPath(0f, 0.15f, k, edge), INK)
        drawPath(torsoPath(0f, 0.15f, k), br)
        drawPath(torsoPath(0.118f, 0.142f, k), BAND.copy(alpha = 0.95f))
    }

    leg(true)

    // ---- neck, trapezius and head ------------------------------------------------------------
    val up = unit(sk.head - sk.shoulder)
    val face = Pt(up.y, -up.x)
    val hs = 0.96f // head scale: bodybuilders carry a comparatively small head on a huge frame
    val lift = 0.024f // draw the head a little higher than the skeleton point so a real neck shows above the shoulder
    fun hw(f: Float, u: Float) = sk.head + up * lift + face * (f * hs) + up * (u * hs)
    fun hp(f: Float, u: Float) = s(hw(f, u))

    if (sprites != null) {
        drawSprite(sprites.head, s(sk.shoulder), s(sk.head), HEAD_SPRITE_WORLD_PER_PX * sc)
    } else {
        run {
            val neckSeg = Seg(spineAt(0.92f), sk.head + up * (lift - 0.040f), 0.064f, 0.046f, SKIN_N)
            val traps = Seg(spineAt(0.94f), sk.shoulder, 0.052f, 0.054f, SKIN_N)
            drawSegs(listOf(neckSeg, traps))
            // sternocleidomastoid + trapezius lines
            curve(hw(-0.012f, -0.050f), hw(0.004f, -0.074f), spineAt(0.97f) + frontAt(0.97f) * 0.014f, 0.0060f, 0.34f)
            curve(hw(-0.040f, -0.045f), spineAt(0.95f) - frontAt(0.95f) * 0.040f, sk.shoulder - frontAt(1f) * 0.020f, 0.0060f, 0.30f)
        }

        // Head: cast shadow on the neck, outlined profile, shaded fill.
        val headPts = HEAD_PROFILE.map { hp(it[0], it[1]) }
        val headPath = smoothClosed(headPts)
        translate(0.006f * sc, 0.010f * sc) { drawPath(headPath, Color.Black.copy(alpha = 0.20f)) }
        drawPath(headPath, INK, style = Stroke(width = edge * 2f * sc, join = StrokeJoin.Round))
        run {
            val c = s(sk.head + up * lift)
            val hw0 = 0.075f * sc
            drawPath(
                headPath,
                Brush.linearGradient(
                    colorStops = arrayOf(0f to SKIN_N.light, 0.40f to SKIN_N.base, 1f to SKIN_N.dark),
                    start = Offset(c.x + LIGHT.x * hw0, c.y + LIGHT.y * hw0), end = Offset(c.x - LIGHT.x * hw0, c.y - LIGHT.y * hw0)
                )
            )
        }
        // ear
        run {
            val ec = hp(-0.020f, 0.000f)
            val ew = 0.0105f * sc * hs
            val eh = 0.017f * sc * hs
            drawOval(INK, Offset(ec.x - ew - edge * sc, ec.y - eh - edge * sc), Size((ew + edge * sc) * 2, (eh + edge * sc) * 2))
            drawOval(SKIN_N.base, Offset(ec.x - ew, ec.y - eh), Size(ew * 2, eh * 2))
            drawOval(SKIN_N.dark.copy(alpha = 0.55f), Offset(ec.x - ew * 0.55f, ec.y - eh * 0.60f), Size(ew * 1.1f, eh * 1.2f))
        }
        // jaw shadow, cheekbone light, brow-ridge shadow
        drawLine(SKIN_N.dark.copy(alpha = 0.34f), hp(-0.022f, -0.046f), hp(0.046f, -0.062f), 0.013f * sc, StrokeCap.Round)
        drawLine(CREASE.copy(alpha = 0.30f), hp(0.030f, 0.022f), hp(0.056f, 0.019f), 0.011f * sc, StrokeCap.Round)
        // eye: almond, iris, lid line
        run {
            val p0 = hp(0.031f, 0.011f)
            val p1 = hp(0.051f, 0.011f)
            val cUp = hp(0.041f, 0.0195f)
            val cDn = hp(0.041f, 0.0045f)
            drawPath(
                Path().apply { moveTo(p0.x, p0.y); quadraticBezierTo(cUp.x, cUp.y, p1.x, p1.y); quadraticBezierTo(cDn.x, cDn.y, p0.x, p0.y); close() },
                Color(0xFFF6F2EE)
            )
            drawCircle(Color(0xFF3A2412), 0.0050f * sc, hp(0.0455f, 0.0115f))
            drawCircle(Color.Black, 0.0027f * sc, hp(0.0460f, 0.0115f))
            drawCircle(Color.White, 0.0012f * sc, hp(0.0445f, 0.0130f))
            drawPath(
                Path().apply { moveTo(p0.x, p0.y); quadraticBezierTo(cUp.x, cUp.y, p1.x, p1.y) },
                INK, style = Stroke(0.0036f * sc, cap = StrokeCap.Round)
            )
        }
        // eyebrow: thick, slightly arched
        curve(hw(0.026f, 0.026f), hw(0.044f, 0.0355f), hw(0.058f, 0.025f), 0.0085f, 1f, HAIR)
        // nose: nostril + wing crease; mouth: lip line with a warmer lower lip
        drawCircle(CREASE.copy(alpha = 0.75f), 0.0030f * sc, hp(0.062f, -0.0165f))
        curve(hw(0.049f, -0.010f), hw(0.056f, -0.018f), hw(0.060f, -0.018f), 0.0035f, 0.40f)
        drawLine(Color(0xFFB45F4F).copy(alpha = 0.55f), hp(0.0555f, -0.0365f), hp(0.0585f, -0.0385f), 0.0060f * sc, StrokeCap.Round)
        drawLine(CREASE.copy(alpha = 0.85f), hp(0.0495f, -0.0318f), hp(0.0605f, -0.0318f), 0.0040f * sc, StrokeCap.Round)
        // short beard: a soft dark patch over the jaw, chin and upper lip
        run {
            val beard = arrayOf(
                floatArrayOf(-0.030f, -0.022f), floatArrayOf(0.010f, -0.027f), floatArrayOf(0.046f, -0.022f), floatArrayOf(0.060f, -0.026f),
                floatArrayOf(0.057f, -0.034f), floatArrayOf(0.060f, -0.040f), floatArrayOf(0.051f, -0.047f), floatArrayOf(0.056f, -0.058f),
                floatArrayOf(0.030f, -0.067f), floatArrayOf(-0.005f, -0.061f), floatArrayOf(-0.030f, -0.048f), floatArrayOf(-0.042f, -0.030f)
            )
            drawPath(smoothClosed(beard.map { hp(it[0], it[1]) }), HAIR.copy(alpha = 0.36f))
        }
        // short-cropped hair with a soft highlight
        run {
            val hairPath = smoothClosed(HAIR_SHAPE.map { hp(it[0], it[1]) })
            val c = s(sk.head + up * lift)
            val hw0 = 0.075f * sc
            drawPath(hairPath, INK, style = Stroke(width = edge * 2f * sc, join = StrokeJoin.Round))
            drawPath(
                hairPath,
                Brush.linearGradient(
                    colorStops = arrayOf(0f to Color(0xFF5A4030), 0.35f to HAIR, 1f to Color(0xFF120A06)),
                    start = Offset(c.x + LIGHT.x * hw0, c.y + LIGHT.y * hw0), end = Offset(c.x - LIGHT.x * hw0, c.y - LIGHT.y * hw0)
                )
            )
            drawLine(Color.White.copy(alpha = 0.14f), hp(0.000f, 0.060f), hp(-0.034f, 0.050f), 0.008f * sc, StrokeCap.Round)
        }

    }

    arm(true)
}

/** The joint whose path across the rep is longest (bar path / foot path), sampled for a dotted trail. */
internal fun MotionDef.computeTrail(): List<Pt> {
    val names = listOf("wristN", "ankleN", "kneeN", "hip", "shoulder")
    var best: List<Pt> = emptyList()
    var bestLen = 0f
    for (name in names) {
        val pts = (0 until 48).map { skeletonAt(it / 48f).point(name) }
        var len = 0f
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % pts.size]
            len += hypot(a.x - b.x, a.y - b.y)
        }
        if (len > bestLen) { bestLen = len; best = pts }
    }
    // Skip near-static holds (planks, yoga) - a trail there would just be a smudge.
    return if (bestLen < 0.14f) emptyList() else best
}

internal fun DrawScope.drawTrail(trail: List<Pt>, progress: Float, s: (Pt) -> Offset, sc: Float, color: Color) {
    if (trail.isEmpty()) return
    val n = trail.size
    val cur = ((progress % 1f) * n).toInt() % n
    for (i in 0 until n step 2) {
        // dots just behind the moving joint are brighter - reads as a comet trail
        val behind = ((cur - i) % n + n) % n
        val a = 0.30f + 0.6f * (1f - behind / n.toFloat()) * (1f - behind / n.toFloat())
        drawCircle(color.copy(alpha = a), (0.006f + 0.004f * (1f - behind / n.toFloat())) * sc, s(trail[i]))
    }
}
