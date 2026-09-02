package com.freegymtrainer.app.presentation.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.data.local.entity.BodyMeasurement
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.SimpleLineChart
import com.freegymtrainer.app.presentation.components.StatCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")
private fun formatShortDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(shortDateFormatter)

@Composable
fun ProgressScreen(container: AppContainer) {
    val viewModel: ProgressViewModel = viewModel(factory = GenericViewModelFactory { ProgressViewModel(container) })
    val state by viewModel.uiState.collectAsState()
    var showWeightDialog by remember { mutableStateOf(false) }
    var showMeasurementDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Progress") }) }) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Day Streak", "${state.workoutStreak} days", Icons.Filled.LocalFireDepartment, modifier = Modifier.weight(1f))
                StatCard("This Week", "${state.weeklyConsistency} workouts", Icons.Filled.CalendarMonth, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        CardHeader(Icons.Filled.MonitorWeight, "Weight")
                        TextButton(onClick = { showWeightDialog = true }) { Text("+ Log") }
                    }
                    Spacer(Modifier.height(8.dp))
                    SimpleLineChart(
                        values = state.weightHistory.map { it.weightKg },
                        labels = state.weightHistory.map { formatShortDate(it.dateEpochDay) },
                        unitLabel = "kg"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    CardHeader(Icons.Filled.FitnessCenter, "Weekly Training Volume")
                    Spacer(Modifier.height(4.dp))
                    Text("Sets × Reps × Weight, summed per week", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    SimpleLineChart(
                        values = state.weeklyVolume.map { it.totalVolumeKg.toFloat() },
                        labels = state.weeklyVolume.map { it.weekLabel },
                        unitLabel = "kg"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        CardHeader(Icons.Filled.Straighten, "Body Measurements")
                        TextButton(onClick = { showMeasurementDialog = true }) { Text("+ Log") }
                    }
                    if (state.measurements.isEmpty()) {
                        Text("No measurements logged yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val latest = state.measurements.last()
                        Spacer(Modifier.height(8.dp))
                        listOfNotNull(
                            latest.chestCm?.let { "Chest" to it }, latest.waistCm?.let { "Waist" to it },
                            latest.hipsCm?.let { "Hips" to it }, latest.armsCm?.let { "Arms" to it },
                            latest.thighsCm?.let { "Thighs" to it }, latest.neckCm?.let { "Neck" to it }
                        ).forEach { (label, value) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${value} cm")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    CardHeader(Icons.Filled.EmojiEvents, "Personal Records")
                    Spacer(Modifier.height(8.dp))
                    if (state.personalRecords.isEmpty()) {
                        Text("Complete a workout to start setting records.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.personalRecords.take(10).forEach { pr ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🏆 ${pr.exerciseName}")
                                Text("${pr.weightKg.toInt()} kg × ${pr.reps}")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            AdBanner()
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showWeightDialog) {
        WeightLogDialog(onDismiss = { showWeightDialog = false }, onSave = { viewModel.addWeightEntry(it); showWeightDialog = false })
    }
    if (showMeasurementDialog) {
        MeasurementLogDialog(onDismiss = { showMeasurementDialog = false }, onSave = { viewModel.addMeasurement(it); showMeasurementDialog = false })
    }
}

@Composable
private fun CardHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun WeightLogDialog(onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var weight by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Weight") },
        text = {
            OutlinedTextField(
                value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Weight (kg)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = { TextButton(onClick = { weight.toFloatOrNull()?.let(onSave) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MeasurementLogDialog(onDismiss: () -> Unit, onSave: (BodyMeasurement) -> Unit) {
    var chest by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var hips by remember { mutableStateOf("") }
    var arms by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Measurements") },
        text = {
            Column {
                OutlinedTextField(value = chest, onValueChange = { chest = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Chest (cm)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = waist, onValueChange = { waist = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Waist (cm)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = hips, onValueChange = { hips = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Hips (cm)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = arms, onValueChange = { arms = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Arms (cm)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    BodyMeasurement(
                        dateEpochDay = java.time.LocalDate.now().toEpochDay(),
                        chestCm = chest.toFloatOrNull(), waistCm = waist.toFloatOrNull(),
                        hipsCm = hips.toFloatOrNull(), armsCm = arms.toFloatOrNull()
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
