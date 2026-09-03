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
    object WorkoutHistory : Screen("workout_history")
    object Nutrition : Screen("nutrition")
    object FoodList : Screen("food_list")
    object FoodDetail : Screen("food_detail/{foodId}") {
        fun createRoute(foodId: Long) = "food_detail/$foodId"
    }
    object Progress : Screen("progress")
    object Settings : Screen("settings")

    companion object {
        // Lazy avoids a JVM class-init-order NPE (KT-25289): this list references sibling
        // `object` singletons of this sealed class, which can still be null if evaluated
        // eagerly while `Screen` itself is mid-initialization.
        val bottomNavItems by lazy { listOf(Home, Exercises, Workouts, Nutrition, Progress) }
    }
}
