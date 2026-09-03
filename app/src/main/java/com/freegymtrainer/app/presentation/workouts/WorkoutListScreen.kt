package com.freegymtrainer.app.presentation.workouts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.EmptyState
import com.freegymtrainer.app.presentation.components.muscleGroupIcon
import com.freegymtrainer.app.presentation.components.rememberAssetImageBitmap
import com.freegymtrainer.app.presentation.components.workoutTemplateIcon
import com.freegymtrainer.app.presentation.theme.orangeSoftContainer

@Composable
fun WorkoutListScreen(container: AppContainer, onOpenDay: (Long) -> Unit, onOpenHistory: () -> Unit, onCreateWorkout: () -> Unit) {
    val viewModel: WorkoutListViewModel = viewModel(factory = GenericViewModelFactory { WorkoutListViewModel(container) })
    val templates by viewModel.templates.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Workouts") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreateWorkout, icon = { Icon(Icons.Filled.Add, contentDescription = null) }, text = { Text("New Workout") })
        }
    ) { padding ->
        if (templates.isEmpty()) {
            EmptyState("Loading workout programs...", modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp), // room for the FAB
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { WorkoutsHero(programCount = templates.size) }

            item { HistoryQuickLink(onClick = onOpenHistory, modifier = Modifier.padding(horizontal = 16.dp)) }

            items(templates) { templateWithDays ->
                TemplateCard(templateWithDays = templateWithDays, onOpenDay = onOpenDay, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun HistoryQuickLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Workout History", style = MaterialTheme.typography.titleMedium)
                Text("See your past completed sessions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WorkoutsHero(programCount: Int) {
    val photo = rememberAssetImageBitmap("gym_pics/gym_pic2.jpg")

    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        if (photo != null) {
            Image(bitmap = photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            // Brand-colored scrim (matches Home's hero) rather than a flat black overlay, so the
            // photo still reads as "this app" rather than a generic dark-gradient stock banner.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.88f)
                        )
                    )
                )
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                )
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text("Workout Programs", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(
                "$programCount programs to choose from - pick one that fits your schedule",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun TemplateCard(templateWithDays: TemplateWithDays, onOpenDay: (Long) -> Unit, modifier: Modifier = Modifier) {
    val template = templateWithDays.template
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        workoutTemplateIcon(template.name),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(template.name, style = MaterialTheme.typography.titleLarge)
                    Text(template.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandChip(template.level, Icons.Filled.FitnessCenter, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                BrandChip(
                    // "My Workouts" isn't a fixed weekly program - daysPerWeek is meaningless
                    // there (always 0), so show how many workouts have actually been built.
                    if (template.isCustom) "${templateWithDays.days.size} workouts" else "${template.daysPerWeek} days/wk",
                    Icons.Filled.CalendarViewWeek,
                    orangeSoftContainer(), MaterialTheme.colorScheme.onPrimaryContainer
                )
                BrandChip(
                    template.goalTag, Icons.Filled.Flag,
                    MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.height(14.dp))
            templateWithDays.days.forEach { dayWithCount ->
                DayRow(dayWithCount = dayWithCount, onClick = { onOpenDay(dayWithCount.day.id) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BrandChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, containerColor: Color, contentColor: Color) {
    AssistChip(
        onClick = {},
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp)) },
        colors = AssistChipDefaults.assistChipColors(containerColor = containerColor, labelColor = contentColor),
        border = null
    )
}

@Composable
private fun DayRow(dayWithCount: DayWithExerciseCount, onClick: () -> Unit) {
    val day = dayWithCount.day
    // A faint solid orange fill (not the neutral surfaceVariant gray, and not a runtime alpha
    // over primaryContainer, which looks different depending on what's layered underneath it)
    // so this row still reads as "part of the same app" - the icon badge flips to white-on-
    // orange to stay legible against it.
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = orangeSoftContainer(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    muscleGroupIcon(day.muscleGroupsSummary.split(",").firstOrNull()?.trim() ?: ""),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(day.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "${day.muscleGroupsSummary}  •  ${dayWithCount.exerciseCount} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
