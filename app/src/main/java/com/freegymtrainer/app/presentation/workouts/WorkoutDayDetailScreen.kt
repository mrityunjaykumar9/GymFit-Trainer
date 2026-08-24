package com.freegymtrainer.app.presentation.workouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.LoadingState

@Composable
fun WorkoutDayDetailScreen(container: AppContainer, dayId: Long, onBack: () -> Unit, onStartWorkout: () -> Unit) {
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
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) { Text("START WORKOUT") }
                }
            }
        }
    ) { padding ->
        val d = detail
        if (d == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(d.day.muscleGroupsSummary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            d.exercises.forEach { (templateExercise, exercise) ->
                ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${templateExercise.sets} sets × ${templateExercise.reps} reps  •  ${templateExercise.restSeconds}s rest",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
