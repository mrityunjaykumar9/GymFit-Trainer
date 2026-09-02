package com.freegymtrainer.app.presentation.exercises

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.EmptyState
import com.freegymtrainer.app.presentation.components.ExerciseAutoMedia
import com.freegymtrainer.app.presentation.components.FilterChipItem

@Composable
fun ExerciseListScreen(container: AppContainer, onOpenExercise: (Long) -> Unit) {
    val viewModel: ExerciseListViewModel = viewModel(factory = GenericViewModelFactory { ExerciseListViewModel(container) })
    val exercises by viewModel.exercises.collectAsState()
    val filter by viewModel.filter.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Exercise Library") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = { viewModel.setQuery(it) },
                placeholder = { Text("Search exercises...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            FilterLabel("Muscle Group")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    FilterChipItem(
                        label = "All",
                        selected = filter.muscle == null,
                        onClick = { viewModel.setMuscle(null) }
                    )
                }
                items(viewModel.muscleGroups) { muscle ->
                    FilterChipItem(
                        label = muscle,
                        selected = filter.muscle == muscle,
                        onClick = { viewModel.setMuscle(if (filter.muscle == muscle) null else muscle) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FilterLabel("Equipment")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    FilterChipItem(
                        label = "All",
                        selected = filter.equipment == null,
                        onClick = { viewModel.setEquipment(null) }
                    )
                }
                items(viewModel.equipmentOptions) { equipment ->
                    FilterChipItem(
                        label = equipment,
                        selected = filter.equipment == equipment,
                        onClick = { viewModel.setEquipment(if (filter.equipment == equipment) null else equipment) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FilterLabel("Difficulty")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(viewModel.difficultyOptions) { level ->
                    FilterChipItem(
                        label = level,
                        selected = filter.difficulty == level,
                        onClick = { viewModel.setDifficulty(if (filter.difficulty == level) null else level) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (exercises.isEmpty()) {
                EmptyState("No exercises match your filters.")
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items(exercises, key = { it.id }) { exercise ->
                        ElevatedCard(
                            onClick = { onOpenExercise(exercise.id) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))) {
                                    ExerciseAutoMedia(exercise = exercise, modifier = Modifier.fillMaxSize())
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${exercise.primaryMuscle} • ${exercise.equipment} • ${exercise.difficulty}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    )
}
