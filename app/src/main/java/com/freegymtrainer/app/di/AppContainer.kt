package com.freegymtrainer.app.di

import android.content.Context
import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.repository.*

/**
 * Minimal, hand-rolled dependency container - avoids pulling in Hilt/Koin for a lean MVP.
 * Every repository is created once and reused. Swap for Hilt/Koin later if the app grows.
 */
class AppContainer(context: Context) {
    private val db: AppDatabase = AppDatabase.getInstance(context)
    private val appContext: Context = context.applicationContext

    val seedRepository by lazy { SeedRepository(db) }
    val exerciseRepository by lazy { ExerciseRepository(db) }
    val workoutRepository by lazy { WorkoutRepository(db) }
    val userRepository by lazy { UserRepository(db) }
    val progressRepository by lazy { ProgressRepository(db) }
    val nutritionRepository by lazy { NutritionRepository(db) }
    val nutritionPlanner by lazy { NutritionPlanner(db, SettingsDataStore(appContext)) }
}
