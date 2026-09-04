package com.freegymtrainer.app.presentation.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.data.local.entity.WorkoutSession
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.AppTopBar
import com.freegymtrainer.app.presentation.components.EmptyState
import com.freegymtrainer.app.presentation.components.workoutTemplateIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val historyDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

private fun formatSessionDate(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    return when (epochDay) {
        today.toEpochDay() -> "Today"
        today.minusDays(1).toEpochDay() -> "Yesterday"
        else -> date.format(historyDateFormatter)
    }
}

@Composable
fun WorkoutHistoryScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: WorkoutHistoryViewModel = viewModel(factory = GenericViewModelFactory { WorkoutHistoryViewModel(container) })
    val sessions by viewModel.sessions.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Workout History",
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            EmptyState("No completed workouts yet - finish one and it'll show up here.", modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "${sessions.size} workout${if (sessions.size == 1) "" else "s"} completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
            items(sessions, key = { it.id }) { session ->
                SessionRow(session)
            }
        }
    }
}

@Composable
private fun SessionRow(session: WorkoutSession) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    workoutTemplateIcon(session.dayTitle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(session.dayTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatSessionDate(session.dateEpochDay),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBit(Icons.Filled.Timer, "${session.durationSeconds / 60} min")
                    StatBit(Icons.Filled.FitnessCenter, "${session.totalVolumeKg.toInt()} kg")
                    StatBit(Icons.Filled.Bolt, "${session.estimatedCalories} kcal")
                }
            }
        }
    }
}

@Composable
private fun StatBit(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
