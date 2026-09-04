package com.freegymtrainer.app.presentation.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.di.AppContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExerciseFilterState(
    val muscle: String? = null,
    val equipment: String? = null,
    val difficulty: String? = null,
    val query: String = ""
)

class ExerciseListViewModel(private val container: AppContainer) : ViewModel() {

    private val filterState = MutableStateFlow(ExerciseFilterState())
    val filter: StateFlow<ExerciseFilterState> = filterState

    val exercises: StateFlow<List<Exercise>> = filterState
        .flatMapLatest { f ->
            container.exerciseRepository.filter(f.muscle, f.equipment, f.difficulty, f.query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val muscleGroups = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Abs", "Glutes", "Forearms", "Traps", "Full Body", "Cardio", "Flexibility", "Balance")
    val equipmentOptions = listOf("Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Yoga Mat")
    val difficultyOptions = listOf("Beginner", "Intermediate", "Advanced")

    fun setMuscle(muscle: String?) { filterState.value = filterState.value.copy(muscle = muscle) }
    fun setEquipment(equipment: String?) { filterState.value = filterState.value.copy(equipment = equipment) }
    fun setDifficulty(difficulty: String?) { filterState.value = filterState.value.copy(difficulty = difficulty) }
    fun setQuery(query: String) { filterState.value = filterState.value.copy(query = query) }
}

class ExerciseDetailViewModel(private val container: AppContainer, private val exerciseId: Long) : ViewModel() {
    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise

    init {
        viewModelScope.launch {
            _exercise.value = container.exerciseRepository.getById(exerciseId)
        }
    }
}
