package com.freegymtrainer.app.presentation.workouts

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.ads.InterstitialAdManager
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.ErrorState
import com.freegymtrainer.app.presentation.components.ExerciseAutoMedia
import com.freegymtrainer.app.presentation.components.LoadingState

@Composable
fun WorkoutExecutionScreen(container: AppContainer, dayId: Long, onFinished: () -> Unit) {
    val viewModel: WorkoutExecutionViewModel = viewModel(factory = GenericViewModelFactory { WorkoutExecutionViewModel(container, dayId) })
    val state by viewModel.uiState.collectAsState()
    var showExitConfirm by remember { mutableStateOf(false) }

    if (state.loading) {
        LoadingState()
        return
    }

    if (state.error != null) {
        Scaffold(topBar = { TopAppBar(title = { Text("Workout") }) }) { padding ->
            ErrorState(state.error!!, onRetry = onFinished, retryLabel = "Go Back", modifier = Modifier.padding(padding))
        }
        return
    }

    if (state.isFinished) {
        WorkoutSummaryContent(state, onFinished)
        return
    }

    val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex)
    val currentSet = currentExercise?.sets?.getOrNull(state.currentSetIndex)

    val showActionBar = currentExercise != null && currentSet != null && !state.isResting

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(state.dayTitle) },
                    navigationIcon = {
                        IconButton(onClick = { showExitConfirm = true }) {
                            Icon(Icons.Filled.Close, contentDescription = "Exit workout")
                        }
                    }
                )
                LinearProgressIndicator(
                    progress = { (state.currentExerciseIndex + 1) / state.exercises.size.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        bottomBar = {
            if (showActionBar) {
                Surface(shadowElevation = 8.dp) {
                    Column(Modifier.padding(20.dp)) {
                        Button(
                            onClick = { viewModel.completeCurrentSet() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("COMPLETE SET", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { viewModel.goToPrevious() }, modifier = Modifier.weight(1f)) { Text("Previous") }
                            TextButton(onClick = { viewModel.skipCurrentSet() }, modifier = Modifier.weight(1f)) { Text("Skip Set") }
                        }
                    }
                }
            }
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

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            ) {
                ExerciseAutoMedia(exercise = currentExercise.exercise, modifier = Modifier.fillMaxSize(), showAllSteps = true)
            }
            Spacer(Modifier.height(16.dp))

            Text(
                "EXERCISE ${state.currentExerciseIndex + 1} OF ${state.exercises.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(currentExercise.exercise.name, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Target: ${currentExercise.templateExercise.reps} reps  •  ${currentExercise.templateExercise.sets} sets",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            Text("Sets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                currentExercise.sets.forEachIndexed { index, set ->
                    SetChip(
                        number = index + 1,
                        completed = set.completed,
                        current = index == state.currentSetIndex,
                        onClick = { viewModel.goToSet(index) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Set ${state.currentSetIndex + 1}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = if (currentSet.actualReps == 0) "" else currentSet.actualReps.toString(),
                            onValueChange = { viewModel.updateCurrentSetReps(it.filter { c -> c.isDigit() }.take(3).toIntOrNull() ?: 0) },
                            label = { Text("Reps") },
                            placeholder = { Text(currentExercise.templateExercise.reps) },
                            leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = if (currentSet.weightKg == 0.0) "" else currentSet.weightKg.toString(),
                            onValueChange = { viewModel.updateCurrentSetWeight(it.filter { c -> c.isDigit() || c == '.' }.toDoubleOrNull() ?: 0.0) },
                            label = { Text("Weight (kg)") },
                            leadingIcon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Exit workout?") },
            text = { Text("Sets you haven't completed yet won't be saved. Completed sets are already logged.") },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false; onFinished() }) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Keep Going") }
            }
        )
    }
}

@Composable
private fun SetChip(number: Int, completed: Boolean, current: Boolean, onClick: () -> Unit) {
    // Every state is a tint of the same orange (primary/primaryContainer), never gray - a
    // fainter primaryContainer for "not reached yet" instead of a neutral surfaceVariant keeps
    // the whole set-tracker on-brand rather than mixing in an unrelated gray.
    val containerColor = when {
        completed -> MaterialTheme.colorScheme.primary
        current -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    }
    val contentColor = when {
        completed -> MaterialTheme.colorScheme.onPrimary
        current -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
    }
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(containerColor)
            .then(
                if (current && !completed) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (completed) {
            Icon(Icons.Filled.Check, contentDescription = "Set $number completed", tint = contentColor, modifier = Modifier.size(20.dp))
        } else {
            Text("$number", color = contentColor, style = MaterialTheme.typography.titleMedium)
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
            val activity = LocalContext.current as? Activity
            Button(
                onClick = {
                    // A finished workout is a natural break point - never shown mid-session.
                    // Falls straight through to onFinished if no ad happens to be ready yet.
                    if (activity != null) {
                        InterstitialAdManager.showIfReady(activity, onDismissed = onFinished)
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Done") }
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
