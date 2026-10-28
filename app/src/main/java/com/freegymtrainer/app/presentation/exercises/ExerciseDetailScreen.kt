package com.freegymtrainer.app.presentation.exercises

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.domain.training.ExerciseCoach
import com.freegymtrainer.app.presentation.components.exerciseSlug
import com.freegymtrainer.app.presentation.components.AppTopBar
import com.freegymtrainer.app.presentation.components.BodyMapPair
import com.freegymtrainer.app.presentation.components.ExerciseDemoCard
import com.freegymtrainer.app.presentation.components.ExercisePicKind
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.MuscleRegions
import com.freegymtrainer.app.presentation.components.motion.ExerciseMotionLibrary
import com.freegymtrainer.app.presentation.components.muscleGroupIcon
import com.freegymtrainer.app.presentation.components.rememberExercisePic

@Composable
fun ExerciseDetailScreen(container: AppContainer, exerciseId: Long, onBack: () -> Unit) {
    val viewModel: ExerciseDetailViewModel = viewModel(
        factory = GenericViewModelFactory { ExerciseDetailViewModel(container, exerciseId) }
    )
    val exercise by viewModel.exercise.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = exercise?.name ?: "",
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        val ex = exercise
        if (ex == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExerciseDemoCard(exercise = ex)
            FactRow(ex)
            CoachCard(ex)
            KeyPositions(ex)
            MusclesWorked(ex)
            AdBanner()
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------- facts

@Composable
private fun FactRow(ex: Exercise) {
    val levelColor = when (ex.difficulty) {
        "Beginner" -> Color(0xFF3DDC84)
        "Intermediate" -> Color(0xFFFFB300)
        else -> Color(0xFFFF5252)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Fact("LEVEL", ex.difficulty, Icons.Filled.Speed, levelColor, Modifier.weight(1f))
        Fact("EQUIPMENT", ex.equipment, Icons.Filled.FitnessCenter, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        Fact("TRAINS", ex.primaryMuscle, muscleGroupIcon(ex.primaryMuscle), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
    }
}

@Composable
private fun Fact(label: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

// ------------------------------------------------------------------------------------------- coach

/** What the exercise builds, and the three things that make the rep right - short on purpose. */
@Composable
private fun CoachCard(ex: Exercise) {
    val coach = remember(ex.name) { ExerciseCoach.of(exerciseSlug(ex.name)) } ?: return
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("WHAT IT BUILDS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(coach.builds, style = MaterialTheme.typography.titleSmall)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("PERFECT FORM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            coach.cues.forEach { cue ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF3DDC84), modifier = Modifier.size(20.dp).padding(top = 1.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(cue, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------- key positions

/** The two positions that matter - where the rep starts and where it peaks - as pictures with one line each. */
@Composable
private fun KeyPositions(ex: Exercise) {
    val context = LocalContext.current
    val motion = remember(ex.name) { ExerciseMotionLibrary.forExerciseName(context, ex.name) }
    val start = rememberExercisePic(ex.name, ExercisePicKind.START, 480)
    val peak = rememberExercisePic(ex.name, ExercisePicKind.PEAK, 480)
    if (start == null || peak == null || motion == null) return
    val last = motion.keyTimes.size - 1

    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("The two key positions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Position(1, start, motion.phaseLabel(0), motion.phaseCue(0), Modifier.weight(1f))
                Box(Modifier.padding(horizontal = 6.dp).padding(top = 60.dp)) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Position(2, peak, motion.phaseLabel(minOf(1, last)), motion.phaseCue(minOf(1, last)), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Position(number: Int, pic: androidx.compose.ui.graphics.ImageBitmap, label: String, cue: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))) {
            Image(bitmap = pic, contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                Modifier.padding(8.dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) { Text("$number", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) }
        }
        Spacer(Modifier.height(6.dp))
        if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2)
        if (cue.isNotBlank()) Text(cue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
    }
}

// ----------------------------------------------------------------------------------------- muscles

@Composable
private fun MusclesWorked(ex: Exercise) {
    val primary = remember(ex.primaryMuscle) { MuscleRegions.of(ex.primaryMuscle) }
    val secondary = remember(ex.secondaryMuscles) { MuscleRegions.of(ex.secondaryMuscles) }
    if (primary.isEmpty() && secondary.isEmpty()) return

    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Muscles worked", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            BodyMapPair(primary, secondary - primary, figureHeight = 200.dp)
            Spacer(Modifier.height(6.dp))
            Legend(MaterialTheme.colorScheme.primary, "Main", ex.primaryMuscle)
            if (ex.secondaryMuscles.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Legend(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), "Also", ex.secondaryMuscles.joinToString("  ·  "))
            }
        }
    }
}

@Composable
private fun Legend(color: Color, title: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
