package com.freegymtrainer.app.presentation.warmup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.presentation.workouts.DayWithExerciseCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class WarmupUiState(
    val loading: Boolean = true,
    val templateDescription: String = "",
    val flowDays: List<DayWithExerciseCount> = emptyList(),
    val exercises: List<Exercise> = emptyList()
)

/**
 * Backs the dedicated Warm-Up & Mobility tab - a genuinely separate section from
 * Exercises/Workouts (not just a filter within them), pitched at prepping/recovering around a
 * gym session rather than a standalone practice for a different audience. Reuses the same
 * WorkoutDayDetail/ExerciseDetail screens for drilling into a routine or a single exercise -
 * only the top-level browsing surface is dedicated, not the whole feature stack.
 */
class WarmupViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(WarmupUiState())
    val uiState: StateFlow<WarmupUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                container.exerciseRepository.getAllMobility(),
                container.workoutRepository.getMobilityTemplate()
            ) { exercises, template -> exercises to template }
                .collect { (exercises, template) ->
                    val days = template?.let { t ->
                        container.workoutRepository.getTemplateDays(t.id).map { day ->
                            DayWithExerciseCount(day, container.workoutRepository.countExercisesForDay(day.id))
                        }
                    } ?: emptyList()
                    _uiState.value = WarmupUiState(
                        loading = false,
                        templateDescription = template?.description.orEmpty(),
                        flowDays = days,
                        exercises = exercises
                    )
                }
        }
    }
}
