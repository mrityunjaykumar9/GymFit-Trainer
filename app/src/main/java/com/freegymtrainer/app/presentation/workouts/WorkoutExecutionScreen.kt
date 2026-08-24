package com.freegymtrainer.app.presentation.workouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.LoadingState

@Composable
fun WorkoutExecutionScreen(container: AppContainer, dayId: Long, onFinished: () -> Unit) {
    val viewModel: WorkoutExecutionViewModel = viewModel(factory = GenericViewModelFactory { WorkoutExecutionViewModel(container, dayId) })
    val state by viewModel.uiState.collectAsState()

    if (state.loading) {
        LoadingState()
        return
    }

    if (state.isFinished) {
        WorkoutSummaryContent(state, onFinished)
        return
    }

    val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex)
    val currentSet = currentExercise?.sets?.getOrNull(state.currentSetIndex)

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("${state.dayTitle}  •  Exercise ${state.currentExerciseIndex + 1}/${state.exercises.size}")
            })
        }
    ) { padding ->
        if (currentExercise == null || currentSet == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        if (state.isResting) {
            RestTimerContent(
                secondsRemaining = state.restSecondsRemaining,
                onSkip = { viewModel.skipRest() },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(currentExercise.exercise.name, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Set ${state.currentSetIndex + 1} of ${currentExercise.sets.size}  •  Target: ${currentExercise.templateExercise.reps} reps",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = if (currentSet.actualReps == 0) "" else currentSet.actualReps.toString(),
                    onValueChange = { viewModel.updateCurrentSetReps(it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0) },
                    label = { Text("Reps") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = if (currentSet.weightKg == 0.0) "" else currentSet.weightKg.toString(),
                    onValueChange = { viewModel.updateCurrentSetWeight(it.toDoubleOrNull() ?: 0.0) },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))
            // Quick set-status row so the user can see progress across the whole exercise.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                currentExercise.sets.forEachIndexed { index, set ->
                    val isCurrent = index == state.currentSetIndex
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            set.completed -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("${index + 1}") }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.goToPrevious() }, modifier = Modifier.weight(1f)) { Text("Previous") }
                OutlinedButton(onClick = { viewModel.skipCurrentSet() }, modifier = Modifier.weight(1f)) { Text("Skip") }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.completeCurrentSet() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("COMPLETE SET") }
        }
    }
}

@Composable
private fun RestTimerContent(secondsRemaining: Int, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("REST", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val minutes = secondsRemaining / 60
            val seconds = secondsRemaining % 60
            Text(
                "%d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                fontSize = 64.sp
            )
            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = onSkip) { Text("Skip Rest") }
        }
    }
}

@Composable
private fun WorkoutSummaryContent(state: WorkoutExecutionUiState, onFinished: () -> Unit) {
    val summary = state.summary
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎉", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Workout Complete!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            if (summary != null) {
                SummaryRow("Duration", "${summary.durationSeconds / 60} min")
                SummaryRow("Sets completed", "${summary.setsCompleted}")
                SummaryRow("Total volume", "${summary.totalVolumeKg.toInt()} kg")
                SummaryRow("Estimated calories", "${summary.estimatedCalories} kcal")

                if (summary.newPersonalRecords.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    summary.newPersonalRecords.forEach { name ->
                        Text("🏆 New Personal Record — $name", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
