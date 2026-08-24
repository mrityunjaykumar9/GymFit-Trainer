package com.freegymtrainer.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** A dashboard stat tile - e.g. "12 Day Streak", "4 Workouts". */
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: String, // emoji glyph keeps this dependency-free
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

/** A single filter choice chip row entry. */
@Composable
fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

/**
 * A bundled Material icon standing in for a photo of someone performing the movement.
 * Avoids shipping real people's likenesses / third-party photos in the APK (Play Store
 * licensing risk) while still being more recognizable than a two-letter initials badge.
 */
fun muscleGroupIcon(primaryMuscle: String): ImageVector = when (primaryMuscle) {
    "Chest" -> Icons.Filled.FitnessCenter
    "Back" -> Icons.Filled.SelfImprovement
    "Shoulders" -> Icons.Filled.AccessibilityNew
    "Biceps" -> Icons.Filled.FitnessCenter
    "Triceps" -> Icons.Filled.FitnessCenter
    "Legs" -> Icons.Filled.DirectionsRun
    "Abs" -> Icons.Filled.Accessibility
    "Glutes" -> Icons.Filled.DirectionsWalk
    "Forearms" -> Icons.Filled.PanTool
    "Traps" -> Icons.Filled.AccessibilityNew
    "Full Body" -> Icons.Filled.SelfImprovement
    "Cardio" -> Icons.Filled.Whatshot
    else -> Icons.Filled.FitnessCenter
}
