package com.freegymtrainer.app.presentation.workouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.data.local.entity.WorkoutTemplateExercise
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.ExerciseAutoMedia
import com.freegymtrainer.app.presentation.components.LoadingState

@Composable
fun WorkoutDayDetailScreen(
    container: AppContainer,
    dayId: Long,
    onBack: () -> Unit,
    onStartWorkout: () -> Unit,
    onOpenExercise: (Long) -> Unit
) {
    val viewModel: WorkoutDayDetailViewModel = viewModel(factory = GenericViewModelFactory { WorkoutDayDetailViewModel(container, dayId) })
    val detail by viewModel.detail.collectAsState()
    val swapCandidates by viewModel.swapCandidates.collectAsState()
    var swapTarget by remember { mutableStateOf<Pair<WorkoutTemplateExercise, Exercise>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.day?.title ?: "") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            if (detail != null) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = onStartWorkout,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("START WORKOUT", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { padding ->
        val d = detail
        if (d == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        val estimatedMinutes = (d.exercises.sumOf { it.first.sets } * 3.5).toInt().coerceAtLeast(15)

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(d.day.muscleGroupsSummary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatChip(Icons.Filled.FitnessCenter, "${d.exercises.size} exercises")
                        StatChip(Icons.Filled.Timer, "~$estimatedMinutes min")
                    }
                }
            }

            d.exercises.forEach { (templateExercise, exercise) ->
                ElevatedCard(
                    onClick = { onOpenExercise(exercise.id) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))) {
                            ExerciseAutoMedia(exercise = exercise, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${templateExercise.sets} sets × ${templateExercise.reps} reps  •  ${templateExercise.restSeconds}s rest",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            swapTarget = templateExercise to exercise
                            viewModel.loadSwapCandidates(exercise.id, exercise.primaryMuscle)
                        }) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap exercise", tint = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    val target = swapTarget
    if (target != null) {
        val (templateExercise, currentExercise) = target
        ModalBottomSheet(onDismissRequest = { swapTarget = null; viewModel.clearSwapCandidates() }) {
            SwapExerciseSheet(
                currentExercise = currentExercise,
                candidates = swapCandidates,
                onPick = { replacement ->
                    viewModel.swapExercise(templateExercise.id, replacement.id)
                    swapTarget = null
                }
            )
        }
    }
}

@Composable
private fun SwapExerciseSheet(currentExercise: Exercise, candidates: List<SwapCandidate>, onPick: (Exercise) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Text("Swap \"${currentExercise.name}\"", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Same muscle group, different equipment - pick a replacement for this slot.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        if (candidates.isEmpty()) {
            Text(
                "No alternatives found for this muscle group.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            candidates.forEach { candidate ->
                val exercise = candidate.exercise
                Surface(
                    onClick = { onPick(exercise) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))) {
                            ExerciseAutoMedia(exercise = exercise, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(exercise.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                exercise.equipment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                        if (candidate.isAvailable) {
                            Spacer(Modifier.width(8.dp))
                            AssistChip(
                                onClick = { onPick(exercise) },
                                label = { Text("Available", style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
