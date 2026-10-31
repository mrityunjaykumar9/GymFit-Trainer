package com.freegymtrainer.app.presentation.components.body3d

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Turns the 3D joint positions of an exercise key pose (Pose3D) into bone transforms for the skinned athlete
 * model. Pure Kotlin - no Filament types - so it is unit-testable on the JVM.
 *
 * How it works, in model space (Y up, the model faces +Z, its left is +X):
 *  - the pelvis is moved/rotated so the hips sit where the pose says and the torso leans like the pose;
 *  - spine, neck and head follow the pose's torso and head angles, relative to the model's own natural curve;
 *  - arms and legs use two-bone IK: the hand / ankle goes exactly where the pose put it (so a barbell, bench or
 *    the floor really is touched) and the elbow / knee bends toward the pose's elbow / knee;
 *  - feet follow the pose's toe direction.
 * Every driven bone is a swing from its rest orientation, so twist (palms down, knees forward) stays natural.
 */

/** One bone of the model in its rest pose. [localS] is the uniform local scale. Bones must come parents-first. */
internal class RestBone(
    val name: String, val parent: String?,
    val localT: V3, val localQ: Q, val localS: Float,
    val worldQ: Q, val worldP: V3
)

/**
 * How a hand is held. Bars run side to side (along X), so [BAR_OVER] is an overhand grip (palm faces down when the
 * forearm points forward, forward when it points up) and [BAR_UNDER] an underhand one. [NEUTRAL] palms face each
 * other (hammer curl, rope, V-handle), [PALM_DOWN] palms to the floor, [FLOOR] hand flat on the mat with fingers
 * forward (the wrist bends), [AUTO] decides from where the hand is.
 */
enum class HandMode { AUTO, BAR_OVER, BAR_UNDER, NEUTRAL, PALM_DOWN, FLOOR }

/** Hand modes for the character's right (near side) and left hand, and how tightly the fingers close. */
internal class Hands(val right: HandMode, val left: HandMode, val fist: Float)

/** A bone's new local transform. */
internal class BoneLocal(@JvmField var t: V3, @JvmField var q: Q, @JvmField var s: Float)

internal class AthletePoser(private val rest: List<RestBone>) {
    private val index = HashMap<String, Int>().apply { rest.forEachIndexed { i, b -> put(b.name, i) } }
    private val parentIdx = IntArray(rest.size) { rest[it].parent?.let { p -> index[p] } ?: -1 }

    // state of the last solve()
    private val wq = Array(rest.size) { rest[it].worldQ }
    private val wp = Array(rest.size) { rest[it].worldP }
    private val cum = FloatArray(rest.size) { 1f }

    private class Limb(val mid: String, val end: String, val target: V3, val pole: V3)

    private val ikEnd = HashMap<String, V3>()
    private val footToe = HashMap<String, V3>()
    private val forearmDir = HashMap<String, V3>()
    private val resolvedFist = HashMap<String, Float>()
    private val resolvedMode = HashMap<String, HandMode>()

    /** True while solving a frontal pose (the movement plane faces the viewer): pose x maps to the body's side-to-side axis. */
    private var frontal = false

    val boneCount get() = rest.size
    fun boneName(i: Int) = rest[i].name
    fun has(name: String) = name in index

    /** World position / rotation of a bone after the last [solve]. */
    fun position(name: String): V3 = wp[index.getValue(name)]
    fun worldRotation(name: String): Q = wq[index.getValue(name)]

    private fun r(name: String) = rest[index.getValue(name)]

    /** Pose units -> model metres. The pose's near side (+z) is the character's right, which is -X in the model. */
    fun toModel(p: V3) = if (frontal) V3(p.x * SCALE, p.y * SCALE, p.z * SCALE) else V3(-p.z * SCALE, p.y * SCALE, p.x * SCALE)
    fun dirToModel(p: V3) = if (frontal) p else V3(-p.z, p.y, p.x)

