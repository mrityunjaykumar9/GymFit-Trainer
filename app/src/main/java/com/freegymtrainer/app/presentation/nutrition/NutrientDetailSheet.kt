package com.freegymtrainer.app.presentation.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.MealLogEntry
import com.freegymtrainer.app.data.local.entity.isVeg
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.presentation.components.FoodIcon
import com.freegymtrainer.app.presentation.components.foodIconFile
import kotlin.math.roundToInt

/** Per-100g reference figures, shown underneath the actual-serving numbers - null for a custom
 * logged entry that has no backing [Food] row to look the reference up from. */
data class NutrientPer100g(val calories: Float, val protein: Float, val carbs: Float, val fat: Float, val fiber: Float)

/** Which food the person tapped: a plan item (always has a real [Food]) or a logged entry
 * (may be a custom, DB-less entry, foodId == 0). [NutrientDetailHost] resolves either into the
 * same sheet so every tappable food item in the Nutrition section opens the same rich detail view. */
sealed class NutrientDetailRequest {
    data class FromFood(val food: Food, val grams: Float) : NutrientDetailRequest()
    data class FromLog(val entry: MealLogEntry) : NutrientDetailRequest()
}

/** Resolves a [NutrientDetailRequest] (looking the food up by id for a logged entry, when it has
 * one) and shows [NutrientDetailSheet] for it. One instance lives on NutritionScreen so every tab
 * shares the same popup. */
@Composable
fun NutrientDetailHost(
    request: NutrientDetailRequest,
    container: AppContainer,
    proteinTargetGrams: Int?,
    onDismiss: () -> Unit
) {
    when (request) {
        is NutrientDetailRequest.FromFood -> {
            val f = request.food
            val factor = request.grams / 100f
            NutrientDetailSheet(
                name = f.name, iconFile = foodIconFile(f.name, f.category), category = f.category, isVeg = f.isVeg(),
                grams = request.grams,
                calories = f.caloriesPer100g * factor, protein = f.proteinPer100g * factor,
                carbs = f.carbsPer100g * factor, fat = f.fatPer100g * factor, fiber = f.fiberPer100g * factor,
                per100g = NutrientPer100g(f.caloriesPer100g, f.proteinPer100g, f.carbsPer100g, f.fatPer100g, f.fiberPer100g),
                proteinTargetGrams = proteinTargetGrams,
                keyNutrients = f.keyNutrients, highlight = f.highlight,
                onDismiss = onDismiss
            )
        }
        is NutrientDetailRequest.FromLog -> {
            val entry = request.entry
            var food by remember(entry.id) { mutableStateOf<Food?>(null) }
            var loaded by remember(entry.id) { mutableStateOf(entry.foodId <= 0L) }
            LaunchedEffect(entry.id) {
                if (entry.foodId > 0L) {
                    food = container.nutritionRepository.getFood(entry.foodId)
                    loaded = true
                }
            }
            if (!loaded) {
                ModalBottomSheet(onDismissRequest = onDismiss) {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                return
            }
            val f = food
            if (f != null) {
                val factor = if (entry.servingGrams > 0f) entry.servingGrams / 100f else 1f
                NutrientDetailSheet(
                    name = f.name, iconFile = foodIconFile(f.name, f.category), category = f.category, isVeg = f.isVeg(),
                    grams = entry.servingGrams,
                    // Calories/protein/carbs/fat come from the log entry itself (what was actually
                    // recorded at the time), not recomputed from the food's current numbers.
                    calories = entry.calories, protein = entry.protein, carbs = entry.carbs, fat = entry.fat,
                    fiber = f.fiberPer100g * factor,
                    per100g = NutrientPer100g(f.caloriesPer100g, f.proteinPer100g, f.carbsPer100g, f.fatPer100g, f.fiberPer100g),
                    proteinTargetGrams = proteinTargetGrams,
                    keyNutrients = f.keyNutrients, highlight = f.highlight,
                    onDismiss = onDismiss
                )
            } else {
                NutrientDetailSheet(
                    name = entry.foodName, iconFile = foodIconFile(entry.foodName), category = "Custom entry", isVeg = null,
                    grams = entry.servingGrams,
                    calories = entry.calories, protein = entry.protein, carbs = entry.carbs, fat = entry.fat, fiber = 0f,
                    per100g = null, proteinTargetGrams = proteinTargetGrams,
                    keyNutrients = emptyList(), highlight = "",
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * Full nutrient breakdown for one food item, scaled to the actual serving someone tapped - opened
 * from a plan meal item or a logged meal item. Protein leads (a hero card of its own, with the
 * share of the day's protein target it covers) since that's the number this app wants people
 * paying attention to; calories/carbs/fat/fiber follow as a secondary row.
 */
@Composable
fun NutrientDetailSheet(
    name: String,
    iconFile: String,
    category: String,
    isVeg: Boolean?,
    grams: Float,
    calories: Float,
    protein: Float,
    carbs: Float,
    fat: Float,
    fiber: Float,
    per100g: NutrientPer100g?,
    proteinTargetGrams: Int?,
    keyNutrients: List<String>,
    highlight: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodIcon(iconFile, size = 60.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isVeg != null) {
                            Box(
                                Modifier.size(14.dp).clip(RoundedCornerShape(3.dp))
                                    .background((if (isVeg) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(if (isVeg) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error))
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (grams > 0f) {
                Text(
                    "FOR THIS SERVING · ${grams.roundToInt()} g".uppercase(), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
            }

            // Protein hero: the number this screen wants to lead with, not calories.
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${protein.roundToInt()} g", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("Protein", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                    }
                    if (proteinTargetGrams != null && proteinTargetGrams > 0) {
                        Text(
                            "${((protein / proteinTargetGrams) * 100).roundToInt()}%\nof daily target",
                            style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetStat("Calories", "${calories.roundToInt()}", Icons.Filled.LocalFireDepartment, Modifier.weight(1f))
                SheetStat("Carbs", "${carbs.roundToInt()}g", Icons.Filled.Grain, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetStat("Fat", "${fat.roundToInt()}g", Icons.Filled.Opacity, Modifier.weight(1f))
                SheetStat("Fiber", "${fiber.roundToInt()}g", Icons.Filled.Spa, Modifier.weight(1f))
            }

            if (keyNutrients.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SheetCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SheetIconBadge(Icons.Filled.HealthAndSafety, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Text("Vitamins & Minerals", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        keyNutrients.forEach { nutrient ->
                            AssistChip(
                                onClick = {}, label = { Text(nutrient, style = MaterialTheme.typography.labelMedium) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    }
                }
            }

            if (highlight.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(10.dp))
                        Text(highlight, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            if (per100g != null) {
                Spacer(Modifier.height(16.dp))
                Text("Per 100g reference", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                SheetCard {
                    SheetNutrientRow("Calories", "${per100g.calories.toInt()} kcal")
                    SheetNutrientRow("Protein", "${per100g.protein} g")
                    SheetNutrientRow("Carbohydrates", "${per100g.carbs} g")
                    SheetNutrientRow("Fat", "${per100g.fat} g")
                    SheetNutrientRow("Fiber", "${per100g.fiber} g", isLast = true)
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Custom entry - no vitamin/mineral or per-100g reference data to show.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SheetStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SheetCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SheetIconBadge(icon: ImageVector, containerColor: Color, contentColor: Color) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(containerColor),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun SheetNutrientRow(label: String, value: String, isLast: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
    if (!isLast) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
