package com.freegymtrainer.app.presentation.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.EmptyState
import com.freegymtrainer.app.presentation.components.ExerciseAutoMedia
import com.freegymtrainer.app.presentation.components.FilterChipItem
import kotlin.math.roundToInt

@Composable
fun ExerciseListScreen(container: AppContainer, onOpenExercise: (Long) -> Unit) {
    val viewModel: ExerciseListViewModel = viewModel(factory = GenericViewModelFactory { ExerciseListViewModel(container) })
    val exercises by viewModel.exercises.collectAsState()
    val filter by viewModel.filter.collectAsState()

    val density = LocalDensity.current
    var filterHeightPx by remember { mutableStateOf(0f) }
    var filterOffsetPx by remember { mutableStateOf(0f) }

    // Lets the search + filter block slide up out of the way as the user scrolls through the
    // exercise list - so the list gets the whole screen instead of always sitting below a fixed
    // block of chips - and slide back down as soon as they scroll back toward the top.
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (filterHeightPx <= 0f) return Offset.Zero
                val newOffset = (filterOffsetPx + available.y).coerceIn(-filterHeightPx, 0f)
                val consumed = newOffset - filterOffsetPx
                filterOffsetPx = newOffset
                return Offset(0f, consumed)
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Exercise Library") }) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(nestedScrollConnection)
        ) {
            val listTopPadding = with(density) { (filterHeightPx + filterOffsetPx).coerceAtLeast(0f).toDp() }

            if (exercises.isEmpty()) {
                EmptyState("No exercises match your filters.", modifier = Modifier.padding(top = listTopPadding))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = listTopPadding, start = 16.dp, end = 16.dp, bottom = 4.dp)
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        ElevatedCard(
                            onClick = { onOpenExercise(exercise.id) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
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

            Column(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { filterHeightPx = it.size.height.toFloat() }
                    .offset { IntOffset(x = 0, y = filterOffsetPx.roundToInt()) }
                    .background(MaterialTheme.colorScheme.background)
            ) {
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
