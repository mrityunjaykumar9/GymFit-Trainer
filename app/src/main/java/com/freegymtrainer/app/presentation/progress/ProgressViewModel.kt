package com.freegymtrainer.app.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.BodyMeasurement
import com.freegymtrainer.app.data.local.entity.PersonalRecord
import com.freegymtrainer.app.data.local.entity.WeightEntry
import com.freegymtrainer.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WeeklyVolumePoint(val weekLabel: String, val totalVolumeKg: Double)

data class ProgressUiState(
    val loading: Boolean = true,
    val weightHistory: List<WeightEntry> = emptyList(),
    val measurements: List<BodyMeasurement> = emptyList(),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val weeklyVolume: List<WeeklyVolumePoint> = emptyList(),
    val workoutStreak: Int = 0,
    val weeklyConsistency: Int = 0 // workouts completed in the last 7 days
)

class ProgressViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState

    init {
        viewModelScope.launch {
            container.progressRepository.getWeightHistory().collect { history ->
                _uiState.value = _uiState.value.copy(weightHistory = history, loading = false)
            }
        }
        viewModelScope.launch {
            container.progressRepository.getMeasurements().collect { measurements ->
                _uiState.value = _uiState.value.copy(measurements = measurements)
            }
        }
        viewModelScope.launch {
            container.workoutRepository.getPersonalRecords().collect { records ->
                _uiState.value = _uiState.value.copy(personalRecords = records)
            }
        }
        loadVolumeAndConsistency()
    }

    private fun loadVolumeAndConsistency() {
        viewModelScope.launch {
            val sessions = container.workoutRepository.getAllSessions().first().filter { it.completed }
            val today = LocalDate.now().toEpochDay()

            // Group completed sessions into the last 6 weekly buckets for a simple trend view.
            val weeklyBuckets = (5 downTo 0).map { weeksAgo ->
                val rangeEnd = today - weeksAgo * 7
                val rangeStart = rangeEnd - 6
                val volume = sessions.filter { it.dateEpochDay in rangeStart..rangeEnd }.sumOf { it.totalVolumeKg }
                WeeklyVolumePoint("W${6 - weeksAgo}", volume)
            }

            val weekAgoMillis = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val weeklyConsistency = container.workoutRepository.getCompletedSince(weekAgoMillis).size

            val daySet = sessions.map { it.dateEpochDay }.toSet()
            var cursor = if (daySet.contains(today)) today else today - 1
            var streak = 0
            while (daySet.contains(cursor)) { streak++; cursor-- }

            _uiState.value = _uiState.value.copy(
                weeklyVolume = weeklyBuckets,
                weeklyConsistency = weeklyConsistency,
                workoutStreak = streak
            )
        }
    }

    fun addWeightEntry(weightKg: Float) {
        viewModelScope.launch {
            container.progressRepository.addWeightEntry(weightKg)
        }
    }

    fun addMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            container.progressRepository.addMeasurement(measurement)
        }
    }
}
