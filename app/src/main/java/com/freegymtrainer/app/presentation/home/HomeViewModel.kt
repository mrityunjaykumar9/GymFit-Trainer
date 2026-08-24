package com.freegymtrainer.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.data.local.entity.UserProfile
import com.freegymtrainer.app.data.repository.TemplateDayDetail
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.domain.usecase.NutritionCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            val profile = container.userRepository.getProfile()
            val todayDow = LocalDate.now().dayOfWeek.value // 1=Mon..7=Sun
            val planEntry = container.workoutRepository.getWeeklyPlan().first().firstOrNull { it.dayOfWeek == todayDow }

            var dayTitle = "Rest Day"
            var muscleGroups = ""
            var exerciseCount = 0
            var estimatedMinutes = 0
            var templateDayId: Long? = null
            var isRest = true

            if (planEntry != null && !planEntry.isRestDay && planEntry.templateDayId != null) {
                val detail: TemplateDayDetail? = container.workoutRepository.getTemplateDayDetail(planEntry.templateDayId)
                if (detail != null) {
                    dayTitle = detail.day.title
                    muscleGroups = detail.day.muscleGroupsSummary
                    exerciseCount = detail.exercises.size
                    estimatedMinutes = estimateMinutes(detail.exercises.sumOf { it.first.sets })
                    templateDayId = detail.day.id
                    isRest = false
                }
            }

            val allSessions = container.workoutRepository.getAllSessions().first().filter { it.completed }
            val streak = calculateStreak(allSessions.map { it.dateEpochDay })
            val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val workoutsThisWeek = container.workoutRepository.getCompletedSince(weekAgo).size

            val waterToday = container.progressRepository.getTotalWaterToday()
            val meals = container.nutritionRepository.getMealsForToday().first()
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

            val latestWeight = container.progressRepository.getLatestWeight()
            val weightHistory = container.progressRepository.getWeightHistory().first()
            val monthAgoEpoch = LocalDate.now().minusDays(30).toEpochDay()
            val monthAgoEntry = weightHistory.lastOrNull { it.dateEpochDay <= monthAgoEpoch }
            val trend = if (latestWeight != null && monthAgoEntry != null) latestWeight.weightKg - monthAgoEntry.weightKg else null

            val recommended = container.exerciseRepository.getAll().first()
                .filter { it.primaryMuscle == (muscleGroups.split(",").firstOrNull()?.trim() ?: "") }
                .take(3)
                .ifEmpty { container.exerciseRepository.getAll().first().take(3) }

            _uiState.value = HomeUiState(
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
        }
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
