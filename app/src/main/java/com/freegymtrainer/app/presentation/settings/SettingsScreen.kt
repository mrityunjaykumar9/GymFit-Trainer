package com.freegymtrainer.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.BuildConfig
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.di.SettingsDataStore

@Composable
fun SettingsScreen(container: AppContainer, settings: SettingsDataStore, onBack: () -> Unit, onDataCleared: () -> Unit) {
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Profile summary
            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val displayName = state.profile?.name?.ifBlank { "Athlete" } ?: "Athlete"
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            displayName.trim().firstOrNull()?.uppercase() ?: "A",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(displayName, style = MaterialTheme.typography.titleLarge)
                        if (!state.profile?.goal.isNullOrBlank()) {
                            Text(
                                state.profile?.goal.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("Appearance", Icons.Filled.Palette)
            SettingsCard {
                Text("Theme", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip("system", "System", Icons.Filled.Brightness6, state.themeMode, viewModel::setThemeMode)
                    ThemeChip("light", "Light", Icons.Filled.LightMode, state.themeMode, viewModel::setThemeMode)
                    ThemeChip("dark", "Dark", Icons.Filled.DarkMode, state.themeMode, viewModel::setThemeMode)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Defaults to Light. Pick Dark to switch, or System to follow your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("Preferences", Icons.Filled.Straighten)
            SettingsCard {
                Text("Units", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("metric" to "kg / cm", "imperial" to "lb / ft").forEach { (value, label) ->
                        FilterChip(selected = state.units == value, onClick = { viewModel.setUnits(value) }, label = { Text(label) })
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("Notifications", Icons.Filled.Notifications)
            SettingsCard {
                val reminders = state.reminderSettings
                SwitchRow(
                    icon = Icons.Filled.FitnessCenter,
                    label = "Workout reminders",
                    checked = reminders.workoutReminderEnabled,
                    onCheckedChange = { viewModel.updateReminderSettings(context, reminders.copy(workoutReminderEnabled = it)) }
                )
                SwitchRow(
                    icon = Icons.Filled.WaterDrop,
                    label = "Water reminders",
                    checked = reminders.waterReminderEnabled,
                    onCheckedChange = { viewModel.updateReminderSettings(context, reminders.copy(waterReminderEnabled = it)) }
                )
                SwitchRow(
                    icon = Icons.Filled.Restaurant,
                    label = "Nutrition reminders",
                    checked = reminders.nutritionReminderEnabled,
                    onCheckedChange = { viewModel.updateReminderSettings(context, reminders.copy(nutritionReminderEnabled = it)) },
                    isLast = true
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("Data", Icons.Filled.DeleteForever)
            ElevatedCard(
                onClick = { showClearDataConfirm = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Clear local data", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            "Deletes all workout history, logs, and your profile from this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsSectionHeader("About", Icons.Filled.Info)
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Free Gym Trainer", style = MaterialTheme.typography.titleMedium)
                        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row {
                    Icon(
                        Icons.Filled.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "This app provides general fitness and nutrition information, not medical advice. " +
                            "Calorie and macro figures are estimates. Consult a professional for medical conditions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                    viewModel.clearAllLocalData(context) { onDataCleared() }
                }) { Text("Delete Everything") }
            },
            dismissButton = { TextButton(onClick = { showClearDataConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ThemeChip(value: String, label: String, icon: ImageVector, current: String, onSelect: (String) -> Unit) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
    )
}

@Composable
private fun SwitchRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, isLast: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    if (!isLast) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
