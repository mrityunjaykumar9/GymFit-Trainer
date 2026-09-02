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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
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
