package com.freegymtrainer.app.presentation.components.body3d

import com.freegymtrainer.app.presentation.components.exerciseSlug
import com.freegymtrainer.app.presentation.components.motion.MotionDef
import com.freegymtrainer.app.presentation.components.motion.Prop

/*
 * How the athlete holds things in each exercise, so the demo teaches the right grip: overhand on a bench press,
 * underhand on a curl, palms-in on a hammer curl or a rope, flat hands on the mat for a push-up.
 */
internal object HandModes {
    private val overhand = setOf(
        "barbell_bench_press", "incline_barbell_bench_press", "close_grip_bench_press", "dumbbell_bench_press",
        "incline_dumbbell_press", "dumbbell_shoulder_press", "overhead_press", "pull_ups", "lat_pulldown",
        "barbell_row", "deadlift", "romanian_deadlift", "barbell_shrug", "skull_crushers", "barbell_back_squat",
        "hanging_leg_raise", "hip_thrust", "rowing_machine", "kettlebell_barbell_clean_and_press"
    )
    private val underhand = setOf("barbell_curl", "dumbbell_curl", "cable_curl", "preacher_curl", "wrist_curl", "front_squat")
    private val neutral = setOf(
        "cable_fly", "pec_deck", "t_bar_row", "seated_cable_row", "rear_delt_fly", "face_pull", "hammer_curl",
        "tricep_pushdown", "overhead_tricep_extension", "walking_lunges", "bulgarian_split_squat", "calf_raise",
        "farmers_carry", "cable_crunch", "crunch", "running", "leg_press", "leg_extension",
        // PALM_DOWN keeps the palm aimed at a near-fixed WORLD direction. For a frontal exercise
        // (toModel maps pose.z straight to model depth, no axis swap) that fixed direction's small
        // z-tilt sits right on the camera's depth axis, so as the arm swept this exercise's ~100deg
        // range the hand visibly rocked forward and back through the rep instead of hanging straight.
        // NEUTRAL's inward target has no z-component at all, so it can't introduce that wobble.
        "lateral_raise"
    )
    private val palmsDown = setOf("front_raise", "stationary_cycling", "stair_climbing")

    fun modeOf(exerciseName: String?): HandMode {
        val slug = exerciseName?.let { exerciseSlug(it) } ?: return HandMode.AUTO
        return when (slug) {
            in overhand -> HandMode.BAR_OVER
            in underhand -> HandMode.BAR_UNDER
            in neutral -> HandMode.NEUTRAL
            in palmsDown -> HandMode.PALM_DOWN
            else -> HandMode.AUTO
        }
    }

    /** Hands for [exerciseName]: closed around the equipment when there is any, loosely curled otherwise. */
    fun handsFor(motion: MotionDef, exerciseName: String?): Hands {
        val holds = motion.props.any { p ->
            fun pinned(j: String?) = j?.startsWith("wrist") == true
            when (p) {
                is Prop.Plate -> true
                is Prop.Dumbbell, is Prop.Cable, is Prop.RopeLoop -> true
                is Prop.Line -> pinned(p.a.joint) || pinned(p.b.joint)
                else -> false
            }
        } || (exerciseName?.let { it.contains("Pull Up", true) || it.contains("Hanging", true) } == true)
        val mode = modeOf(exerciseName)
        val fist = when {
            holds -> 1f
            exerciseSlug(exerciseName ?: "") == "running" -> 0.6f
            else -> 0.2f
        }
        return Hands(mode, mode, fist)
    }
}
