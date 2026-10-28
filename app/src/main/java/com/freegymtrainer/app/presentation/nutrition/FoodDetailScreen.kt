package com.freegymtrainer.app.presentation.nutrition

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.isVeg
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.presentation.components.AppTopBar
import com.freegymtrainer.app.presentation.components.FoodIcon
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.foodIconFile
import com.freegymtrainer.app.presentation.components.StatCard

@Composable
fun FoodDetailScreen(container: AppContainer, foodId: Long, onBack: () -> Unit) {
    var food by remember(foodId) { mutableStateOf<Food?>(null) }
    LaunchedEffect(foodId) {
        food = container.nutritionRepository.getFood(foodId)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = food?.name ?: "",
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        val f = food
        if (f == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        val servingFactor = f.defaultServingGrams / 100f

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            FoodHeroBanner(food = f)

            Column(Modifier.padding(16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (f.isVeg()) "Vegetarian" else "Non-Vegetarian") },
                        leadingIcon = {
                            Icon(
                                if (f.isVeg()) Icons.Filled.Eco else Icons.Filled.Egg,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    AssistChip(onClick = {}, label = { Text(f.category) })
                }

                Spacer(Modifier.height(20.dp))
                Text("Per serving (${f.defaultServingGrams.toInt()}g)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        "Calories", "${(f.caloriesPer100g * servingFactor).toInt()}", Icons.Filled.LocalFireDepartment,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        "Protein", "${(f.proteinPer100g * servingFactor).toInt()}g", Icons.Filled.FitnessCenter,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        "Carbs", "${(f.carbsPer100g * servingFactor).toInt()}g", Icons.Filled.Grain,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        "Fat", "${(f.fatPer100g * servingFactor).toInt()}g", Icons.Filled.Opacity,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        "Fiber", "${(f.fiberPer100g * servingFactor).toInt()}g", Icons.Filled.Spa,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Mid-screen rather than after everything: this comes right after the core
                // macro stats someone opens a food page to check, so it's seen even by a user
                // who doesn't scroll into vitamins/highlight/per-100g reference below.
                Spacer(Modifier.height(16.dp))
                AdBanner()

                if (f.keyNutrients.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    DetailCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.HealthAndSafety, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Text("Vitamins & Minerals", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(12.dp))
                        FlowRowChips(f.keyNutrients)
                    }
                }

                if (f.highlight.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp)) {
                            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                f.highlight,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Per 100g reference", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                DetailCard {
                    NutrientRow("Calories", "${f.caloriesPer100g.toInt()} kcal")
                    NutrientRow("Protein", "${f.proteinPer100g} g")
                    NutrientRow("Carbohydrates", "${f.carbsPer100g} g")
                    NutrientRow("Fat", "${f.fatPer100g} g")
                    NutrientRow("Fiber", "${f.fiberPer100g} g", isLast = true)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Values are typical/approximate reference figures, not lab-measured for a specific batch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Colourful header with the food's icon, name and category. */
@Composable
private fun FoodHeroBanner(food: Food) {
    Box(Modifier.fillMaxWidth().height(170.dp)) {
        Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primary, Color.Black, 0.6f)))
                )
            )
        
        FoodIcon(
            foodIconFile(food.name, food.category),
            size = 96.dp,
            background = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp).padding(end = 110.dp)) {
            Text(food.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(food.category, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun FlowRowChips(items: List<String>) {
    // Real FlowRow: each chip wraps onto a new line as a whole unit based on its actual
    // measured width, so it's correct at any screen size or font scale - unlike a
    // character-count heuristic, which can still overflow a narrow phone or waste space on a
    // wide one.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { nutrient ->
            AssistChip(
                onClick = {},
                label = { Text(nutrient, style = MaterialTheme.typography.labelMedium) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        }
    }
}

@Composable
private fun NutrientRow(label: String, value: String, isLast: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
    if (!isLast) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun IconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
    }
}
