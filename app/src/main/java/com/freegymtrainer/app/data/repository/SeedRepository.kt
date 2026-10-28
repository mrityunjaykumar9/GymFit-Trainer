package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.WorkoutTemplateDay
import com.freegymtrainer.app.data.local.entity.WorkoutTemplateExercise
import com.freegymtrainer.app.data.local.entity.WorkoutTemplate
import com.freegymtrainer.app.data.seed.ExerciseSeedData
import com.freegymtrainer.app.data.seed.FoodSeedData
import com.freegymtrainer.app.data.seed.WorkoutTemplateSeedData

/** Populates the local database with exercises, foods and workout templates; newly shipped foods are added on later launches. */
class SeedRepository(private val db: AppDatabase) {

    suspend fun seedIfNeeded() {
        if (db.exerciseDao().count() == 0) {
            db.exerciseDao().insertAll(ExerciseSeedData.all)
        } else {
            // Existing installs: add any newly shipped exercises (by name) without touching what
            // is there - same mechanism as foods below. This is how the v8->v9 Warm-Up & Mobility
            // rework's brand-new dynamic drills (Arm Circles, Jumping Jacks, ...) reach an
            // already-seeded device; the 12 pre-existing pose renames are handled by
            // MIGRATION_8_9 instead, since those rows already exist and just need updating.
            val have = db.exerciseDao().allNames().toHashSet()
            val missing = ExerciseSeedData.all.filter { it.name !in have }
            if (missing.isNotEmpty()) db.exerciseDao().insertAll(missing)
        }
        if (db.foodDao().count() == 0) {
            db.foodDao().insertAll(FoodSeedData.all)
        } else {
            // Existing installs: add any newly shipped foods (by name) without touching what is there.
            val have = db.foodDao().allNames().toHashSet()
            val missing = FoodSeedData.all.filter { it.name !in have }
            if (missing.isNotEmpty()) db.foodDao().insertAll(missing)
        }
        if (db.workoutTemplateDao().countTemplates() == 0) {
            seedWorkoutTemplates()
        }
    }

    private suspend fun seedWorkoutTemplates() {
        for (seedTemplate in WorkoutTemplateSeedData.all) {
            val templateId = db.workoutTemplateDao().insertTemplate(
                WorkoutTemplate(
                    name = seedTemplate.name,
                    description = seedTemplate.description,
                    daysPerWeek = seedTemplate.daysPerWeek,
                    level = seedTemplate.level,
                    goalTag = seedTemplate.goalTag,
                    isYoga = seedTemplate.isYoga
                )
            )
            seedTemplate.days.forEachIndexed { dayIndex, seedDay ->
                val dayId = db.workoutTemplateDao().insertTemplateDay(
                    WorkoutTemplateDay(
                        templateId = templateId,
                        orderIndex = dayIndex + 1,
                        title = seedDay.title,
                        muscleGroupsSummary = seedDay.muscleGroupsSummary
                    )
                )
                val namesInOrder = seedDay.exercises.map { it.exerciseName }
                val resolved = db.exerciseDao().getByNames(namesInOrder)
                val byName = resolved.associateBy { it.name }
                val templateExercises = seedDay.exercises.mapIndexedNotNull { i, se ->
                    byName[se.exerciseName]?.let { exercise ->
                        WorkoutTemplateExercise(
                            templateDayId = dayId,
                            exerciseId = exercise.id,
                            orderIndex = i + 1,
                            sets = se.sets,
                            reps = se.reps,
                            restSeconds = se.restSeconds
                        )
                    }
                }
                db.workoutTemplateDao().insertTemplateExercises(templateExercises)
            }
        }
    }
}
