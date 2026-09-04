package com.freegymtrainer.app.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
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
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.data.local.dao.DayOption
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.ExerciseAutoMedia
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.SectionHeader
import com.freegymtrainer.app.presentation.components.StatCard
import com.freegymtrainer.app.presentation.components.rememberAssetImageBitmap

@Composable
fun HomeScreen(
    container: AppContainer,
    onStartWorkout: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onOpenWorkouts: () -> Unit,
    onOpenYoga: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = GenericViewModelFactory { HomeViewModel(container) })
    val state by viewModel.uiState.collectAsState()
    var showWeightDialog by remember { mutableStateOf(false) }

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
        ) {
            val displayName = state.profile?.name?.ifBlank { "Athlete" } ?: "Athlete"
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayName.trim().firstOrNull()?.uppercase() ?: "A",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "${timeBasedGreeting()} 👋",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        displayName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            TrainingModeSelector(onOpenGym = onOpenWorkouts, onOpenYoga = onOpenYoga)

            Spacer(Modifier.height(12.dp))

            TodayWorkoutHero(
                state = state,
                onStartWorkout = onStartWorkout,
                onSkipToday = { viewModel.setSkipToday(true) },
                onUndoSkip = { viewModel.setSkipToday(false) },
                onOpenWorkouts = onOpenWorkouts
            )

            Spacer(Modifier.height(24.dp))
            SectionHeader("Today's Progress")

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Day Streak", "${state.workoutStreak} days", Icons.Filled.LocalFireDepartment, modifier = Modifier.weight(1f))
                StatCard("Workouts", "${state.workoutsThisWeek} this wk", Icons.Filled.FitnessCenter, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    "Water",
                    "${"%.1f".format(state.waterTodayMl / 1000f)} / ${"%.1f".format(state.waterTargetMl / 1000f)} L",
                    Icons.Filled.WaterDrop,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    "Protein",
                    "${state.proteinTodayG} / ${state.proteinTargetG} g",
                    Icons.Filled.DinnerDining,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Weight", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                state.latestWeightKg?.let { "%.1f kg".format(it) } ?: "Not logged yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    if (state.latestWeightKg == null) {
                        TextButton(onClick = { showWeightDialog = true }) { Text("+ Log") }
                    } else {
                        state.weightTrendKg?.let { trend ->
                            val isDown = trend <= 0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isDown) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isDown) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("${"%.1f".format(kotlin.math.abs(trend))} kg this month", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            GymPhotoBanner()

            if (state.recommendedExercises.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader("Recommended")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(state.recommendedExercises) { exercise ->
                        ElevatedCard(
                            onClick = { onOpenExercise(exercise.id) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(160.dp)
                        ) {
                            Column {
                                Box(Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))) {
                                    ExerciseAutoMedia(exercise = exercise, modifier = Modifier.fillMaxSize())
                                }
                                Column(Modifier.padding(12.dp)) {
                                    Text(exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                    Spacer(Modifier.height(4.dp))
                                    Text(exercise.primaryMuscle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Nutrition", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("${state.calorieTarget} kcal target", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(20.dp))
            AdBanner()
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showWeightDialog) {
        QuickWeightLogDialog(
            onDismiss = { showWeightDialog = false },
            onSave = { viewModel.addWeightEntry(it); showWeightDialog = false }
        )
    }
}

private fun timeBasedGreeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

@Composable
private fun QuickWeightLogDialog(onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var weight by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Weight") },
        text = {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Weight (kg)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )
        },
        confirmButton = { TextButton(onClick = { weight.toFloatOrNull()?.let(onSave) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Gym-goers and yoga practitioners are largely different audiences with different intents, so
 * Home leads with this choice up front rather than mixing both into one undifferentiated feed -
 * pick a path here, or use both by returning and tapping the other one another day.
 */
@Composable
private fun TrainingModeSelector(onOpenGym: () -> Unit, onOpenYoga: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeCard(
            icon = Icons.Filled.FitnessCenter,
            label = "Gym",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onOpenGym,
            modifier = Modifier.weight(1f)
        )
        ModeCard(
            icon = Icons.Filled.Spa,
            label = "Yoga",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onOpenYoga,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = contentColor)
        }
    }
}

/**
 * The one "what am I doing right now" section on Home: today's scheduled workout (or rest-day /
 * skip messaging) driven by the weekly plan, plus a row of quick-pick program chips underneath
 * that override it for this visit only, without touching the actual schedule - one representative
 * day per program (Beginner Full Body, Push Pull Legs, ...; custom ones from the workout builder
 * show up the same way) rather than every single day flattened into a long list. Uses the gym
 * photo you supplied (assets/gym_pics/gym_pic1.jpg) as the background with a color scrim over it
 * for text legibility; falls back to a plain gradient if that asset is ever missing.
 */
@Composable
private fun TodayWorkoutHero(
    state: HomeUiState,
    onStartWorkout: (Long) -> Unit,
    onSkipToday: () -> Unit,
    onUndoSkip: () -> Unit,
    onOpenWorkouts: () -> Unit
) {
    val heroPhoto = rememberAssetImageBitmap("gym_pics/gym_pic1.jpg")

    val categoryChips = remember(state.dayOptions) {
        state.dayOptions.groupBy { it.templateName }.values.mapNotNull { it.firstOrNull() }
    }
    var override by remember { mutableStateOf<DayOption?>(null) }
    val picked = override

    // Both scrim stops are kept mid-to-dark so a single light text color stays legible
    // everywhere (a pastel "container" tone at either end would wash the text out).
    val isRestDay = picked == null && state.isRestDay
    val scrimTop = if (isRestDay) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val scrimBottom = MaterialTheme.colorScheme.secondary
    val onGradient = MaterialTheme.colorScheme.onPrimary

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
        Box(Modifier.fillMaxWidth().height(230.dp)) {
            if (heroPhoto != null) {
                Image(bitmap = heroPhoto, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(scrimTop.copy(alpha = 0.45f), scrimBottom.copy(alpha = 0.88f))))
                )
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(scrimTop, scrimBottom))))
                Icon(
                    if (isRestDay) Icons.Filled.SelfImprovement else Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = onGradient.copy(alpha = 0.15f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(130.dp)
                )
            }

            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                if (picked != null) {
                    Text("QUICK START", style = MaterialTheme.typography.labelLarge, color = onGradient.copy(alpha = 0.85f))
                    Spacer(Modifier.height(4.dp))
                    Text(picked.title.uppercase(), style = MaterialTheme.typography.headlineMedium, color = onGradient)
                    Spacer(Modifier.height(4.dp))
                    Text(picked.muscleGroupsSummary, style = MaterialTheme.typography.bodyMedium, color = onGradient.copy(alpha = 0.9f))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${picked.exerciseCount} exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onGradient.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onStartWorkout(picked.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("START WORKOUT") }
                } else {
                    Text("TODAY'S WORKOUT", style = MaterialTheme.typography.labelLarge, color = onGradient.copy(alpha = 0.85f))
                    Spacer(Modifier.height(4.dp))
                    Text(state.todayDayTitle.uppercase(), style = MaterialTheme.typography.headlineMedium, color = onGradient)

                    if (!state.isRestDay) {
                        Spacer(Modifier.height(4.dp))
                        Text(state.todayMuscleGroups, style = MaterialTheme.typography.bodyMedium, color = onGradient.copy(alpha = 0.9f))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${state.todayExerciseCount} exercises  •  ~${state.todayEstimatedMinutes} minutes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onGradient.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { state.todayTemplateDayId?.let(onStartWorkout) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("START WORKOUT") }
                        TextButton(onClick = onSkipToday, colors = ButtonDefaults.textButtonColors(contentColor = onGradient.copy(alpha = 0.85f))) {
                            Text("Not today, skip it")
                        }
                    } else if (state.isManualSkip) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You skipped today's scheduled workout. Change your mind?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onGradient.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onUndoSkip,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("ACTUALLY, LET'S TRAIN") }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Enjoy your rest day - recovery is part of the plan too.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onGradient.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onOpenWorkouts,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = onGradient),
                            border = androidx.compose.foundation.BorderStroke(1.dp, onGradient.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("BROWSE WORKOUTS") }
                    }
                }
            }
        }

        if (categoryChips.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        if (picked == null) "Or start something else" else "Tap again to go back to today's plan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryChips, key = { it.id }) { chip ->
                            FilterChip(
                                selected = picked?.id == chip.id,
                                onClick = { override = if (picked?.id == chip.id) null else chip },
                                label = { Text(chip.templateName.substringBefore(" (")) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small photo banner (assets/gym_pics/gym_pic2.jpg) breaking up the middle of the Home
 * screen. Quietly omits itself if the asset is missing, rather than showing an empty card.
 */
@Composable
private fun GymPhotoBanner() {
    val photo = rememberAssetImageBitmap("gym_pics/gym_pic2.jpg") ?: return

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        Image(bitmap = photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
        )
        Text(
            "Every rep counts. Keep pushing.",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp, end = 70.dp)
        )
    }
}