    /**
     * Solves [pose] into [out] (one entry per bone, in rest-list order). [hands] says how each hand is held.
     */
    fun solve(pose: Pose3D, out: Array<BoneLocal>, hands: Hands = Hands(HandMode.AUTO, HandMode.AUTO, 0.2f), frontal: Boolean = false) {
        this.frontal = frontal
        val hipM = toModel(pose.hip)
        val neckM = toModel(pose.neckBase)
        val headM = toModel(pose.head)
        val fwdM = if (frontal) V3.Z else dirToModel(pose.fwd).unit(V3.Z)

        // --- torso: lean follows the pose, relative to the model's natural stance
        val pelvisR = r("pelvis"); val neckR = r("neck_01")
        val up0 = (neckR.worldP - pelvisR.worldP).unit()
        val up1 = (neckM - hipM).unit()
        val qTorso = Q.frame(up1, fwdM) * Q.frame(up0, V3.Z).conj()
        val lateral = up1.cross(fwdM).unit(V3.X)          // rotating about this tips "up" toward "front"
        val torsoLen = (neckM - hipM).len().coerceAtLeast(0.2f)
        val spanRest = (neckR.worldP - pelvisR.worldP).dot(up0)
        val bulge = pose.spineBulge * SCALE

        val desired = arrayOfNulls<Q>(rest.size)
        desired[index.getValue("pelvis")] = qTorso * pelvisR.worldQ
        for (name in SPINE) {
            val b = r(name)
            val f = ((b.worldP - pelvisR.worldP).dot(up0) / spanRest).coerceIn(0f, 1f)
            // slope of the forward bulge curve 4*B*t*(1-t)
            val ang = atan2(4f * bulge * (1f - 2f * f), torsoLen)
            desired[index.getValue(name)] = Q.axisAngle(lateral, ang) * qTorso * b.worldQ
        }

        // --- head and neck tip by the pose's head angle relative to the torso line
        val headDelta = Q.fromTo(up1, (headM - neckM).unit(up1))
        desired[index.getValue("neck_01")] = Q.IDENTITY.nlerp(headDelta, 0.5f) * qTorso * neckR.worldQ
        desired[index.getValue("Head")] = headDelta * qTorso * r("Head").worldQ

        // --- limb targets. Near side = the character's right.
        val limbs = HashMap<String, Limb>()
        // in a frontal pose the authored points sit on the body's centre line; the near (+x) limbs belong a shoulder / hip
        // width to the +x side and the far ones to the -x side
        fun shifted(p: V3, dx: Float) = if (frontal) p + V3(dx, 0f, 0f) else p
        fun arm(side: String, wrist: V3, elbow: V3) {
            val dx = if (side == "l") FRONTAL_ARM_X else -FRONTAL_ARM_X
            limbs["upperarm_$side"] = Limb("lowerarm_$side", "hand_$side", shifted(toModel(wrist), dx), shifted(toModel(elbow), dx))
        }
        fun leg(side: String, ankle: V3, knee: V3, toe: V3) {
            val dx = if (side == "l") FRONTAL_LEG_X else -FRONTAL_LEG_X
            limbs["thigh_$side"] = Limb("calf_$side", "foot_$side", shifted(toModel(ankle), dx), shifted(toModel(knee), dx))
            footToe[side] = if (frontal) {
                // front foot points along the stance line, the back foot turns toward the viewer, a centred foot points forward
                if (ankle.x - pose.hip.x > 0.12f) V3.X else V3.Z
            } else toModel(toe) - toModel(ankle)
        }
        // normally the near side is the character's right; in a frontal pose "near" is the +x side = the character's left
        val nearSide = if (frontal) "l" else "r"
        val farSide = if (frontal) "r" else "l"
        arm(nearSide, pose.wristN, pose.elbowN); arm(farSide, pose.wristF, pose.elbowF)
        leg(nearSide, pose.ankleN, pose.kneeN, pose.toeN); leg(farSide, pose.ankleF, pose.kneeF, pose.toeF)
        ikEnd.clear(); forearmDir.clear(); resolvedFist.clear(); resolvedMode.clear()

        // --- forward kinematics with overrides, parents first
        for (i in rest.indices) {
            val b = rest[i]
            val p = parentIdx[i]
            val localScale = scaleOf(b.name, b.localS)
            if (p < 0) {
                wq[i] = b.worldQ; wp[i] = b.worldP; cum[i] = localScale
                out[i].t = b.localT; out[i].q = b.localQ; out[i].s = localScale
                continue
            }
            val pos: V3
            var worldQ = desired[i]
            if (b.name == "pelvis") {
                // the midpoint between the thigh joints goes to the pose's hip point
                val mid = (r("thigh_l").localT + r("thigh_r").localT) * 0.5f
                pos = hipM - worldQ!!.rotate(mid * (cum[p] * localScale))
            } else {
                pos = wp[p] + wq[p].rotate(b.localT * cum[p])
            }

            val limb = limbs[b.name]
            if (limb != null) {
                // root of an IK limb (upper arm / thigh): swing toward the solved mid joint
                val own = cum[p] * localScale
                val l1 = r(limb.mid).localT.len() * own
                val l2 = r(limb.end).localT.len() * own * scaleOf(limb.mid, r(limb.mid).localS)
                val (mid, end) = twoBoneIk(pos, limb.target, l1, l2, limb.pole)
                worldQ = Q.fromTo((r(limb.mid).worldP - b.worldP).unit(), mid - pos) * b.worldQ
                ikEnd[limb.mid] = end
            } else if (worldQ == null && ikEnd.containsKey(b.name)) {
                // mid bone (forearm / shin): aim from its final joint position to the IK end point
                val end = ikEnd.getValue(b.name)
                val isArm = b.name.startsWith("lowerarm_")
                val child = r(if (isArm) "hand_" + b.name.substring(9) else "foot_" + b.name.substring(5))
                var q = Q.fromTo((child.worldP - b.worldP).unit(), end - pos) * b.worldQ
                if (isArm) {
                    // forearm roll: turn it toward the palm orientation this grip needs (the hand takes the rest)
                    val side = b.name.substring(9)
                    val d2 = (end - pos).unit()
                    forearmDir[side] = d2
                    val (_, pn) = handTarget(side, d2, end, hands)
                    val follow = q * child.localQ
                    val now = flat(follow.rotate(palmLocal(side)), d2)
                    val want = flat(pn, d2)
                    if (now != null && want != null) {
                        val theta = atan2(now.cross(want).dot(d2), now.dot(want))
                        q = Q.axisAngle(d2, theta * FOREARM_TWIST_SHARE) * q
                    }
                } else {
                    // The knee is a hinge: it should never twist relative to the thigh above it, but
                    // Q.fromTo's shortest-arc rotation is recomputed fresh every frame from a fixed
                    // rest direction, with no continuity constraint on roll - on a deep bend like a
                    // leg curl that drifts and reads as the shin twisting mid-rep. Lock its roll to
                    // the thigh's own current roll instead.
                    val d2 = (end - pos).unit()
                    // "want" has to go through the bone's own rest-local-to-parent rotation first -
                    // comparing raw world axes directly ignores that the calf's and thigh's local
                    // frames aren't the same to start with, which silently no-opped the first version
                    // of this fix (the two reference vectors it compared weren't even in the same
                    // frame, so there was nothing meaningful to cancel).
                    val now = flat(q.rotate(V3.X), d2)
                    val want = flat(wq[p].rotate(b.localQ.rotate(V3.X)), d2)
                    if (now != null && want != null) {
                        val theta = atan2(now.cross(want).dot(d2), now.dot(want))
                        q = Q.axisAngle(d2, theta) * q
                    }
                }
                worldQ = q
            } else if (worldQ == null && b.name.startsWith("hand_")) {
                // the hand is oriented outright (fingers + palm direction); the wrist bends if it has to
                val side = b.name.substring(5)
                val d2 = forearmDir[side] ?: wq[p].rotate(V3.Y)
                val (f, pn) = handTarget(side, d2, pos, hands)
                val fingers = f.unit(d2)
                val palm = flat(pn, fingers) ?: flat(V3.Y.cross(fingers), fingers) ?: V3.Y
                val world = Q.fromAxes(fingers, palm, fingers.cross(palm))
                val pl = palmLocal(side)
                val local = Q.fromAxes(V3.Y, pl, V3.Y.cross(pl))
                worldQ = world * local.conj()
            } else if (worldQ == null && b.name.startsWith("foot_")) {
                val side = b.name.substring(5)
                val restDir = (r("ball_$side").worldP - b.worldP).unit()
                // on a flat foot the ankle->ball line dips below the horizontal by this angle
                val dip = atan2(-restDir.y, hypot(restDir.x, restDir.z))
                val f = (footToe[side] ?: V3.Z).unit(V3.Z)
                val n = (V3.Y - f * V3.Y.dot(f)).unit(V3.Y)
                val target = f * cos(dip) - n * sin(dip)
                var footQ = Q.fromTo(restDir, target) * b.worldQ
                // Same fix as the knee above: lock the ankle's roll to the calf's current roll
                // instead of Q.fromTo's independently-recomputed shortest arc, which was letting the
                // foot twist on its own as the leg swung through a big range (e.g. mid leg-curl).
                val now = flat(footQ.rotate(V3.X), target)
                val want = flat(wq[p].rotate(b.localQ.rotate(V3.X)), target)
                if (now != null && want != null) {
                    val theta = atan2(now.cross(want).dot(target), now.dot(want))
                    footQ = Q.axisAngle(target, theta) * footQ
                }
                worldQ = footQ
            }

            var localQ = if (worldQ != null) wq[p].conj() * worldQ else b.localQ
            if (FINGER.matches(b.name)) {
                val side = b.name.substring(b.name.length - 1)
                val fist = resolvedFist[side] ?: hands.fist
                if (fist > 0f) localQ = curl(b.name, localQ, fist)
            }
            val q = wq[p] * localQ
            wq[i] = q; wp[i] = pos; cum[i] = cum[p] * localScale
            out[i].t = if (b.name == "pelvis") wq[p].conj().rotate(pos - wp[p]) * (1f / max(cum[p], 1e-4f)) else b.localT
            out[i].q = localQ
            out[i].s = localScale
        }
    }

