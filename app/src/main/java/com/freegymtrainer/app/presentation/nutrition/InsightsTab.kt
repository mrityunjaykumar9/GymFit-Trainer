package com.freegymtrainer.app.presentation.nutrition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.data.local.entity.WeightEntry
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.domain.nutrition.DayTotals
import com.freegymtrainer.app.domain.nutrition.WeekSummary
import com.freegymtrainer.app.presentation.components.LoadingState
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val OnTargetGreen = Color(0xFF4CAF50)
private val UnderAmber = Color(0xFFFFB74D)
private val OverRed = Color(0xFFE57373)

/** The Insights tab: how the last seven days went, and where the weight trend is heading. */
@Composable
fun InsightsTab(container: AppContainer, settings: SettingsDataStore) {
    val vm: InsightsViewModel = viewModel(key = "insights", factory = GenericViewModelFactory { InsightsViewModel(container, settings) })
    val state by vm.uiState.collectAsState()
    if (state.loading) { LoadingState(); return }
    val resolved = state.resolved
    val week = state.week
    if (resolved == null || week == null) {
        Text("Complete onboarding to see your nutrition insights.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
        return
    }
    val target = resolved.targets.calorieTarget

    Column(Modifier.padding(16.dp)) {
        Text("Last 7 days", style = MaterialTheme.typography.titleLarge)
        Text(
            if (week.loggedDays == 0) "Log your meals to see your week here."
            else "You logged food on ${week.loggedDays} of 7 days.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                WeekChart(week.days, target)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Legend(OnTargetGreen, "On target")
                    Legend(UnderAmber, "Under")
                    Legend(OverRed, "Over")
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Avg calories", if (week.loggedDays == 0) "-" else "%,d".format(week.avgCalories), "of %,d target".format(target), Modifier.weight(1f))
            StatTile("Avg protein", if (week.loggedDays == 0) "-" else "${week.avgProtein} g", "of ${resolved.targets.proteinGrams} g", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("On target", "${week.daysOnTarget} / ${max(week.loggedDays, 1)}", "days within 10%", Modifier.weight(1f))
            StatTile("Logging streak", "${week.streak}", if (week.streak == 1) "day" else "days", Modifier.weight(1f))
        }

        if (week.loggedDays > 0) {
            Spacer(Modifier.height(16.dp))
            MacroSplitCard(week)
        }

        Spacer(Modifier.height(16.dp))
        WeightCard(
            onLogWeight = { vm.logWeight(it) },
            weights = state.weights,
            fallbackKg = resolved.profile.weightKg,
            targetKg = resolved.prefs.targetWeightKg,
            weeklyChangeKg = resolved.targets.weeklyChangeKg,
            weeksToGoal = state.weeksToGoal
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Weight moves slowly and fluctuates day to day with water and salt. Judge your plan by the trend over 2-3 weeks, not a single weigh-in.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatTile(title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Seven bars (one per day) with a dashed line at the calorie target. */
@Composable
private fun WeekChart(days: List<DayTotals>, target: Int) {
    val labels = days.map { java.time.LocalDate.ofEpochDay(it.epochDay).dayOfWeek.name.take(1) }
    val track = MaterialTheme.colorScheme.surfaceVariant
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val top = max(days.maxOf { it.calories }.toFloat(), target * 1.3f)
    Column {
        Box(Modifier.fillMaxWidth().height(170.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val n = days.size
                val slot = size.width / n
                val barW = slot * 0.56f
                val h = size.height
                days.forEachIndexed { i, d ->
                    val x = i * slot + (slot - barW) / 2f
                    // faint full-height track so empty days still read as days
                    drawRoundRect(track.copy(alpha = 0.45f), Offset(x, 0f), Size(barW, h), CornerRadius(8.dp.toPx()))
                    if (d.logged) {
                        val color = when {
                            abs(d.calories - target) <= target * 0.10 -> OnTargetGreen
                            d.calories < target -> UnderAmber
                            else -> OverRed
                        }
                        val bh = (d.calories / top * h).coerceAtLeast(6.dp.toPx())
                        drawRoundRect(color, Offset(x, h - bh), Size(barW, bh), CornerRadius(8.dp.toPx()))
                    }
                }
                val y = h - target / top * h
                drawLine(
                    lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
            Text(
                "target ${"%,d".format(target)}", style = MaterialTheme.typography.labelSmall, color = lineColor,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 2.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEachIndexed { i, d ->
                val isToday = d.epochDay == LocalDate.now().toEpochDay()
                Text(
                    labels[i], style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Where the calories came from this week: protein / carbs / fat as a share of energy. */
@Composable
private fun MacroSplitCard(week: WeekSummary) {
    val logged = week.days.filter { it.logged }
    val p = logged.sumOf { it.protein } * 4f
    val c = logged.sumOf { it.carbs } * 4f
    val f = logged.sumOf { it.fat } * 9f
    val total = (p + c + f).coerceAtLeast(1f)
    val pp = (p / total * 100).roundToInt()
    val cp = (c / total * 100).roundToInt()
    val fp = (100 - pp - cp).coerceAtLeast(0)
    val proteinColor = MaterialTheme.colorScheme.primary
    val carbColor = MaterialTheme.colorScheme.tertiary
    val fatColor = Color(0xFF42A5F5)
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Where your calories come from", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp))) {
                if (pp > 0) Box(Modifier.weight(pp.toFloat()).fillMaxHeight().background(proteinColor))
                if (cp > 0) Box(Modifier.weight(cp.toFloat()).fillMaxHeight().background(carbColor))
                if (fp > 0) Box(Modifier.weight(fp.toFloat()).fillMaxHeight().background(fatColor))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(proteinColor, "Protein $pp%")
                Legend(carbColor, "Carbs $cp%")
                Legend(fatColor, "Fat $fp%")
            }
        }
    }
}

@Composable
private fun WeightCard(onLogWeight: (Float) -> Unit, weights: List<WeightEntry>, fallbackKg: Float, targetKg: Float?, weeklyChangeKg: Float, weeksToGoal: Int?) {
    val current = weights.lastOrNull()?.weightKg ?: fallbackKg
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            var showDialog by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Weight trend", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { showDialog = true }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    Text("+ Log weight")
                }
            }
            if (showDialog) LogWeightDialog(currentKg = current, onSave = { onLogWeight(it); showDialog = false }, onDismiss = { showDialog = false })
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.1f".format(current), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(" kg", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 3.dp))
                if (weights.size >= 2) {
                    val delta = weights.last().weightKg - weights.first().weightKg
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "${if (delta > 0) "+" else ""}${"%.1f".format(delta)} kg over ${weights.size} entries",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            if (weights.size >= 2) {
                Spacer(Modifier.height(10.dp))
                WeightLine(weights.map { it.weightKg }, targetKg)
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Add your weight on the Progress tab every week or so to see your trend here.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (targetKg != null) {
                Spacer(Modifier.height(10.dp))
                val away = abs(current - targetKg)
                Text(
                    "Goal ${"%.1f".format(targetKg)} kg · ${"%.1f".format(away)} kg to go" +
                        (weeksToGoal?.let { " · about $it weeks at your plan" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (weeklyChangeKg != 0f) {
                Spacer(Modifier.height(8.dp))
                Text("Set a goal weight on the Plan tab to see an estimate.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LogWeightDialog(currentKg: Float, onSave: (Float) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("%.1f".format(currentKg)) }
    val value = text.toFloatOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log your weight") },
        text = {
            Column {
                Text("Weigh yourself at the same time of day for the most useful trend.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Weight (kg)") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
            }
        },
        confirmButton = { TextButton(onClick = { value?.let(onSave) }, enabled = value != null && value in 20f..400f) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun WeightLine(values: List<Float>, targetKg: Float?) {
    val lineColor = MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val all = values + listOfNotNull(targetKg)
        val lo = all.min() - 0.5f
        val hi = all.max() + 0.5f
        val span = (hi - lo).coerceAtLeast(0.5f)
        fun y(v: Float) = size.height - (v - lo) / span * size.height
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        if (targetKg != null) {
            drawLine(targetColor, Offset(0f, y(targetKg)), Offset(size.width, y(targetKg)), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        }
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * stepX, y(v)) }
        drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        values.forEachIndexed { i, v -> drawCircle(lineColor, 4.dp.toPx(), Offset(i * stepX, y(v))) }
    }
}
