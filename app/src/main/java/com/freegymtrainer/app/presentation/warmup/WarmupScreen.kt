package com.freegymtrainer.app.presentation.warmup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.AppTopBar
import com.freegymtrainer.app.presentation.components.EmptyState
import com.freegymtrainer.app.presentation.components.ExerciseThumb
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.muscleGroupIcon
import com.freegymtrainer.app.presentation.workouts.DayWithExerciseCount

/**
 * The dedicated Warm-Up & Mobility tab: dynamic pre-lifting drills (Arm Circles, Jumping Jacks,
 * ...) and static stretches (Cat-Cow, Downward-Facing Dog, ...) side by side, pitched at "prep
 * your body to lift" rather than a standalone yoga practice - see [WarmupViewModel] and
 * ExerciseSeedData.kt's "Warm-Up & Mobility" block for the reasoning.
 */
@Composable
fun WarmupScreen(container: AppContainer, onOpenDay: (Long) -> Unit, onOpenExercise: (Long) -> Unit) {
    val viewModel: WarmupViewModel = viewModel(factory = GenericViewModelFactory { WarmupViewModel(container) })
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Warm-Up & Mobility") }) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        if (state.exercises.isEmpty()) {
            EmptyState("Warm-up content is still loading - try again shortly.", modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { WarmupHero(exerciseCount = state.exercises.size, description = state.templateDescription) }

            if (state.flowDays.isNotEmpty()) {
                item {
                    Text(
                        "Routines",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(state.flowDays, key = { "flow_${it.day.id}" }) { day ->
                    RoutineCard(day = day, onClick = { onOpenDay(day.day.id) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            item {
                Text(
                    "All Exercises (${state.exercises.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            items(state.exercises, key = { "mobility_${it.id}" }) { exercise ->
                MobilityExerciseRow(exercise = exercise, onClick = { onOpenExercise(exercise.id) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            }
            item { AdBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
        }
    }
}

/** Calming gradient header with a SelfImprovement watermark. */
@Composable
private fun WarmupHero(exerciseCount: Int, description: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary))
            )
        )
        Icon(
            Icons.Filled.SelfImprovement,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).size(140.dp)
        )

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp)) {
            Text(
                "Warm-Up & Mobility",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                description.ifBlank { "$exerciseCount exercises to prep your body before you lift and help it recover after" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RoutineCard(day: DayWithExerciseCount, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    day.day.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    "${day.day.muscleGroupsSummary}  •  ${day.exerciseCount} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MobilityExerciseRow(exercise: Exercise, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))) {
                ExerciseThumb(exercise = exercise, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        muscleGroupIcon(exercise.primaryMuscle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${exercise.primaryMuscle} • ${if (exercise.isHeldPose) "Hold" else "Reps"} • ${exercise.difficulty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
