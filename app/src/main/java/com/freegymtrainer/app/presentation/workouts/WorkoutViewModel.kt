package com.freegymtrainer.app.presentation.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.*
import com.freegymtrainer.app.data.repository.TemplateDayDetail
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.domain.usecase.CalorieEstimator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ---------------- List screen ----------------

data class DayWithExerciseCount(val day: WorkoutTemplateDay, val exerciseCount: Int)
data class TemplateWithDays(val template: WorkoutTemplate, val days: List<DayWithExerciseCount>)

class WorkoutListViewModel(private val container: AppContainer) : ViewModel() {
    private val _templates = MutableStateFlow<List<TemplateWithDays>>(emptyList())
    val templates: StateFlow<List<TemplateWithDays>> = _templates

    init {
        viewModelScope.launch {
            val all = container.workoutRepository.getAllTemplates().first()
            _templates.value = all.map { template ->
                val days = container.workoutRepository.getTemplateDays(template.id).map { day ->
                    DayWithExerciseCount(day, container.workoutRepository.countExercisesForDay(day.id))
                }
                TemplateWithDays(template, days)
            }
        }
    }
}

// ---------------- Day detail screen ----------------

class WorkoutDayDetailViewModel(private val container: AppContainer, private val dayId: Long) : ViewModel() {
    private val _detail = MutableStateFlow<TemplateDayDetail?>(null)
    val detail: StateFlow<TemplateDayDetail?> = _detail

    init {
        viewModelScope.launch { _detail.value = container.workoutRepository.getTemplateDayDetail(dayId) }
    }
}

// ---------------- Execution screen ----------------

data class SetProgress(val actualReps: Int, val weightKg: Double, val completed: Boolean)

data class ExecutionExerciseState(
    val exercise: Exercise,
    val templateExercise: WorkoutTemplateExercise,
    val sets: List<SetProgress>
)

data class WorkoutExecutionUiState(
    val loading: Boolean = true,
    val dayTitle: String = "",
    val exercises: List<ExecutionExerciseState> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val currentSetIndex: Int = 0,
    val isResting: Boolean = false,
    val restSecondsRemaining: Int = 0,
    val isFinished: Boolean = false,
    val summary: WorkoutSummary? = null,
    val error: String? = null
)

data class WorkoutSummary(
    val durationSeconds: Int,
    val totalVolumeKg: Double,
    val estimatedCalories: Int,
    val setsCompleted: Int,
    val newPersonalRecords: List<String>
)