    private class Surf(val p: V3, val r: Float)

    private fun pointOn(name: String, offsetAtRest: V3, radius: Float): Surf? {
        val i = index[name] ?: return null
        // the offset is given in world axes at rest and carried along with the bone, so a heel stays under the heel
        return Surf(wp[i] + wq[i].rotate(rest[i].worldQ.conj().rotate(offsetAtRest)), radius)
    }

    /** Hands, feet, knees and elbows: the parts that take weight and must rest ON the floor, never in it. */
    private fun limbPoints(): List<Surf> {
        val out = ArrayList<Surf>(32)
        for (s in listOf("l", "r")) {
            pointOn("lowerarm_$s", V3.ZERO, 0.045f)?.let(out::add)
            pointOn("hand_$s", V3.ZERO, 0.04f)?.let(out::add)
            for (f in listOf("index", "middle", "ring", "pinky", "thumb")) pointOn("${f}_04_leaf_$s", V3.ZERO, 0.012f)?.let(out::add)
            pointOn("calf_$s", V3.ZERO, 0.06f)?.let(out::add)
            pointOn("foot_$s", V3(0f, -0.098f, -0.03f), 0f)?.let(out::add)      // heel (sole of the shoe)
            pointOn("ball_$s", V3(0f, -0.027f, 0f), 0f)?.let(out::add)          // ball of the foot
            pointOn("ball_leaf_$s", V3(0f, -0.024f, 0f), 0f)?.let(out::add)     // toe tip
        }
        return out
    }

