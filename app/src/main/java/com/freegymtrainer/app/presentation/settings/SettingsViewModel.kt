package com.freegymtrainer.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.ReminderSettings
import com.freegymtrainer.app.data.local.entity.UserProfile
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: UserProfile? = null,
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val themeMode: String = "light",
    val units: String = "metric"
)

class SettingsViewModel(
    private val container: AppContainer,
    private val settingsStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            container.userRepository.observeProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(profile = profile)
            }
        }
        viewModelScope.launch {
            container.userRepository.observeReminderSettings().collect { settings ->
                if (settings != null) _uiState.value = _uiState.value.copy(reminderSettings = settings)
            }
        }
        viewModelScope.launch {
            settingsStore.themeMode.collect { mode -> _uiState.value = _uiState.value.copy(themeMode = mode) }
        }
        viewModelScope.launch {
            settingsStore.units.collect { units -> _uiState.value = _uiState.value.copy(units = units) }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setUnits(units: String) {
        viewModelScope.launch { settingsStore.setUnits(units) }
    }

    fun updateReminderSettings(context: Context, settings: ReminderSettings) {
        viewModelScope.launch {
            container.userRepository.saveReminderSettings(settings)
            ReminderScheduler.cancelAll(context)
            if (settings.workoutReminderEnabled) {
                ReminderScheduler.scheduleWorkoutReminder(context, settings.workoutReminderHour, settings.workoutReminderMinute)
            }
            if (settings.waterReminderEnabled) {
                ReminderScheduler.scheduleWaterReminder(context, settings.waterReminderIntervalHours)
            }
            if (settings.nutritionReminderEnabled) {
                ReminderScheduler.scheduleNutritionReminder(context)
            }
        }
    }

    fun updateProfileGoal(goal: String) {
        viewModelScope.launch {
            _uiState.value.profile?.let { container.userRepository.saveProfile(it.copy(goal = goal)) }
        }
    }

    /** Clears all locally stored data (workouts, logs, profile) and reseeds reference data
     * (exercise library, foods, workout templates) so the app is immediately usable again -
     * equivalent to a fresh install minus re-downloading. Irreversible; UI must confirm first. */
    fun clearAllLocalData(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            AppDatabase.getInstance(context).clearAllTables()
            container.seedRepository.seedIfNeeded()
            ReminderScheduler.cancelAll(context)
            onDone()
        }
    }
}