class WorkoutExecutionViewModel(private val container: AppContainer, private val dayId: Long) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutExecutionUiState())
    val uiState: StateFlow<WorkoutExecutionUiState> = _uiState

    private var sessionId: Long = -1
    private var startTimeMillis: Long = 0
    private var restJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            val detail = container.workoutRepository.getTemplateDayDetail(dayId)
            if (detail == null || detail.exercises.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "This workout couldn't be loaded. Please go back and try again."
                )
                return@launch
            }
            val exerciseStates = detail.exercises.map { (te, ex) ->
                val sets = (1..te.sets).map { SetProgress(actualReps = 0, weightKg = 0.0, completed = false) }
                ExecutionExerciseState(ex, te, sets)
            }
            startTimeMillis = System.currentTimeMillis()
            sessionId = container.workoutRepository.startSession(dayId, detail.day.title)
            _uiState.value = WorkoutExecutionUiState(
                loading = false,
                dayTitle = detail.day.title,
                exercises = exerciseStates
            )
        }
    }

    fun updateCurrentSetReps(reps: Int) = updateCurrentSet { it.copy(actualReps = reps) }
    fun updateCurrentSetWeight(weight: Double) = updateCurrentSet { it.copy(weightKg = weight) }

    /**
     * Rebuilds a fresh set/exercise/exercises chain rather than mutating in place. SetProgress
     * used to have `var` fields mutated directly, but since the outer lists were only shallow-
     * copied, the "old" and "new" WorkoutExecutionUiState ended up structurally equal (they
     * shared the same mutated leaf object) - StateFlow conflates equal values and silently
     * dropped the update, so typed reps/weight never reached the screen.
     */
    private fun updateCurrentSet(mutation: (SetProgress) -> SetProgress) {
        val state = _uiState.value
        val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val set = currentExercise.sets.getOrNull(state.currentSetIndex) ?: return

        val updatedSets = currentExercise.sets.toMutableList().apply { this[state.currentSetIndex] = mutation(set) }
        val updatedExercises = state.exercises.toMutableList().apply {
            this[state.currentExerciseIndex] = currentExercise.copy(sets = updatedSets)
        }
        _uiState.value = state.copy(exercises = updatedExercises)
    }

    fun completeCurrentSet() {
        val state = _uiState.value
        val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val set = currentExercise.sets.getOrNull(state.currentSetIndex) ?: return
        val completedSet = set.copy(completed = true)

        val updatedSets = currentExercise.sets.toMutableList().apply { this[state.currentSetIndex] = completedSet }
        val updatedExercises = state.exercises.toMutableList().apply {
            this[state.currentExerciseIndex] = currentExercise.copy(sets = updatedSets)
        }
        _uiState.value = state.copy(exercises = updatedExercises)

        viewModelScope.launch {
            container.workoutRepository.logSet(
                WorkoutSetLog(
                    sessionId = sessionId,
                    exerciseId = currentExercise.exercise.id,
                    exerciseName = currentExercise.exercise.name,
                    setIndex = state.currentSetIndex + 1,
                    targetReps = currentExercise.templateExercise.reps,
                    actualReps = completedSet.actualReps,
                    weightKg = completedSet.weightKg,
                    completed = true
                )
            )
        }

        advance(currentExercise.templateExercise.restSeconds)
    }

    fun skipCurrentSet() = advance(restSeconds = 0)

    private fun advance(restSeconds: Int) {
        val state = _uiState.value
        val currentExercise = state.exercises[state.currentExerciseIndex]
        val isLastSetOfExercise = state.currentSetIndex >= currentExercise.sets.size - 1
        val isLastExercise = state.currentExerciseIndex >= state.exercises.size - 1

        when {
            !isLastSetOfExercise -> {
                _uiState.value = state.copy(currentSetIndex = state.currentSetIndex + 1)
                if (restSeconds > 0) startRest(restSeconds)
            }
            !isLastExercise -> {
                _uiState.value = state.copy(currentExerciseIndex = state.currentExerciseIndex + 1, currentSetIndex = 0)
                if (restSeconds > 0) startRest(restSeconds)
            }
            else -> finishWorkout()
        }
    }

    /** Jumps directly to a set within the current exercise - used by the tappable set chips. */
    fun goToSet(index: Int) {
        val state = _uiState.value
        val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex) ?: return
        if (index !in currentExercise.sets.indices) return
        restJob?.cancel()
        _uiState.value = state.copy(currentSetIndex = index, isResting = false, restSecondsRemaining = 0)
    }

    fun goToPrevious() {
        val state = _uiState.value
        if (state.currentSetIndex > 0) {
            _uiState.value = state.copy(currentSetIndex = state.currentSetIndex - 1)
        } else if (state.currentExerciseIndex > 0) {
            val previousExercise = state.exercises[state.currentExerciseIndex - 1]
            _uiState.value = state.copy(
                currentExerciseIndex = state.currentExerciseIndex - 1,
                currentSetIndex = previousExercise.sets.size - 1
            )
        }
    }

    private fun startRest(seconds: Int) {
        restJob?.cancel()
        _uiState.value = _uiState.value.copy(isResting = true, restSecondsRemaining = seconds)
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.value = _uiState.value.copy(restSecondsRemaining = remaining)
            }
            _uiState.value = _uiState.value.copy(isResting = false)
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _uiState.value = _uiState.value.copy(isResting = false, restSecondsRemaining = 0)
    }

    private fun finishWorkout() {
        viewModelScope.launch {
            val state = _uiState.value
            val endTime = System.currentTimeMillis()
            val durationSeconds = ((endTime - startTimeMillis) / 1000).toInt()

            var totalVolume = 0.0
            var setsCompleted = 0
            val newRecords = mutableListOf<String>()

            for (exState in state.exercises) {
                for (set in exState.sets) {
                    if (set.completed) {
                        totalVolume += set.weightKg * set.actualReps
                        setsCompleted++
                    }
                }
                val bestSet = exState.sets.filter { it.completed }.maxByOrNull { it.weightKg * (1 + it.actualReps / 30.0) }
                if (bestSet != null) {
                    val isPr = container.workoutRepository.maybeRecordPersonalRecord(
                        exState.exercise.id, exState.exercise.name, bestSet.weightKg, bestSet.actualReps
                    )
                    if (isPr) newRecords.add(exState.exercise.name)
                }
            }

            val profile = container.userRepository.getProfile()
            val estimatedCalories = CalorieEstimator.estimateForSession(
                durationMinutes = durationSeconds / 60, bodyWeightKg = profile?.weightKg ?: 70f
            )

            container.workoutRepository.completeSession(
                WorkoutSession(
                    id = sessionId,
                    dateEpochDay = java.time.LocalDate.now().toEpochDay(),
                    templateDayId = dayId,
                    dayTitle = state.dayTitle,
                    startTimeMillis = startTimeMillis,
                    endTimeMillis = endTime,
                    durationSeconds = durationSeconds,
                    totalVolumeKg = totalVolume,
                    estimatedCalories = estimatedCalories,
                    completed = true
                )
            )

            _uiState.value = state.copy(
                isFinished = true,
                summary = WorkoutSummary(durationSeconds, totalVolume, estimatedCalories, setsCompleted, newRecords)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        restJob?.cancel()
    }
}
