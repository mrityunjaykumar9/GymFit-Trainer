package com.freegymtrainer.app.presentation.components.motion

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.hypot

/*
 * Photographic body parts (the PNGs in assets/body, cut out of a reference photo with transparent
 * backgrounds) that are rotated onto the joint skeleton like a cut-out puppet. If the files are
 * missing or fail to decode, BodybuilderFigure falls back to the fully vector-drawn athlete.
 * The parts are produced by tools/body_sprites/body_sprites.py.
 */

/** [p0]/[p1] are two reference points (pivot and a second point defining the bone axis) in bitmap pixels. */
internal class Sprite(val bmp: ImageBitmap, val p0: Offset, val p1: Offset) {
    val boneLenPx: Float get() = hypot(p1.x - p0.x, p1.y - p0.y)
}

internal class BodySprites(
    val head: Sprite,      // p0 = neck base, p1 = head centre
    val torso: Sprite,     // p0 = hip, p1 = shoulder (spine top)
    val upperArm: Sprite,  // shoulder -> elbow
    val forearm: Sprite,   // elbow -> fist centre (the skeleton's wrist)
    val thigh: Sprite,     // hip -> knee
    val shin: Sprite,      // knee -> ankle
    val foot: Sprite       // ankle -> toe tip
)

internal object BodySpriteLibrary {
    @Volatile private var cache: BodySprites? = null
    @Volatile private var failed = false

    /** Decodes the sprites once (a few hundred KB); returns null if they are unavailable. */
    fun get(context: Context): BodySprites? {
        cache?.let { return it }
        if (failed) return null
        synchronized(this) {
            cache?.let { return it }
            return try {
                val assets = context.applicationContext.assets
                val meta = JSONObject(assets.open("body/meta.json").bufferedReader().use { it.readText() })
                fun load(name: String, k0: String, k1: String): Sprite {
                    val bmp = assets.open("body/$name.png").use { BitmapFactory.decodeStream(it) }.asImageBitmap()
                    val m = meta.getJSONObject(name)
                    fun pt(k: String) = m.getJSONArray(k).let { Offset(it.getDouble(0).toFloat(), it.getDouble(1).toFloat()) }
                    return Sprite(bmp, pt(k0), pt(k1))
                }
                val sprites = BodySprites(
                    head = load("head", "neck", "center"),
                    torso = load("torso", "hip", "shoulder"),
                    upperArm = load("upper_arm", "shoulder", "elbow"),
                    forearm = load("forearm", "elbow", "fist"),
                    thigh = load("thigh", "hip", "knee"),
                    shin = load("shin", "knee", "ankle"),
                    foot = load("foot", "ankle", "toe")
                )
                cache = sprites
                sprites
            } catch (e: Exception) {
                failed = true
                null
            }
        }
    }
}

/** Darkens a sprite for limbs on the far side of the body. */
internal val FAR_LIMB_FILTER: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.60f, 0f, 0f, 0f, 0f,
            0f, 0.58f, 0f, 0f, 0f,
            0f, 0f, 0.58f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

/**
 * Draws [sp] with its p0 pinned at [a] and its p0->p1 axis rotated to point at [b]. [scale] is screen
 * pixels per sprite pixel (the caller derives it from the skeleton's fixed bone length so the part
 * does not stretch when a limb is foreshortened or the spine bends).
 */
internal fun DrawScope.drawSprite(sp: Sprite, a: Offset, b: Offset, scale: Float, filter: ColorFilter? = null, alpha: Float = 1f) {
    val srcAng = atan2(sp.p1.y - sp.p0.y, sp.p1.x - sp.p0.x)
    val dstAng = atan2(b.y - a.y, b.x - a.x)
    withTransform({
        translate(a.x, a.y)
        rotate(Math.toDegrees((dstAng - srcAng).toDouble()).toFloat(), Offset.Zero)
        scale(scale, scale, Offset.Zero)
    }) {
        drawImage(sp.bmp, Offset(-sp.p0.x, -sp.p0.y), alpha = alpha, colorFilter = filter)
    }
}
