package com.freegymtrainer.app.presentation.exercises

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.muscleGroupIcon

@Composable
fun ExerciseDetailScreen(container: AppContainer, exerciseId: Long, onBack: () -> Unit) {
    val viewModel: ExerciseDetailViewModel = viewModel(
        factory = GenericViewModelFactory { ExerciseDetailViewModel(container, exerciseId) }
    )
    val exercise by viewModel.exercise.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "") },
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
                .padding(16.dp)
        ) {
            // Placeholder for the animation/video asset called for in the full spec - the MVP
            // ships without bundled video to keep the APK small; wire a WebView/ExoPlayer here
            // pointed at a hosted clip later (see README -> "Adding exercise animations").
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Icon(
                            muscleGroupIcon(ex.primaryMuscle),
                            contentDescription = ex.primaryMuscle,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${ex.primaryMuscle} demonstration", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(ex.difficulty) })
                AssistChip(onClick = {}, label = { Text(ex.equipment) })
                AssistChip(onClick = {}, label = { Text(ex.primaryMuscle) })
            }

            Spacer(Modifier.height(20.dp))
            Text("Muscles worked", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Primary: ${ex.primaryMuscle}", style = MaterialTheme.typography.bodyLarge)
            if (ex.secondaryMuscles.isNotEmpty()) {
                Text("Secondary: ${ex.secondaryMuscles.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(20.dp))
            Text("How to perform", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ex.instructions.forEachIndexed { index, step ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("${index + 1}.  ", style = MaterialTheme.typography.bodyLarge)
                    Text(step, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Common mistakes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ex.commonMistakes.forEach { mistake ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge)
                    Text(mistake, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(20.dp))
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp)) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        ex.safetyNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
