package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.WorkoutTemplateDay
import com.freegymtrainer.app.data.local.entity.WorkoutTemplateExercise
import com.freegymtrainer.app.data.local.entity.WorkoutTemplate
import com.freegymtrainer.app.data.seed.ExerciseSeedData
import com.freegymtrainer.app.data.seed.FoodSeedData
import com.freegymtrainer.app.data.seed.WorkoutTemplateSeedData

/** Populates the local database with exercises, foods, and workout templates on first launch only. */
class SeedRepository(private val db: AppDatabase) {

    suspend fun seedIfNeeded() {
        if (db.exerciseDao().count() == 0) {
            db.exerciseDao().insertAll(ExerciseSeedData.all)
        }
        if (db.foodDao().count() == 0) {
            db.foodDao().insertAll(FoodSeedData.all)
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
                    goalTag = seedTemplate.goalTag
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
