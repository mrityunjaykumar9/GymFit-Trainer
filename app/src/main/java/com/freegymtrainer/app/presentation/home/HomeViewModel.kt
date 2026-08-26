package com.freegymtrainer.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.data.local.entity.UserProfile
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.domain.usecase.NutritionCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val todayDayTitle: String = "Rest Day",
    val todayMuscleGroups: String = "",
    val todayExerciseCount: Int = 0,
    val todayEstimatedMinutes: Int = 0,
    val todayTemplateDayId: Long? = null,
    val isRestDay: Boolean = true,
    val workoutStreak: Int = 0,
    val workoutsThisWeek: Int = 0,
    val waterTodayMl: Int = 0,
    val waterTargetMl: Int = 3000,
    val proteinTodayG: Int = 0,
    val proteinTargetG: Int = 140,
    val calorieTarget: Int = 2200,
    val latestWeightKg: Float? = null,
    val weightTrendKg: Float? = null, // negative = down since last month
    val recommendedExercises: List<Exercise> = emptyList()
)

/**
 * Every stat here is collected from a live Flow and recombined into a fresh [HomeUiState]
 * whenever any of them change - logging water/food, finishing a workout, or logging weight
 * from any other screen updates Home immediately instead of only on next app launch (this
 * screen's ViewModel survives tab switches, so a one-shot load on init would otherwise go
 * stale the moment you leave and come back).
 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init { observe() }

    private fun observe() {
        val todayDow = LocalDate.now().dayOfWeek.value // 1=Mon..7=Sun

        val planAndSessions = combine(
            container.userRepository.observeProfile(),
            container.workoutRepository.getWeeklyPlan(),
            container.workoutRepository.getAllSessions()
        ) { profile, weeklyPlan, sessions -> Triple(profile, weeklyPlan, sessions) }

        val nutritionAndWeight = combine(
            container.progressRepository.getWaterForToday(),
            container.nutritionRepository.getMealsForToday(),
            container.progressRepository.getWeightHistory()
        ) { water, meals, weightHistory -> Triple(water, meals, weightHistory) }

        viewModelScope.launch {
            combine(planAndSessions, nutritionAndWeight) { (profile, weeklyPlan, sessions), (waterLogs, meals, weightHistory) ->
                val planEntry = weeklyPlan.firstOrNull { it.dayOfWeek == todayDow }

                var dayTitle = "Rest Day"
                var muscleGroups = ""
                var exerciseCount = 0
                var estimatedMinutes = 0
                var templateDayId: Long? = null
                var isRest = true

                if (planEntry != null && !planEntry.isRestDay && planEntry.templateDayId != null) {
                    val detail = container.workoutRepository.getTemplateDayDetail(planEntry.templateDayId)
                    if (detail != null) {
                        dayTitle = detail.day.title
                        muscleGroups = detail.day.muscleGroupsSummary
                        exerciseCount = detail.exercises.size
                        estimatedMinutes = estimateMinutes(detail.exercises.sumOf { it.first.sets })
                        templateDayId = detail.day.id
                        isRest = false
                    }
                }

                val completedSessions = sessions.filter { it.completed }
                val streak = calculateStreak(completedSessions.map { it.dateEpochDay })
                val weekAgoMillis = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                val workoutsThisWeek = completedSessions.count { it.startTimeMillis >= weekAgoMillis }

                val waterToday = waterLogs.sumOf { it.amountMl }
                val proteinToday = meals.sumOf { it.protein.toDouble() }.toInt()

                var nutritionTargets: NutritionTargetsSnapshot? = null
                if (profile != null) {
                    val targets = NutritionCalculator.calculateTargets(
                        weightKg = profile.weightKg, heightCm = profile.heightCm,
                        ageYears = estimateAge(profile), gender = profile.gender,
                        activityLevel = profile.activityLevel, goal = profile.goal
                    )
                    nutritionTargets = NutritionTargetsSnapshot(targets.calorieTarget, targets.proteinGrams, targets.waterMl)
                }

                val latestWeight = weightHistory.maxByOrNull { it.dateEpochDay }
                val monthAgoEpoch = LocalDate.now().minusDays(30).toEpochDay()
                val monthAgoEntry = weightHistory.filter { it.dateEpochDay <= monthAgoEpoch }.maxByOrNull { it.dateEpochDay }
                val trend = if (latestWeight != null && monthAgoEntry != null) latestWeight.weightKg - monthAgoEntry.weightKg else null

                val allExercises = container.exerciseRepository.getAll().first()
                val recommended = allExercises
                    .filter { it.primaryMuscle == (muscleGroups.split(",").firstOrNull()?.trim() ?: "") }
                    .take(3)
                    .ifEmpty { allExercises.take(3) }

                HomeUiState(
                    loading = false,
                    profile = profile,
                    todayDayTitle = dayTitle,
                    todayMuscleGroups = muscleGroups,
                    todayExerciseCount = exerciseCount,
                    todayEstimatedMinutes = estimatedMinutes,
                    todayTemplateDayId = templateDayId,
                    isRestDay = isRest,
                    workoutStreak = streak,
                    workoutsThisWeek = workoutsThisWeek,
                    waterTodayMl = waterToday,
                    waterTargetMl = nutritionTargets?.waterMl ?: 3000,
                    proteinTodayG = proteinToday,
                    proteinTargetG = nutritionTargets?.proteinGrams ?: 140,
                    calorieTarget = nutritionTargets?.calorieTarget ?: 2200,
                    latestWeightKg = latestWeight?.weightKg,
                    weightTrendKg = trend,
                    recommendedExercises = recommended
                )
            }.collect { newState -> _uiState.value = newState }
        }
    }

    fun addWeightEntry(weightKg: Float) {
        viewModelScope.launch { container.progressRepository.addWeightEntry(weightKg) }
    }

    private fun estimateMinutes(totalSets: Int): Int = (totalSets * 3.5).toInt().coerceAtLeast(15)

    private fun estimateAge(profile: UserProfile): Int {
        // MVP simplification: date of birth collection was optional during onboarding, so we
        // fall back to a reasonable adult default when it wasn't provided.
        return 28
    }

    private fun calculateStreak(completedEpochDays: List<Long>): Int {
        if (completedEpochDays.isEmpty()) return 0
        val daySet = completedEpochDays.toSet()
        val today = LocalDate.now().toEpochDay()
        // If today has no session yet, start counting from yesterday so the streak doesn't
        // reset to zero simply because the user hasn't trained *yet today*.
        var cursor = if (daySet.contains(today)) today else today - 1
        var streak = 0
        while (daySet.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }

    private data class NutritionTargetsSnapshot(val calorieTarget: Int, val proteinGrams: Int, val waterMl: Int)
}
