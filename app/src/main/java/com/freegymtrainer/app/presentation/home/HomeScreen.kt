package com.freegymtrainer.app.presentation.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.domain.training.BodyPart
import com.freegymtrainer.app.domain.training.BodyPartPlans
import com.freegymtrainer.app.presentation.components.AppTopBar
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.SectionHeader
import com.freegymtrainer.app.presentation.components.body3d.Athlete3DCanvas
import com.freegymtrainer.app.presentation.components.body3d.OrbitState
import com.freegymtrainer.app.presentation.components.motion.ExerciseMotionLibrary
import com.freegymtrainer.app.presentation.exercises.MuscleMap

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenSettings: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onOpenBodyPart: (String) -> Unit,
    onOpenExercises: () -> Unit,
    onOpenWarmup: () -> Unit,
    onOpenNutrition: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = GenericViewModelFactory { HomeViewModel(container) })
    val state by viewModel.uiState.collectAsState()
    var showWeightDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "GymFit Trainer",
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
            Greeting(state.profile?.name?.ifBlank { null } ?: "Athlete")
            Spacer(Modifier.height(12.dp))

            TodayHero(
                part = state.todayPart,
                exerciseCount = state.todayExerciseCount,
                onOpenBodyPart = onOpenBodyPart,
                onOpenWarmup = onOpenWarmup
            )

            Spacer(Modifier.height(22.dp))
            SectionHeader("Train a muscle")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(BodyPartPlans.all, key = { it.id }) { part -> MuscleChip(part) { onOpenBodyPart(part.id) } }
            }

            state.featured?.let { exercise ->
                Spacer(Modifier.height(24.dp))
                SectionHeader("See it done right")
                FeaturedDemo(exercise = exercise, onClick = { onOpenExercise(exercise.id) })
            }

            Spacer(Modifier.height(20.dp))
            AdBanner()

            Spacer(Modifier.height(20.dp))
            SectionHeader("More")
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShortcutCard(
                        "All exercises", "66 with 3D demos", Icons.Filled.FitnessCenter,
                        MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer,
                        onOpenExercises, Modifier.weight(1f)
                    )
                    ShortcutCard(
                        "Warm-Up", "Mobility & stretches", Icons.Filled.SelfImprovement,
                        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer,
                        onOpenWarmup, Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShortcutCard(
                        "Nutrition", "${state.calorieTarget} kcal target", Icons.Filled.Restaurant,
                        MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                        onOpenNutrition, Modifier.weight(1f)
                    )
                    WeightCard(
                        weightKg = state.latestWeightKg, trendKg = state.weightTrendKg,
                        onLog = { showWeightDialog = true }, modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showWeightDialog) {
        QuickWeightLogDialog(
            onDismiss = { showWeightDialog = false },
            onSave = { viewModel.addWeightEntry(it); showWeightDialog = false }
        )
    }
}

@Composable
private fun Greeting(displayName: String) {
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
            Text("${timeBasedGreeting()} 👋", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(displayName, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
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

/** Today's suggestion: a bold card with the glowing body diagram of the muscle to train. Sunday is a rest day. */
@Composable
private fun TodayHero(part: BodyPart?, exerciseCount: Int, onOpenBodyPart: (String) -> Unit, onOpenWarmup: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val base = if (part == null) cs.tertiary else cs.primary
    val brush = Brush.linearGradient(listOf(base, androidx.compose.ui.graphics.lerp(base, Color.Black, 0.62f)))
    val shown = part ?: BodyPartPlans.byId("full")
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(brush)
    ) {
        // soft light behind the figure
        Box(
            Modifier.align(Alignment.CenterEnd).size(230.dp)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)))
        )
        if (shown != null) {
            MuscleMap(
                shown, Modifier.align(Alignment.CenterEnd).padding(end = 10.dp, top = 10.dp, bottom = 10.dp).width(112.dp).height(200.dp),
                highlight = Color.White, skinColor = Color.White
            )
        }
        Column(Modifier.padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 130.dp)) {
            if (part != null) {
                Text("SUGGESTED FOR TODAY", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(2.dp))
                Text("${part.title} day", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("$exerciseCount exercises  ·  ${part.subtitle}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f), maxLines = 2)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onOpenBodyPart(part.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1B1B1F))
                ) {
                    Text("Start", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Text("REST DAY", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(2.dp))
                Text("Recover well", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Muscles grow while you rest.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onOpenBodyPart("full") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1B1B1F))
                    ) { Text("Full body") }
                    OutlinedButton(
                        onClick = onOpenWarmup,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))
                    ) { Text("Mobility") }
                }
            }
        }
    }
}

@Composable
private fun MuscleChip(part: BodyPart, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(18.dp), modifier = Modifier.width(104.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MuscleMap(part, Modifier.height(86.dp).fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(part.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Live 3D demo of the exercise of the day: loops the movement while slowly turning, so the first thing a
 * visitor sees is what the app is for. Tap for the full guide.
 */
@Composable
private fun FeaturedDemo(exercise: Exercise, onClick: () -> Unit) {
    val context = LocalContext.current
    val motion = remember(exercise.name) { ExerciseMotionLibrary.forExerciseName(context, exercise.name) } ?: return
    val transition = rememberInfiniteTransition(label = "featured")
    val progress = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(motion.durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    val orbit = remember { OrbitState(initialYaw = -60f) }
    LaunchedEffect(motion) {
        // a slow, endless sweep between a front and a side view
        while (true) {
            orbit.yaw.animateTo(-15f, tween(7000, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            orbit.yaw.animateTo(-70f, tween(7000, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }
    }

    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            Athlete3DCanvas(motion = motion, progress = { progress.value }, orbit = orbit, exerciseName = exercise.name)
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                    .padding(16.dp)
            ) {
                Column(Modifier.align(Alignment.BottomStart)) {
                    Text("EXERCISE OF THE DAY", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Tap for step-by-step form tips", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                }
                Icon(Icons.Filled.ViewInAr, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    title: String, subtitle: String, icon: ImageVector,
    container: Color, content: Color, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = container), modifier = modifier
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = content)
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = content)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun WeightCard(weightKg: Float?, trendKg: Float?, onLog: () -> Unit, modifier: Modifier = Modifier) {
    val container = MaterialTheme.colorScheme.surfaceVariant
    val content = MaterialTheme.colorScheme.onSurfaceVariant
    ElevatedCard(
        onClick = onLog, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = container), modifier = modifier
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = content)
            Spacer(Modifier.height(10.dp))
            Text(weightKg?.let { "%.1f kg".format(it) } ?: "Log weight", style = MaterialTheme.typography.titleMedium, color = content)
            if (trendKg != null) {
                val down = trendKg <= 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (down) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp, contentDescription = null,
                        tint = if (down) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${"%.1f".format(kotlin.math.abs(trendKg))} kg / month", style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.8f))
                }
            } else {
                Text(if (weightKg == null) "Tap to add today's weight" else "Tap to update", style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.8f))
            }
        }
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
