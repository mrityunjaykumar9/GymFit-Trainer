package com.freegymtrainer.app.presentation.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freegymtrainer.app.data.local.entity.isVeg
import com.freegymtrainer.app.presentation.components.FoodIcon
import com.freegymtrainer.app.presentation.components.foodIconFile
import com.freegymtrainer.app.presentation.components.mealIconFile
import com.freegymtrainer.app.presentation.theme.DeepNavy
import com.freegymtrainer.app.presentation.theme.OrangePrimary

val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val DAY_FULL = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** Suggested clock time for each meal - kept in sync with ReminderSettings defaults. */
fun mealTimeHint(mealType: String): String = when {
    mealType.equals("Breakfast", ignoreCase = true) -> "around 8:00 AM"
    mealType.equals("Lunch", ignoreCase = true) -> "around 1:00 PM"
    mealType.equals("Snack", ignoreCase = true) -> "around 5:30 PM"
    mealType.equals("Dinner", ignoreCase = true) -> "around 8:30 PM"
    else -> ""
}

fun mealTypeIcon(mealType: String): ImageVector = when {
    mealType.contains("Breakfast", ignoreCase = true) -> Icons.Filled.FreeBreakfast
    mealType.contains("Morning", ignoreCase = true) -> Icons.Filled.Coffee
    mealType.contains("Lunch", ignoreCase = true) -> Icons.Filled.LunchDining
    mealType.contains("Post Workout", ignoreCase = true) -> Icons.Filled.Bolt
    mealType.contains("Workout", ignoreCase = true) -> Icons.Filled.FitnessCenter
    mealType.contains("Dinner", ignoreCase = true) -> Icons.Filled.DinnerDining
    mealType.contains("Snack", ignoreCase = true) -> Icons.Filled.Coffee
    else -> Icons.Filled.Restaurant
}

/** A scrollable Mon-Sun day picker with the current day marked by a dot. */
@Composable
fun DaySelectorRow(selectedDay: Int, today: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..7).forEach { dow ->
            val isSelected = dow == selectedDay
            val isToday = dow == today
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSelect(dow) }
                    .padding(horizontal = 15.dp, vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    DAY_LABELS[dow - 1],
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (!isToday) Color.Transparent
                            else if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

@Composable
fun MealPlanMacroPill(icon: ImageVector, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** Card comparing a plan day's totals to the user's calorie / protein targets. */
@Composable
fun PlanDayTotalCard(planCalories: Int, planProtein: Int, calorieTarget: Int, proteinTarget: Int) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Full day plan total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$planProtein g protein  •  $planCalories kcal",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (proteinTarget > 0) (planProtein.toFloat() / proteinTarget).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your target: $proteinTarget g protein  •  $calorieTarget kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
