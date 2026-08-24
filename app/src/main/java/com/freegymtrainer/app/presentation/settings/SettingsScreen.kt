package com.freegymtrainer.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.BuildConfig
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.di.SettingsDataStore

@Composable
fun SettingsScreen(container: AppContainer, settings: SettingsDataStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = GenericViewModelFactory { SettingsViewModel(container, settings) })
    val state by viewModel.uiState.collectAsState()
    var showClearDataConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            SettingsSectionHeader("Account")
            ListItem(headlineContent = { Text(state.profile?.name?.ifBlank { "Athlete" } ?: "Athlete") }, supportingContent = { Text(state.profile?.goal ?: "") })

            SettingsSectionHeader("Appearance")
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                        FilterChip(selected = state.themeMode == value, onClick = { viewModel.setThemeMode(value) }, label = { Text(label) })
                    }
                }
            }

            SettingsSectionHeader("Preferences")
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Units", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("metric" to "kg / cm", "imperial" to "lb / ft").forEach { (value, label) ->
                        FilterChip(selected = state.units == value, onClick = { viewModel.setUnits(value) }, label = { Text(label) })
                    }
                }
            }

            SettingsSectionHeader("Notifications")
            val reminders = state.reminderSettings
            SwitchRow(
                label = "Workout reminders",
                checked = reminders.workoutReminderEnabled,
                onCheckedChange = { viewModel.updateReminderSettings(context, reminders.copy(workoutReminderEnabled = it)) }
            )
            SwitchRow(
                label = "Water reminders",
                checked = reminders.waterReminderEnabled,
                onCheckedChange = { viewModel.updateReminderSettings(context, reminders.copy(waterReminderEnabled = it)) }
            )
            SwitchRow(
                label = "Nutrition reminders",
                checked = reminders.nutritionReminderEnabled,
                onCheckedChange = { viewModel.updateReminderSettings(context, reminders.copy(nutritionReminderEnabled = it)) }
            )

            SettingsSectionHeader("Data")
            ListItem(
                headlineContent = { Text("Clear local data") },
                supportingContent = { Text("Deletes all workout history, logs, and your profile from this device") },
                modifier = Modifier.clickable(onClick = { showClearDataConfirm = true })
            )

            SettingsSectionHeader("About")
            ListItem(headlineContent = { Text("Free Gym Trainer") }, supportingContent = { Text("Version ${BuildConfig.VERSION_NAME}") })
            ListItem(
                headlineContent = { Text("Health disclaimer") },
                supportingContent = {
                    Text(
                        "This app provides general fitness and nutrition information, not medical advice. " +
                            "Calorie and macro figures are estimates. Consult a professional for medical conditions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear all local data?") },
            text = { Text("This permanently deletes your profile, workout history, and logs from this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataConfirm = false
                    viewModel.clearAllLocalData(context) { onBack() }
                }) { Text("Delete Everything") }
            },
            dismissButton = { TextButton(onClick = { showClearDataConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}