    private fun trunkPoints(): List<Surf> = listOfNotNull(
        pointOn("pelvis", V3.ZERO, 0.09f), pointOn("spine_01", V3.ZERO, 0.09f),
        pointOn("spine_02", V3.ZERO, 0.10f), pointOn("spine_03", V3.ZERO, 0.10f),
        pointOn("thigh_l", V3.ZERO, 0.085f), pointOn("thigh_r", V3.ZERO, 0.085f)
    )

    /** Height of the lowest hand / foot / knee / elbow after the last [solve]. */
    fun lowestLimb(): Float = limbPoints().minOf { it.p.y - it.r }

    /** Height of the lowest part of the trunk after the last [solve]. */
    fun lowestTrunk(): Float = trunkPoints().minOf { it.p.y - it.r }

    /** Lowest point of the head (a ball around the skull). */
    fun lowestHead(): Float = pointOn("Head", V3(0f, 0.08f, 0.02f), 0.10f)?.let { it.p.y - it.r } ?: Float.MAX_VALUE

    /** Reach of the body after the last [solve]: minX, maxX, minZ, maxZ and (index 4) the highest point. */
    fun extents(into: FloatArray) {
        val all = limbPoints() + trunkPoints() + listOfNotNull(pointOn("Head", V3(0f, 0.08f, 0.02f), 0.10f))
        for (sp in all) {
            into[0] = min(into[0], sp.p.x - sp.r); into[1] = max(into[1], sp.p.x + sp.r)
            into[2] = min(into[2], sp.p.z - sp.r); into[3] = max(into[3], sp.p.z + sp.r)
            into[4] = max(into[4], sp.p.y + sp.r)
        }
    }

