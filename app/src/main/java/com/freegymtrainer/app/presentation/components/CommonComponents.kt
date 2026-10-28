package com.freegymtrainer.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A dashboard stat tile - e.g. "12 Day Streak", "4 Workouts". */
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
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
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, retryLabel: String = "Retry", modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRetry) { Text(retryLabel) }
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
    "Flexibility" -> Icons.Filled.Spa
    "Balance" -> Icons.Filled.SelfImprovement
    else -> Icons.Filled.FitnessCenter
}

/** Icon standing in for a workout program's cover image, keyed off its name. */
fun workoutTemplateIcon(templateName: String): ImageVector = when {
    templateName.contains("Warm-Up", ignoreCase = true) || templateName.contains("Mobility", ignoreCase = true) -> Icons.Filled.SelfImprovement
    templateName.contains("Home", ignoreCase = true) -> Icons.Filled.Home
    templateName.contains("Upper Lower", ignoreCase = true) -> Icons.Filled.SwapVert
    templateName.contains("Fat Loss", ignoreCase = true) -> Icons.Filled.LocalFireDepartment
    templateName.contains("Full Body", ignoreCase = true) -> Icons.Filled.SelfImprovement
    else -> Icons.Filled.FitnessCenter
}

/**
 * Shared top bar used on every screen so header height/spacing/title style is consistent
 * app-wide instead of each screen tuning Material3's TopAppBar independently. Deliberately
 * a plain Surface + Row (not Material3's TopAppBar) so the content height is fixed at 56.dp -
 * more compact than Material3's default 64.dp small app bar - while still reserving exactly
 * as much space as the status bar actually needs via windowInsetsTopHeight.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(start = if (navigationIcon != null) 4.dp else 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navigationIcon?.invoke()
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = if (navigationIcon != null) 4.dp else 0.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
    }
}
