package com.freegymtrainer.app.presentation.workouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.EmptyState

@Composable
fun WorkoutListScreen(container: AppContainer, onOpenDay: (Long) -> Unit) {
    val viewModel: WorkoutListViewModel = viewModel(factory = GenericViewModelFactory { WorkoutListViewModel(container) })
    val templates by viewModel.templates.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Workouts") }) }) { padding ->
        if (templates.isEmpty()) {
            EmptyState("Loading workout programs...", modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(templates) { templateWithDays ->
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(templateWithDays.template.name, style = MaterialTheme.typography.titleLarge)
                            AssistChip(onClick = {}, label = { Text(templateWithDays.template.level) })
                        }
                        Text(templateWithDays.template.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${templateWithDays.template.daysPerWeek} days/week  •  ${templateWithDays.template.goalTag}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        templateWithDays.days.forEach { day ->
                            OutlinedButton(
                                onClick = { onOpenDay(day.id) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(day.title)
                                    Text(day.muscleGroupsSummary, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