    /** Arms are lengthened a little: the cartoon skeleton the poses were authored on has longer arms than this model. */
    private fun scaleOf(name: String, rest: Float): Float = when {
        name.startsWith("upperarm_") -> rest * ARM_SCALE
        name.startsWith("hand_") -> rest / ARM_SCALE
        else -> rest
    }

    /** Palm direction in the hand bone's own axes (palms face down in the model's rest pose). */
    private fun palmLocal(side: String): V3 = r("hand_$side").worldQ.conj().rotate(V3(0f, -1f, 0f))

    /** The part of [v] perpendicular to [axis], normalised; null if there is (almost) none. */
    private fun flat(v: V3, axis: V3): V3? {
        val a = axis.unit()
        val w = v - a * v.dot(a)
        return if (w.len() < 0.15f * max(v.len(), 1e-6f) || w.len() < 1e-4f) null else w.unit()
    }

    /** Finger direction and palm direction (world) for the hand on [side] ("l" / "r") given its forearm direction. */
    private fun handTarget(side: String, d2: V3, wrist: V3, hands: Hands): Pair<V3, V3> {
        val left = side == "l"
        var mode = if (left) hands.left else hands.right
        if (mode == HandMode.AUTO) {
            mode = when {
                wrist.y < FLOOR_HAND_HEIGHT -> HandMode.FLOOR
                kotlin.math.abs(d2.y) < 0.6f -> HandMode.PALM_DOWN
                else -> HandMode.NEUTRAL
            }
        }
        resolvedMode[side] = mode
        resolvedFist[side] = if (mode == HandMode.FLOOR) 0f else hands.fist
        // Tilted up off pure horizontal on purpose: a few exercises (rear delt fly) swing the forearm
        // out until it's nearly horizontal itself, and a palm target exactly in that plane is a
        // degenerate target to stay perpendicular to - the hand (and the dumbbell with it) has to spin
        // fast to track it right at the end of the rep. The tilt keeps the two directions apart.
        val inward = V3(if (left) -0.8f else 0.8f, 0.45f, 0f)
        // Same reasoning as `inward` above, but for a forearm that hangs straight down instead of
        // out to the side: at the bottom of a lateral/front raise the forearm itself points almost
        // exactly along -Y, the same line as a dead-straight-down palm target, so the hand had to
        // spin to catch up once the arm swung clear of that dead spot. A small forward tilt keeps
        // the palm reading as "down" at the top (where it matters) without ever lining up with the
        // forearm at the bottom.
        // For a frontal exercise there is no sagittal/model axis swap (toModel maps pose.z straight to
        // model depth), so the usual z-axis tilt - which lands harmlessly on left-right for every other
        // PALM_DOWN exercise - sits right on the camera's depth axis instead, and the hand (and the
        // dumbbell with it) visibly rocked forward and back through the rep. Tilting on x instead (toward
        // this side's own raised position) breaks the same bottom-of-rep degenerate alignment without
        // ever moving the hand in depth.
        val palmDown = if (frontal) V3(if (left) 0.18f else -0.18f, -1f, 0f) else V3(0f, -1f, 0.18f)
        return when (mode) {
            HandMode.FLOOR -> V3.Z to V3(0f, -1f, 0f)
            HandMode.BAR_OVER -> d2 to V3.X.cross(d2)
            HandMode.BAR_UNDER -> d2 to -V3.X.cross(d2)
            HandMode.NEUTRAL -> d2 to inward
            else -> d2 to palmDown
        }
    }

