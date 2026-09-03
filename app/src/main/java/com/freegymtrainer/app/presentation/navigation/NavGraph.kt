package com.freegymtrainer.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.presentation.exercises.ExerciseDetailScreen
import com.freegymtrainer.app.presentation.exercises.ExerciseListScreen
import com.freegymtrainer.app.presentation.home.HomeScreen
import com.freegymtrainer.app.presentation.nutrition.FoodDetailScreen
import com.freegymtrainer.app.presentation.nutrition.FoodListScreen
import com.freegymtrainer.app.presentation.nutrition.NutritionScreen
import com.freegymtrainer.app.presentation.onboarding.OnboardingScreen
import com.freegymtrainer.app.presentation.progress.ProgressScreen
import com.freegymtrainer.app.presentation.settings.SettingsScreen
import com.freegymtrainer.app.presentation.workouts.WorkoutDayDetailScreen
import com.freegymtrainer.app.presentation.workouts.WorkoutExecutionScreen
import com.freegymtrainer.app.presentation.workouts.WorkoutHistoryScreen
import com.freegymtrainer.app.presentation.workouts.WorkoutListScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    container: AppContainer,
    settings: SettingsDataStore,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(container = container, onFinished = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                container = container,
                onStartWorkout = { dayId -> navController.navigate(Screen.WorkoutExecution.createRoute(dayId)) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenExercise = { id -> navController.navigate(Screen.ExerciseDetail.createRoute(id)) },
                onOpenWorkouts = {
                    navController.navigate(Screen.Workouts.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.Exercises.route) {
            ExerciseListScreen(
                container = container,
                onOpenExercise = { id -> navController.navigate(Screen.ExerciseDetail.createRoute(id)) }
            )
        }

        composable(
            Screen.ExerciseDetail.route,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("exerciseId") ?: 0L
            ExerciseDetailScreen(container = container, exerciseId = id, onBack = { navController.popBackStack() })
        }

        composable(Screen.Workouts.route) {
            WorkoutListScreen(
                container = container,
                onOpenDay = { dayId -> navController.navigate(Screen.WorkoutDayDetail.createRoute(dayId)) },
                onOpenHistory = { navController.navigate(Screen.WorkoutHistory.route) }
            )
        }

        composable(Screen.WorkoutHistory.route) {
            WorkoutHistoryScreen(container = container, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.WorkoutDayDetail.route,
            arguments = listOf(navArgument("dayId") { type = NavType.LongType })
        ) { backStackEntry ->
            val dayId = backStackEntry.arguments?.getLong("dayId") ?: 0L
            WorkoutDayDetailScreen(
                container = container, dayId = dayId,
                onBack = { navController.popBackStack() },
                onStartWorkout = { navController.navigate(Screen.WorkoutExecution.createRoute(dayId)) },
                onOpenExercise = { id -> navController.navigate(Screen.ExerciseDetail.createRoute(id)) }
            )
        }

        composable(
            Screen.WorkoutExecution.route,
            arguments = listOf(navArgument("dayId") { type = NavType.LongType })
        ) { backStackEntry ->
            val dayId = backStackEntry.arguments?.getLong("dayId") ?: 0L
            WorkoutExecutionScreen(
                container = container, dayId = dayId,
                onFinished = { navController.popBackStack(Screen.Home.route, inclusive = false) }
            )
        }

        composable(Screen.Nutrition.route) {
            NutritionScreen(
                container = container,
                settings = settings,
                onOpenFoodList = { navController.navigate(Screen.FoodList.route) }
            )
        }

        composable(Screen.FoodList.route) {
            FoodListScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onOpenFood = { id -> navController.navigate(Screen.FoodDetail.createRoute(id)) }
            )
        }

        composable(
            Screen.FoodDetail.route,
            arguments = listOf(navArgument("foodId") { type = NavType.LongType })
        ) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getLong("foodId") ?: 0L
            FoodDetailScreen(container = container, foodId = foodId, onBack = { navController.popBackStack() })
        }

        composable(Screen.Progress.route) {
            ProgressScreen(container = container)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                container = container,
                settings = settings,
                onBack = { navController.popBackStack() },
                onDataCleared = {
                    // Profile and weekly plan are gone after this - send the user back through
                    // onboarding instead of Home, and clear the whole back stack so they can't
                    // navigate "back" into screens that assume a profile exists.
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
