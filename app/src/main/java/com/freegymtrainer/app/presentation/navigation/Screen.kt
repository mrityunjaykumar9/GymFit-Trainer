package com.freegymtrainer.app.presentation.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Exercises : Screen("exercises")
    object ExerciseDetail : Screen("exercise_detail/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "exercise_detail/$exerciseId"
    }
    object Workouts : Screen("workouts")
    object WorkoutDayDetail : Screen("workout_day/{dayId}") {
        fun createRoute(dayId: Long) = "workout_day/$dayId"
    }
    object WorkoutExecution : Screen("workout_execution/{dayId}") {
        fun createRoute(dayId: Long) = "workout_execution/$dayId"
    }
    object Nutrition : Screen("nutrition")
    object Progress : Screen("progress")
    object Settings : Screen("settings")

    companion object {
        val bottomNavItems = listOf(Home, Exercises, Workouts, Nutrition, Progress)
    }
}