    /** Which axis fingers wrap around when the hand closes, in world space (the handle direction of a dumbbell). */
    fun handleAxis(side: String): V3 {
        val pl = palmLocal(side)
        return wq[index.getValue("hand_$side")].rotate(V3.Y.cross(pl)).unit(V3.X)
    }

    private fun curl(name: String, q: Q, fist: Float): Q {
        val m = FINGER.matchEntire(name) ?: return q
        val joint = m.groupValues[2].toInt()
        if (joint !in 1..3) return q
        val thumb = m.groupValues[1] == "thumb"
        // The old thumb angle (0.45) barely closed it around a handle, leaving it stuck straight
        // out like a thumbs-up - the thumb's base joint needs more curl than the fingers' to swing
        // all the way across and wrap over them into a proper closed fist.
        val ang = fist * (if (thumb) 2.0f else 1.25f)
        return q * Q.axisAngle(V3.X, -ang)
    }

    companion object {
        /** Model height (m) over pose height (1.0). */
        const val SCALE = 1.81f
        const val ARM_SCALE = 1.2f
        /** Half the shoulder / hip joint spacing (m): where frontal-pose arms and legs sit either side of the centre line. */
        private const val FRONTAL_ARM_X = 0.21f
        private const val FRONTAL_LEG_X = 0.114f
        /** How much of the forearm-to-palm roll the forearm takes (the hand takes the rest at the wrist). */
        private const val FOREARM_TWIST_SHARE = 0.65f
        /** A hand lower than this (m) is resting on the floor / mat. */
        private const val FLOOR_HAND_HEIGHT = 0.17f
        private val SPINE = listOf("spine_01", "spine_02", "spine_03")
        private val FINGER = Regex("(index|middle|ring|pinky|thumb)_0(\\d)_[lr]")

        /** Two-bone IK. Returns (mid joint, end point); the mid joint bends toward [pole]. */
        fun twoBoneIk(root: V3, target: V3, l1: Float, l2: Float, pole: V3): Pair<V3, V3> {
            val toT = target - root
            val dist = toT.len()
            val d = min(max(dist, kotlin.math.abs(l1 - l2) + 1e-3f), l1 + l2 - 1e-3f)
            val u = if (dist < 1e-5f) V3.Y else toT * (1f / dist)
            val a = (l1 * l1 - l2 * l2 + d * d) / (2f * d)
            val h = sqrt(max(l1 * l1 - a * a, 0f))
            var perp = (pole - root) - u * (pole - root).dot(u)
            if (perp.len() < 1e-4f) perp = V3.Z - u * V3.Z.dot(u)
            perp = perp.unit(V3.Z)
            return (root + u * a + perp * h) to (root + u * d)
        }
    }
}
