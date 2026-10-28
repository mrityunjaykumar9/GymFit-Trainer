package com.freegymtrainer.app.presentation.components.body3d

import com.freegymtrainer.app.presentation.components.motion.Skeleton
import com.freegymtrainer.app.presentation.components.motion.Pt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * Lifts the side-view joint skeleton (the same key-pose data that drives the 2D animation, plus the
 * optional sideways information in [com.freegymtrainer.app.presentation.components.motion.Lateral])
 * into 3D joint positions. AthletePoser then turns those into bone rotations for the skinned model.
 * Units: a standing figure is ~1.0 tall, +x is the way it faces, +y up, +z the near side.
 */

internal const val SHOULDER_HALF = 0.185f   // half the distance between the shoulder joints
internal const val HIP_HALF = 0.090f

/** Where the hands / feet go sideways when an exercise doesn't say (pose units): about shoulder / hip width of the 3D model. */
private const val DEFAULT_GRIP = 0.135f
private const val DEFAULT_STANCE = 0.075f

/** 3D joint positions. "N" is the near side (+z), "F" the far side (-z). */
internal class Pose3D(
    val hip: V3, val neckBase: V3, val head: V3,
    val up: V3, val fwd: V3, val spineBulge: Float,
    val shoulderN: V3, val elbowN: V3, val wristN: V3,
    val shoulderF: V3, val elbowF: V3, val wristF: V3,
    val hipN: V3, val kneeN: V3, val ankleN: V3, val toeN: V3,
    val hipF: V3, val kneeF: V3, val ankleF: V3, val toeF: V3
)

private fun rad(deg: Float) = Math.toRadians(deg.toDouble()).toFloat()

/** Lifts the side-view [Skeleton] into 3D using its sideways data (arm / leg swing, grip, stance). */
internal fun Skeleton.toPose3D(): Pose3D {
    val lat = lateral
    val hip3 = V3(hip.x, hip.y, 0f)
    val neck3 = V3(shoulder.x, shoulder.y, 0f)
    val up = (neck3 - hip3).unit()
    val tr = rad(torso)
    val fwd = V3(cos(tr), -sin(tr), 0f)

    fun arm(side: Float, elbow2: Pt, wrist2: Pt, phiDeg: Float): Triple<V3, V3, V3> {
        val sh = V3(shoulder.x, shoulder.y, side * SHOULDER_HALF)
        if (lat.grip <= 0f && abs(phiDeg) > 0.5f) {
            // free swing out to the side (or in, if negative): the planar vectors turn toward the side by phi
            val c = cos(rad(phiDeg)); val s = sin(rad(phiDeg))
            val ux = elbow2.x - shoulder.x; val uy = elbow2.y - shoulder.y
            val fx = wrist2.x - elbow2.x; val fy = wrist2.y - elbow2.y
            val elbow = sh + V3(ux * c, uy * c, side * hypot(ux, uy) * s)
            val wrist = elbow + V3(fx * c, fy * c, side * hypot(fx, fy) * s)
            return Triple(sh, elbow, wrist)
        }
        val gripZ = side * (if (lat.grip > 0f) lat.grip else DEFAULT_GRIP)
        val flare = side * sin(rad(phiDeg)) * 0.075f
        val elbowZ = side * SHOULDER_HALF + (gripZ - side * SHOULDER_HALF) * 0.5f + flare
        return Triple(sh, V3(elbow2.x, elbow2.y, elbowZ), V3(wrist2.x, wrist2.y, gripZ))
    }

    fun leg(side: Float, knee2: Pt, ankle2: Pt, toe2: Pt, phiDeg: Float): Quad {
        val hj = V3(hip.x, hip.y, side * HIP_HALF)
        if (lat.stance <= 0f && abs(phiDeg) > 0.5f) {
            val c = cos(rad(phiDeg)); val s = sin(rad(phiDeg))
            val tx = knee2.x - hip.x; val ty = knee2.y - hip.y
            val sx = ankle2.x - knee2.x; val sy = ankle2.y - knee2.y
            val knee = hj + V3(tx * c, ty * c, side * hypot(tx, ty) * s)
            val ankle = knee + V3(sx * c, sy * c, side * hypot(sx, sy) * s)
            val toe = ankle + V3(toe2.x - ankle2.x, toe2.y - ankle2.y, side * 0.02f)
            return Quad(hj, knee, ankle, toe)
        }
        val az = side * (if (lat.stance > 0f) lat.stance else DEFAULT_STANCE)
        val kz = side * HIP_HALF + (az - side * HIP_HALF) * 0.5f
        return Quad(hj, V3(knee2.x, knee2.y, kz), V3(ankle2.x, ankle2.y, az), V3(toe2.x, toe2.y, az + side * 0.012f))
    }

    val (shN, elN, wrN) = arm(1f, elbowN, wristN, lateral.armN)
    val (shF, elF, wrF) = arm(-1f, elbowF, wristF, lateral.armF)
    val ln = leg(1f, kneeN, ankleN, toeN, lateral.legN)
    val lf = leg(-1f, kneeF, ankleF, toeF, lateral.legF)
    return Pose3D(
        hip3, neck3, V3(head.x, head.y, 0f), up, fwd, spine,
        shN, elN, wrN, shF, elF, wrF,
        ln.a, ln.b, ln.c, ln.d, lf.a, lf.b, lf.c, lf.d
    )
}

/** The same pose raised by [dy] (pose units), e.g. to lift a hand or foot that would sink through the floor. */
internal fun Pose3D.lifted(dy: Float): Pose3D {
    val d = V3(0f, dy, 0f)
    return Pose3D(
        hip + d, neckBase + d, head + d, up, fwd, spineBulge,
        shoulderN + d, elbowN + d, wristN + d, shoulderF + d, elbowF + d, wristF + d,
        hipN + d, kneeN + d, ankleN + d, toeN + d, hipF + d, kneeF + d, ankleF + d, toeF + d
    )
}

/** The same pose with the head point replaced (e.g. tipped up so the skull doesn't sink into the floor). */
internal fun Pose3D.withHead(h: V3) = Pose3D(
    hip, neckBase, h, up, fwd, spineBulge,
    shoulderN, elbowN, wristN, shoulderF, elbowF, wristF,
    hipN, kneeN, ankleN, toeN, hipF, kneeF, ankleF, toeF
)

/** The same pose with the body turned over (chest down instead of up), for exercises done prone like the leg curl. */
internal fun Pose3D.flippedFacing() = Pose3D(
    hip, neckBase, head, up, fwd * -1f, spineBulge,
    shoulderN, elbowN, wristN, shoulderF, elbowF, wristF,
    hipN, kneeN, ankleN, toeN, hipF, kneeF, ankleF, toeF
)

private class Quad(val a: V3, val b: V3, val c: V3, val d: V3)
