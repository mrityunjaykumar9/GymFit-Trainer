package com.freegymtrainer.app.presentation.components.body3d

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Tiny vector / quaternion toolkit for posing the 3D athlete. Immutable value classes: the pose
 * solver runs for ~65 bones once per frame, so allocation is a non-issue.
 */

internal class V3(@JvmField val x: Float, @JvmField val y: Float, @JvmField val z: Float) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Float) = V3(x * k, y * k, z * k)
    operator fun unaryMinus() = V3(-x, -y, -z)
    fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    fun cross(o: V3) = V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun len() = sqrt(x * x + y * y + z * z)
    fun unit(fallback: V3 = Y): V3 { val l = len(); return if (l < 1e-6f) fallback else V3(x / l, y / l, z / l) }
    override fun toString() = "(%.3f, %.3f, %.3f)".format(x, y, z)

    companion object {
        val ZERO = V3(0f, 0f, 0f)
        val X = V3(1f, 0f, 0f)
        val Y = V3(0f, 1f, 0f)
        val Z = V3(0f, 0f, 1f)
        fun lerp(a: V3, b: V3, t: Float) = V3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t)
    }
}

/** Unit quaternion (x, y, z, w), the same component order glTF and Filament use. */
internal class Q(@JvmField val x: Float, @JvmField val y: Float, @JvmField val z: Float, @JvmField val w: Float) {
    /** Hamilton product: applying (this * o) rotates by [o] first, then by this. */
    operator fun times(o: Q) = Q(
        w * o.x + x * o.w + y * o.z - z * o.y,
        w * o.y - x * o.z + y * o.w + z * o.x,
        w * o.z + x * o.y - y * o.x + z * o.w,
        w * o.w - x * o.x - y * o.y - z * o.z
    )

    fun conj() = Q(-x, -y, -z, w)

    fun normalized(): Q {
        val l = sqrt(x * x + y * y + z * z + w * w)
        return if (l < 1e-9f) IDENTITY else Q(x / l, y / l, z / l, w / l)
    }

    fun rotate(v: V3): V3 {
        // v' = v + 2w(q x v) + 2 q x (q x v)
        val qv = V3(x, y, z)
        val t = qv.cross(v) * 2f
        return v + t * w + qv.cross(t)
    }

    /** Shortest-arc blend, used to ease key poses. */
    fun nlerp(o: Q, t: Float): Q {
        val s = if (x * o.x + y * o.y + z * o.z + w * o.w < 0f) -1f else 1f
        return Q(x + (o.x * s - x) * t, y + (o.y * s - y) * t, z + (o.z * s - z) * t, w + (o.w * s - w) * t).normalized()
    }

