package com.freegymtrainer.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.data.local.entity.UserProfile
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.domain.training.BodyPart
import com.freegymtrainer.app.domain.training.BodyPartPlans
import com.freegymtrainer.app.domain.training.TrainingLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    /** The muscle group suggested for today (a simple weekly split), and how many exercises its routine has. */
    val todayPart: BodyPart? = null,
    val todayExerciseCount: Int = 0,
    val isRestDay: Boolean = false,
    val calorieTarget: Int = 2200,
    val latestWeightKg: Float? = null,
    val weightTrendKg: Float? = null, // negative = down since last month
    /** "Exercise of the day": shown as a live 3D demo. */
    val featured: Exercise? = null
)

/**
 * Home is a launch pad, not a dashboard: suggest what to train today, let the person pick any muscle in one
 * tap, and show one exercise being performed in 3D. Everything here comes from live flows so a weight logged
 * elsewhere shows up immediately.
 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init { observe() }

    private fun observe() {
        viewModelScope.launch {
            val showcase = container.exerciseRepository.getByNames(FEATURED_NAMES)
            combine(
                container.userRepository.observeProfile(),
                container.progressRepository.getWeightHistory()
            ) { profile, weightHistory ->
                val today = LocalDate.now()
                val partId = WEEKLY_SPLIT[today.dayOfWeek.value - 1]
                val part = partId?.let { BodyPartPlans.byId(it) }
                val level = TrainingLevel.fromProfile(profile?.experienceLevel ?: "Beginner")

                val targets = if (profile != null) container.nutritionPlanner.resolve()?.targets else null

                val latest = weightHistory.maxWithOrNull(compareBy({ it.dateEpochDay }, { it.id }))
                val monthAgoEpoch = today.minusDays(30).toEpochDay()
                val monthAgo = weightHistory.filter { it.dateEpochDay <= monthAgoEpoch }.maxWithOrNull(compareBy({ it.dateEpochDay }, { it.id }))

                val featuredName = FEATURED_NAMES[(today.toEpochDay() % FEATURED_NAMES.size).toInt()]
                HomeUiState(
                    loading = false,
                    profile = profile,
                    todayPart = part,
                    todayExerciseCount = part?.plan(level)?.size ?: 0,
                    isRestDay = part == null,
                    calorieTarget = targets?.calorieTarget ?: 2200,
                    latestWeightKg = latest?.weightKg,
                    weightTrendKg = if (latest != null && monthAgo != null) latest.weightKg - monthAgo.weightKg else null,
                    featured = showcase[featuredName]
                )
            }.collect { _uiState.value = it }
        }
    }

    fun addWeightEntry(weightKg: Float) {
        viewModelScope.launch { container.progressRepository.addWeightEntry(weightKg) }
    }

    companion object {
        /** Monday..Sunday. null = rest day. A classic push / pull / legs flavoured week. */
        private val WEEKLY_SPLIT = listOf("chest", "back", "legs", "shoulders", "biceps", "abs", null)

        /** Exercises worth showing off on Home, rotated daily. */
        val FEATURED_NAMES = listOf(
            "Barbell Bench Press", "Barbell Back Squat", "Deadlift", "Pull Ups",
            "Overhead Press", "Barbell Curl", "Lat Pulldown"
        )
    }
}
