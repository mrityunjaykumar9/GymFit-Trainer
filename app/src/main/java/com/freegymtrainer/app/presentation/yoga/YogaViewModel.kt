package com.freegymtrainer.app.presentation.yoga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.presentation.workouts.DayWithExerciseCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class YogaUiState(
    val loading: Boolean = true,
    val templateDescription: String = "",
    val flowDays: List<DayWithExerciseCount> = emptyList(),
    val poses: List<Exercise> = emptyList()
)

/**
 * Backs the dedicated Yoga tab - a genuinely separate section from Exercises/Workouts, not
 * just a filter within them, since gym-goers and yoga practitioners are largely different
 * audiences and shouldn't have to wade through each other's content. Reuses the same
 * WorkoutDayDetail/ExerciseDetail screens for drilling into a flow or a pose - only the top-
 * level browsing surface is dedicated, not the whole feature stack.
 */
class YogaViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(YogaUiState())
    val uiState: StateFlow<YogaUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                container.exerciseRepository.getAllYoga(),
                container.workoutRepository.getYogaTemplate()
            ) { poses, template -> poses to template }
                .collect { (poses, template) ->
                    val days = template?.let { t ->
                        container.workoutRepository.getTemplateDays(t.id).map { day ->
                            DayWithExerciseCount(day, container.workoutRepository.countExercisesForDay(day.id))
                        }
                    } ?: emptyList()
                    _uiState.value = YogaUiState(
                        loading = false,
                        templateDescription = template?.description.orEmpty(),
                        flowDays = days,
                        poses = poses
                    )
                }
        }
    }
}
