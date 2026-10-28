package com.freegymtrainer.app.presentation.nutrition

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.freegymtrainer.app.data.local.entity.MealLogEntry
import com.freegymtrainer.app.domain.usecase.NutritionTargets
import com.freegymtrainer.app.presentation.components.FoodIcon
import com.freegymtrainer.app.presentation.components.foodIconFile
import com.freegymtrainer.app.presentation.components.mealIconFile
import kotlin.math.roundToInt

private val WaterBlue = Color(0xFF3FA9F5)

/** Today at a glance: a PROTEIN ring (the number this app wants front and centre) with what's
 * left to hit the target, plus calories / carbs / fat progress bars underneath. */
@Composable
fun CalorieSummaryCard(consumed: Int, targets: NutritionTargets, protein: Int, carbs: Int, fat: Int, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        (protein.toFloat() / targets.proteinGrams.coerceAtLeast(1)).coerceIn(0f, 1f), tween(700), label = "proteinRing"
    )
    val remaining = (targets.proteinGrams - protein).coerceAtLeast(0)
    val targetMet = protein >= targets.proteinGrams
    val ringColor = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Today", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(128.dp).semantics {
                        contentDescription = "$protein of ${targets.proteinGrams} grams protein eaten" + if (targetMet) ", target reached" else ", $remaining grams left"
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(128.dp)) {
                        val stroke = 14.dp.toPx()
                        val inset = stroke / 2
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                        drawArc(ringColor, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (targetMet) "${protein}g" else "$remaining",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (targetMet) "protein hit" else "g protein left",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacroBar("Calories", consumed, targets.calorieTarget, MaterialTheme.colorScheme.tertiary, unit = "kcal")
                    MacroBar("Carbs", carbs, targets.carbGrams, MaterialTheme.colorScheme.secondary)
                    MacroBar("Fat", fat, targets.fatGrams, MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "$protein g of ${targets.proteinGrams} g protein eaten today. Estimates use the Mifflin-St Jeor formula - individual needs vary.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MacroBar(label: String, consumed: Int, target: Int, color: Color, unit: String = "g") {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("$consumed / $target $unit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (consumed.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.16f),
            strokeCap = StrokeCap.Round
        )
    }
}

/** One 250 ml glass, filled [fill] (0..1) from the bottom. */
@Composable
private fun WaterGlass(fill: Float, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    Canvas(modifier.size(width = 25.dp, height = 34.dp)) {
        val w = size.width
        val h = size.height
        val topInset = 0f
        val bottomInset = w * 0.16f
        val glass = Path().apply {
            moveTo(topInset, 0f)
            lineTo(w - topInset, 0f)
            lineTo(w - bottomInset, h)
            lineTo(bottomInset, h)
            close()
        }
        if (fill > 0f) {
            clipPath(glass) {
                drawRect(WaterBlue, Offset(0f, h * (1f - fill)), Size(w, h * fill))
            }
        }
        drawPath(glass, outline, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun WaterCard(waterMl: Int, targetMl: Int, onAdd: (Int) -> Unit, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    val glassMl = 250
    val glasses = (targetMl / glassMl.toFloat()).roundToInt().coerceIn(6, 12)
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Water", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${"%.2f".format(waterMl / 1000f)} / ${"%.1f".format(targetMl / 1000f)} L",
                    style = MaterialTheme.typography.titleMedium,
                    color = WaterBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(glasses) { i ->
                    WaterGlass(((waterMl - i * glassMl) / glassMl.toFloat()).coerceIn(0f, 1f))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Each glass = $glassMl ml", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { onAdd(250) }) { Text("+250 ml") }
                FilledTonalButton(onClick = { onAdd(500) }) { Text("+500 ml") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onUndo, enabled = waterMl > 0) {
                    Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Undo")
                }
            }
        }
    }
}

private val MEAL_ORDER = listOf("Breakfast", "Mid Morning", "Lunch", "Pre Workout", "Post Workout", "Snack", "Dinner")

/** Today's logged food grouped by meal, each a collapsible card with a subtotal, illustrated per
 * item - tap the header to expand/collapse, tap an item to see its full nutrient breakdown. */
@Composable
fun MealLogSection(meals: List<MealLogEntry>, onRemove: (MealLogEntry) -> Unit, onItemClick: (MealLogEntry) -> Unit = {}) {
    if (meals.isEmpty()) {
        ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FoodIcon("plate-dinner", size = 72.dp)
                Spacer(Modifier.height(12.dp))
                Text("Nothing logged yet today", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap Log Food to add what you ate, or log a whole meal from your plan above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }
    val grouped = meals.groupBy { it.mealType }
    val order = (MEAL_ORDER + grouped.keys).distinct().filter { it in grouped }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        order.forEach { type ->
            val entries = grouped.getValue(type)
            MealLogGroupCard(type = type, entries = entries, onRemove = onRemove, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun MealLogGroupCard(
    type: String, entries: List<MealLogEntry>, onRemove: (MealLogEntry) -> Unit, onItemClick: (MealLogEntry) -> Unit
) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable(type) { androidx.compose.runtime.mutableStateOf(false) }
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.animateContentSize()) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FoodIcon(mealIconFile(type), size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(type, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${entries.sumOf { it.protein.toDouble() }.roundToInt()} g protein",
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "  ·  ${entries.sumOf { it.calories.toDouble() }.roundToInt()} kcal",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 10.dp)) {
                    entries.forEach { meal ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onItemClick(meal) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FoodIcon(foodIconFile(meal.foodName), size = 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(meal.foodName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    (if (meal.servingGrams > 0f) "${meal.servingGrams.roundToInt()} g  ·  " else "") +
                                        "${meal.protein.roundToInt()} g protein  ·  ${meal.calories.roundToInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onRemove(meal) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove ${meal.foodName}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
