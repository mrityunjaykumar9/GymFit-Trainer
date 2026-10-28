package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,               // "Push Pull Legs", "Beginner Full Body"...
    val description: String,
    val daysPerWeek: Int,
    val level: String,              // Beginner / Intermediate / Advanced
    val goalTag: String,            // Muscle gain / Fat loss / Strength / General fitness
    // True only for the single auto-created "My Workouts" template that holds every day the
    // user builds themselves via the workout builder - lets the UI/queries distinguish it from
    // the seeded programs without fragile name-matching.
    val isCustom: Boolean = false,
    // True only for the single seeded "Warm-Up & Mobility" program (dynamic warm-up drills plus
    // static stretches - see the dedicated tab). Column name kept as "isYoga" from before that
    // section was reframed from a standalone Yoga tab; renaming the column itself would need a
    // migration for zero user-facing benefit, so only the meaning changed, not the name. Keeps
    // this content out of the Workouts tab and the onboarding default-plan picker, both
    // gym-focused, without fragile name-matching.
    val isYoga: Boolean = false
)

@Entity(tableName = "workout_template_days")
data class WorkoutTemplateDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val orderIndex: Int,            // 1..N within the template's cycle
    val title: String,              // "Push Day", "Leg Day"
    val muscleGroupsSummary: String // "Chest, Shoulders, Triceps"
)

@Entity(tableName = "workout_template_exercises")
data class WorkoutTemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateDayId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val sets: Int,
    val reps: String,               // "8-10" or "12" - kept as string for range support
    val restSeconds: Int
)
