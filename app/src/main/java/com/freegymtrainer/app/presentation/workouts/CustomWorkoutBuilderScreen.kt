package com.freegymtrainer.app.presentation.workouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.ExerciseAutoMedia
import com.freegymtrainer.app.presentation.theme.orangeSoftContainer

@Composable
fun CustomWorkoutBuilderScreen(container: AppContainer, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val viewModel: CustomWorkoutBuilderViewModel = viewModel(factory = GenericViewModelFactory { CustomWorkoutBuilderViewModel(container) })
    val state by viewModel.uiState.collectAsState()
    var showDiscardConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedDayId) {
        state.savedDayId?.let(onSaved)
    }

    val canSave = state.title.isNotBlank() && state.selectedEntries.isNotEmpty() && !state.saving
    val requestBack = { if (state.selectedEntries.isNotEmpty() || state.title.isNotBlank()) showDiscardConfirm = true else onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Workout") },
                navigationIcon = { IconButton(onClick = requestBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = viewModel::save,
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (state.saving) "Saving..." else "SAVE WORKOUT", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        label = { Text("Workout name") },
                        placeholder = { Text("e.g. My Push Day") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Text(
                    "Your Exercises (${state.selectedEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.selectedEntries.isEmpty()) {
                item {
                    Text(
                        "No exercises added yet - search below and tap one to add it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(state.selectedEntries, key = { "selected_${it.exercise.id}" }) { entry ->
                    SelectedExerciseCard(
                        entry = entry,
                        onRemove = { viewModel.removeExercise(entry.exercise.id) },
                        onSetsChange = { viewModel.updateSets(entry.exercise.id, it) },
                        onRepsChange = { viewModel.updateReps(entry.exercise.id, it) },
                        onRestChange = { viewModel.updateRestSeconds(entry.exercise.id, it) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                Text("Add Exercises", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search exercises...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            val available = state.availableExercises
            if (available.isEmpty()) {
                item {
                    Text(
                        "No matching exercises.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(available, key = { "picker_${it.id}" }) { exercise ->
                    PickerExerciseRow(
                        exercise = exercise,
                        onAdd = { viewModel.addExercise(exercise) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard this workout?") },
            text = { Text("What you've added so far won't be saved.") },
            confirmButton = { TextButton(onClick = { showDiscardConfirm = false; onBack() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showDiscardConfirm = false }) { Text("Keep Editing") } }
        )
    }
}

@Composable
private fun SelectedExerciseCard(
    entry: BuilderExerciseEntry,
    onRemove: () -> Unit,
    onSetsChange: (Int) -> Unit,
    onRepsChange: (String) -> Unit,
    onRestChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))) {
                    ExerciseAutoMedia(exercise = entry.exercise, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(10.dp))
                Text(entry.exercise.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove ${entry.exercise.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = entry.sets.toString(),
                    onValueChange = { onSetsChange(it.filter { c -> c.isDigit() }.take(2).toIntOrNull() ?: 1) },
                    label = { Text("Sets") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = entry.reps,
                    onValueChange = onRepsChange,
                    label = { Text("Reps") },
                    leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = entry.restSeconds.toString(),
                    onValueChange = { onRestChange(it.filter { c -> c.isDigit() }.take(3).toIntOrNull() ?: 0) },
                    label = { Text("Rest (s)") },
                    leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

@Composable
private fun PickerExerciseRow(exercise: Exercise, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onAdd,
        shape = RoundedCornerShape(12.dp),
        color = orangeSoftContainer(),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))) {
                ExerciseAutoMedia(exercise = exercise, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "${exercise.primaryMuscle} • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Icon(Icons.Filled.Add, contentDescription = "Add ${exercise.name}", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
