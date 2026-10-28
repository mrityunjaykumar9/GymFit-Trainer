package com.freegymtrainer.app.presentation.exercises

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.data.local.entity.Exercise
import com.freegymtrainer.app.data.repository.CustomExerciseInput
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.domain.training.BodyPart
import com.freegymtrainer.app.domain.training.BodyPartPlans
import com.freegymtrainer.app.domain.training.PlanStep
import com.freegymtrainer.app.domain.training.TrainingLevel
import com.freegymtrainer.app.presentation.components.AppTopBar
import com.freegymtrainer.app.presentation.components.BodyMap
import com.freegymtrainer.app.presentation.components.ExerciseThumb
import com.freegymtrainer.app.presentation.components.LoadingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ------------------------------------------------------------------------------------------------
// Hub: pick what to train today
// ------------------------------------------------------------------------------------------------

/** "What do you want to train today?" - a grid of body parts, each leading to a ready-made routine. */
@Composable
fun BodyPartHub(onOpenBodyPart: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                Text("What do you want to train today?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pick a muscle and get a complete routine - the right exercises, sets, reps and rest - sized to your level.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(BodyPartPlans.all, key = { it.id }) { part -> BodyPartCard(part) { onOpenBodyPart(part.id) } }
        item(span = { GridItemSpan(2) }) {
            Column {
                Spacer(Modifier.height(4.dp))
                AdBanner()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BodyPartCard(part: BodyPart, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MuscleMap(part, Modifier.height(112.dp).fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(part.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                part.subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2, maxLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${part.allExerciseNames.size} exercises", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** The body diagram for a body-part routine: front or back view with the trained muscles lit up. */
@Composable
fun MuscleMap(
    part: BodyPart, modifier: Modifier = Modifier,
    highlight: Color = MaterialTheme.colorScheme.primary, skinColor: Color = MaterialTheme.colorScheme.onSurface
) {
    BodyMap(front = part.front, primary = part.regions, modifier = modifier, highlight = highlight, skinColor = skinColor)
}

// ------------------------------------------------------------------------------------------------
// One body part: how to train it and the routine
// ------------------------------------------------------------------------------------------------

class ResolvedStep(val exercise: Exercise, val step: PlanStep)

data class BodyPartUiState(
    val loading: Boolean = true,
    val part: BodyPart? = null,
    val level: TrainingLevel = TrainingLevel.BEGINNER,
    val steps: List<ResolvedStep> = emptyList()
)

class BodyPartViewModel(private val container: AppContainer, private val partId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(BodyPartUiState())
    val uiState: StateFlow<BodyPartUiState> = _uiState
    private var exercises: Map<String, Exercise> = emptyMap()

    init {
        viewModelScope.launch {
            val part = BodyPartPlans.byId(partId)
            if (part == null) { _uiState.value = BodyPartUiState(loading = false); return@launch }
            exercises = container.exerciseRepository.getByNames(part.allExerciseNames)
            val level = TrainingLevel.fromProfile(container.userRepository.getProfile()?.experienceLevel ?: "Beginner")
            _uiState.value = BodyPartUiState(loading = false, part = part, level = level, steps = resolve(part, level))
        }
    }

    private fun resolve(part: BodyPart, level: TrainingLevel) =
        part.plan(level).mapNotNull { step -> exercises[step.exercise]?.let { ResolvedStep(it, step) } }

    fun setLevel(level: TrainingLevel) {
        val part = _uiState.value.part ?: return
        _uiState.value = _uiState.value.copy(level = level, steps = resolve(part, level))
    }

    /** Saves the routine under "My Workouts" (replacing an older copy) and returns its id to start it. */
    fun start(onReady: (Long) -> Unit) {
        val s = _uiState.value
        val part = s.part ?: return
        viewModelScope.launch {
            val dayId = container.workoutRepository.createOrReplaceCustomDay(
                title = "${part.title} - ${s.level.label}",
                muscleGroupsSummary = part.subtitle,
                exercises = s.steps.map { CustomExerciseInput(it.exercise.id, it.step.sets, it.step.reps, it.step.restSeconds) }
            )
            onReady(dayId)
        }
    }
}

@Composable
fun BodyPartWorkoutScreen(
    container: AppContainer,
    partId: String,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit
) {
    val vm: BodyPartViewModel = viewModel(key = "bodyPart-$partId", factory = GenericViewModelFactory { BodyPartViewModel(container, partId) })
    val state by vm.uiState.collectAsState()
    val part = state.part

    Scaffold(
        topBar = {
            AppTopBar(
                title = part?.title ?: "Training",
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            if (part != null && state.steps.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = { vm.start(onStartWorkout) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start ${part.title.lowercase()} workout")
                    }
                }
            }
        }
    ) { padding ->
        if (state.loading) { LoadingState(Modifier.padding(padding)); return@Scaffold }
        if (part == null) {
            Text("This routine could not be found.", Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val minutes = 5 + state.steps.sumOf { it.step.sets * (45 + it.step.restSeconds) } / 60
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        MuscleMap(part, Modifier.width(84.dp).height(150.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(part.title, style = MaterialTheme.typography.headlineSmall)
                            Text(part.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "${state.steps.size} exercises  ·  about $minutes min",
                                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            item {
                Text("Your level", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrainingLevel.values().forEach { lvl ->
                        FilterChip(selected = state.level == lvl, onClick = { vm.setLevel(lvl) }, label = { Text(lvl.label) })
                    }
                }
                Text(state.level.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { Text("Your routine", style = MaterialTheme.typography.titleLarge) }
            items(state.steps.size) { i ->
                RoutineStepCard(index = i + 1, resolved = state.steps[i], onClick = { onOpenExercise(state.steps[i].exercise.id) })
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("How to train ${part.title.lowercase()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        part.howToTrain.forEach { tip ->
                            Spacer(Modifier.height(8.dp))
                            Text("•  $tip", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item { AdBanner(); Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun RoutineStepCard(index: Int, resolved: ResolvedStep, onClick: () -> Unit) {
    val step = resolved.step
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) { Text("$index", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) {
                ExerciseThumb(exercise = resolved.exercise, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(resolved.exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${step.sets} sets × ${step.reps}  ·  rest ${step.restSeconds} s",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary
                )
                Text(step.tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
