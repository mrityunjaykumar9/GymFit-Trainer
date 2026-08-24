package com.freegymtrainer.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.StatCard

@Composable
fun HomeScreen(
    container: AppContainer,
    onStartWorkout: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExercise: (Long) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = GenericViewModelFactory { HomeViewModel(container) })
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Free Gym Trainer") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile & settings")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Good day, ${state.profile?.name?.ifBlank { "Athlete" } ?: "Athlete"} 👋",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(16.dp))

            // Today's workout card
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("TODAY'S WORKOUT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(state.todayDayTitle.uppercase(), style = MaterialTheme.typography.headlineMedium)
                    if (!state.isRestDay) {
                        Text(state.todayMuscleGroups, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("${state.todayExerciseCount} exercises  •  ~${state.todayEstimatedMinutes} minutes", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { state.todayTemplateDayId?.let(onStartWorkout) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("START WORKOUT") }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text("Enjoy your rest day - recovery is part of the plan too.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Today's Progress", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Day Streak", "🔥 ${state.workoutStreak}", "🔥", modifier = Modifier.weight(1f))
                StatCard("Workouts", "${state.workoutsThisWeek} this wk", "🏋", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Water", "${"%.1f".format(state.waterTodayMl / 1000f)} / ${"%.1f".format(state.waterTargetMl / 1000f)} L", "💧", modifier = Modifier.weight(1f))
                StatCard("Protein", "${state.proteinTodayG} / ${state.proteinTargetG} g", "🥩", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Weight", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            state.latestWeightKg?.let { "%.1f kg".format(it) } ?: "Not logged yet",
                            style = MaterialTheme.typography.titleLarge
                        )
                        state.weightTrendKg?.let { trend ->
                            val arrow = if (trend <= 0) "↓" else "↑"
                            Text("$arrow ${"%.1f".format(kotlin.math.abs(trend))} kg this month", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (state.recommendedExercises.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Recommended", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.recommendedExercises) { exercise ->
                        ElevatedCard(
                            onClick = { onOpenExercise(exercise.id) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(160.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                Spacer(Modifier.height(4.dp))
                                Text(exercise.primaryMuscle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Nutrition", style = MaterialTheme.typography.titleMedium)
                    Text("${state.calorieTarget} kcal target", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(20.dp))
            AdBanner()
            Spacer(Modifier.height(8.dp))
        }
    }
}
