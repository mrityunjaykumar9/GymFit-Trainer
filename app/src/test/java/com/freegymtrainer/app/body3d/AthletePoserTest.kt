package com.freegymtrainer.app.presentation.components.body3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AthletePoserTest {

    private fun near(a: Float, b: Float, eps: Float = 1e-3f) = abs(a - b) < eps
    private fun assertVec(expected: V3, actual: V3, eps: Float = 1e-3f) {
        assertTrue("expected $expected but was $actual", near(expected.x, actual.x, eps) && near(expected.y, actual.y, eps) && near(expected.z, actual.z, eps))
    }

    @Test
    fun fromToRotatesOneDirectionOntoAnother() {
        val q = Q.fromTo(V3.Y, V3.Z)
        assertVec(V3.Z, q.rotate(V3.Y))
        // opposite directions must still give a valid half-turn
        assertVec(-V3.X, Q.fromTo(V3.X, -V3.X).rotate(V3.X))
    }

    @Test
    fun quaternionComposesLikeMatrices() {
        val a = Q.axisAngle(V3.X, 0.7f); val b = Q.axisAngle(V3.Y, -0.4f)
        val v = V3(0.3f, -0.8f, 0.5f)
        assertVec(a.rotate(b.rotate(v)), (a * b).rotate(v))
        assertVec(v, a.conj().rotate(a.rotate(v)))
    }

    @Test
    fun frameRoundTripsThroughAxes() {
        val q = Q.frame(V3(0f, 1f, 0.2f), V3(0f, 0.1f, 1f))
        val m = composeMatrix(V3.ZERO, q, 1f)
        assertVec(q.rotate(V3.X), V3(m[0], m[1], m[2]))
        assertVec(q.rotate(V3.Y), V3(m[4], m[5], m[6]))
        assertVec(q.rotate(V3.Z), V3(m[8], m[9], m[10]))
        val back = Q.fromMatrix(m)
        assertVec(q.rotate(V3(1f, 2f, 3f)), back.rotate(V3(1f, 2f, 3f)))
    }

    @Test
    fun twoBoneIkReachesTargetAndKeepsBoneLengths() {
        val root = V3(0f, 1f, 0f)
        val target = V3(0.3f, 0.45f, 0.2f)
        val (mid, end) = AthletePoser.twoBoneIk(root, target, 0.45f, 0.43f, root + V3(0f, 0f, 1f))
        assertVec(target, end)
        assertEquals(0.45f, (mid - root).len(), 1e-3f)
        assertEquals(0.43f, (end - mid).len(), 1e-3f)
        // the mid joint bends toward the pole (+Z side)
        assertTrue(mid.z > (root.z + target.z) / 2f)
    }

    @Test
    fun twoBoneIkClampsWhenTargetIsOutOfReach() {
        val root = V3.ZERO
        val (mid, end) = AthletePoser.twoBoneIk(root, V3(0f, -5f, 0f), 0.4f, 0.4f, V3(0f, 0f, 1f))
        assertEquals(0.8f, end.len(), 5e-3f)
        assertEquals(0.4f, mid.len(), 1e-3f)
        assertTrue("end stays on the line toward the target", end.y < 0f && abs(end.x) < 1e-3f)
    }
}