    companion object {
        val IDENTITY = Q(0f, 0f, 0f, 1f)

        fun axisAngle(axis: V3, rad: Float): Q {
            val a = axis.unit()
            val s = sin(rad / 2f)
            return Q(a.x * s, a.y * s, a.z * s, cos(rad / 2f))
        }

        /** Smallest rotation that takes direction [a] onto direction [b]. */
        fun fromTo(a: V3, b: V3): Q {
            val u = a.unit(); val v = b.unit()
            val d = u.dot(v)
            if (d > 0.999999f) return IDENTITY
            if (d < -0.999999f) {
                var axis = V3.X.cross(u)
                if (axis.len() < 1e-3f) axis = V3.Y.cross(u)
                return axisAngle(axis, Math.PI.toFloat())
            }
            val c = u.cross(v)
            return Q(c.x, c.y, c.z, 1f + d).normalized()
        }

        /** Rotation whose columns are the given orthonormal axes (new x, y, z axes expressed in the old frame). */
        fun fromAxes(ax: V3, ay: V3, az: V3): Q {
            val m00 = ax.x; val m10 = ax.y; val m20 = ax.z
            val m01 = ay.x; val m11 = ay.y; val m21 = ay.z
            val m02 = az.x; val m12 = az.y; val m22 = az.z
            val tr = m00 + m11 + m22
            return if (tr > 0f) {
                val s = sqrt(tr + 1f) * 2f
                Q((m21 - m12) / s, (m02 - m20) / s, (m10 - m01) / s, 0.25f * s)
            } else if (m00 > m11 && m00 > m22) {
                val s = sqrt(1f + m00 - m11 - m22) * 2f
                Q(0.25f * s, (m01 + m10) / s, (m02 + m20) / s, (m21 - m12) / s)
            } else if (m11 > m22) {
                val s = sqrt(1f + m11 - m00 - m22) * 2f
                Q((m01 + m10) / s, 0.25f * s, (m12 + m21) / s, (m02 - m20) / s)
            } else {
                val s = sqrt(1f + m22 - m00 - m11) * 2f
                Q((m02 + m20) / s, (m12 + m21) / s, 0.25f * s, (m10 - m01) / s)
            }.normalized()
        }

        /**
         * Orientation of a body part with its "up" along [up] and its front toward [front] (made perpendicular
         * to up). A rest frame and a target frame built the same way give a clean difference rotation.
         */
        fun frame(up: V3, front: V3): Q {
            val u = up.unit()
            var f = front - u * front.dot(u)
            if (f.len() < 1e-4f) f = V3.Z - u * V3.Z.dot(u)
            if (f.len() < 1e-4f) f = V3.X - u * V3.X.dot(u)
            f = f.unit()
            val r = u.cross(f)
            return fromAxes(r, u, f)
        }

        /** Rotation from a column-major 4x4 matrix (scale is divided out of the columns). */
        fun fromMatrix(m: FloatArray): Q {
            fun col(c: Int) = V3(m[c * 4], m[c * 4 + 1], m[c * 4 + 2])
            val sx = col(0).len().coerceAtLeast(1e-6f); val sy = col(1).len().coerceAtLeast(1e-6f); val sz = col(2).len().coerceAtLeast(1e-6f)
            return fromAxes(col(0) * (1f / sx), col(1) * (1f / sy), col(2) * (1f / sz))
        }
    }
}

/** Column-major 4x4 matrix (what Filament's TransformManager takes) from translation, rotation and uniform scale. */
internal fun composeMatrix(t: V3, q: Q, s: Float, out: FloatArray = FloatArray(16)): FloatArray {
    val x = q.x; val y = q.y; val z = q.z; val w = q.w
    out[0] = (1f - 2f * (y * y + z * z)) * s; out[1] = 2f * (x * y + z * w) * s; out[2] = 2f * (x * z - y * w) * s; out[3] = 0f
    out[4] = 2f * (x * y - z * w) * s; out[5] = (1f - 2f * (x * x + z * z)) * s; out[6] = 2f * (y * z + x * w) * s; out[7] = 0f
    out[8] = 2f * (x * z + y * w) * s; out[9] = 2f * (y * z - x * w) * s; out[10] = (1f - 2f * (x * x + y * y)) * s; out[11] = 0f
    out[12] = t.x; out[13] = t.y; out[14] = t.z; out[15] = 1f
    return out
}

/** Like [composeMatrix] with a different scale along each local axis (used to stretch props). */
internal fun composeMatrix(t: V3, q: Q, sx: Float, sy: Float, sz: Float, out: FloatArray = FloatArray(16)): FloatArray {
    composeMatrix(t, q, 1f, out)
    for (i in 0..2) { out[i] *= sx; out[4 + i] *= sy; out[8 + i] *= sz }
    return out
}

internal fun clamp01(v: Float) = max(0f, min(1f, v))

internal fun angleBetween(a: V3, b: V3): Float = acos(a.unit().dot(b.unit()).coerceIn(-1f, 1f))

internal fun nearlyZero(v: Float) = abs(v) < 1e-5f
