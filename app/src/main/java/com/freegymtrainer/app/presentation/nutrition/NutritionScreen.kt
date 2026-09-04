package com.freegymtrainer.app.presentation.nutrition

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.isVeg
import com.freegymtrainer.app.data.repository.MealPlanItemDetail
import com.freegymtrainer.app.data.repository.MealPlanMealDetail
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.presentation.components.AppTopBar
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.rememberAssetImageBitmap

private fun MealPlanItemDetail.calories(): Float = food.caloriesPer100g * grams / 100f
private fun MealPlanItemDetail.protein(): Float = food.proteinPer100g * grams / 100f
private fun MealPlanMealDetail.totalProtein(): Float = items.sumOf { it.protein().toDouble() }.toFloat()
private fun MealPlanMealDetail.totalCalories(): Float = items.sumOf { it.calories().toDouble() }.toFloat()

@Composable
fun NutritionScreen(container: AppContainer, settings: SettingsDataStore, onOpenFoodList: () -> Unit) {
    val viewModel: NutritionViewModel = viewModel(factory = GenericViewModelFactory { NutritionViewModel(container, settings) })
    val state by viewModel.uiState.collectAsState()
    var showAddFoodSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppTopBar(title = "Nutrition") },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAddFoodSheet = true }, text = { Text("Log Food") }, icon = {})
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val targets = state.targets

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            NutritionHeroBanner(consumedCalories = state.consumedCalories, calorieTarget = targets?.calorieTarget)

            Column(Modifier.padding(16.dp)) {
            if (targets == null) {
                Text("Complete onboarding to see your personalized nutrition targets.", style = MaterialTheme.typography.bodyLarge)
            } else {
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Calories", style = MaterialTheme.typography.titleMedium)
                        Text("${state.consumedCalories} / ${targets.calorieTarget} kcal", style = MaterialTheme.typography.headlineMedium)
                        LinearProgressIndicator(
                            progress = { (state.consumedCalories.toFloat() / targets.calorieTarget).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MacroColumn("Protein", state.consumedProtein, targets.proteinGrams)
                            MacroColumn("Carbs", state.consumedCarbs, targets.carbGrams)
                            MacroColumn("Fat", state.consumedFat, targets.fatGrams)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Estimates based on the Mifflin-St Jeor formula. Individual needs vary - adjust as needed.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("💧 Water", style = MaterialTheme.typography.titleMedium)
                            Text("${"%.2f".format(state.waterMl / 1000f)} / ${"%.1f".format(targets.waterMl / 1000f)} L", style = MaterialTheme.typography.titleMedium)
                        }
                        LinearProgressIndicator(
                            progress = { (state.waterMl.toFloat() / targets.waterMl).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(250, 500, 750).forEach { amount ->
                                OutlinedButton(onClick = { viewModel.addWater(amount) }) { Text("+${amount}ml") }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            FoodDatabasePromoCard(onClick = onOpenFoodList)

            Spacer(Modifier.height(24.dp))
            MealPlanSection(state = state, viewModel = viewModel)

            Spacer(Modifier.height(24.dp))
            Text("Today's Meals", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (state.meals.isEmpty()) {
                Text("Nothing logged yet today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.meals.forEach { meal ->
                    ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(meal.foodName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${meal.mealType} • ${meal.servingGrams.toInt()}g • ${meal.calories.toInt()} kcal",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.deleteMeal(meal) }) { Text("Remove") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            AdBanner()
            Spacer(Modifier.height(80.dp)) // room for FAB
            }
        }
    }

    if (showAddFoodSheet) {
        AddFoodSheet(
            state = state,
            onQueryChange = { viewModel.setFoodSearchQuery(it) },
            onLog = { food, mealType, grams -> viewModel.logFood(food, mealType, grams); showAddFoodSheet = false },
            onDismiss = { showAddFoodSheet = false },
            mealTypeOptions = viewModel.mealTypeOptions
        )
    }
}

/**
 * Uses the nutrition-themed photo you supplied (assets/nutrients_pics/nutrients.jpg) as a hero
 * banner, mirroring the Home/Workouts tabs so Nutrition doesn't feel like the plain, text-only
 * screen of the three.
 */
@Composable
private fun NutritionHeroBanner(consumedCalories: Int, calorieTarget: Int?) {
    val photo = rememberAssetImageBitmap("nutrients_pics/nutrients.jpg")

    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        if (photo != null) {
            Image(bitmap = photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.8f)))
                )
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                )
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text("Fuel Your Progress", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(
                if (calorieTarget != null) "$consumedCalories / $calorieTarget kcal logged today" else "Complete onboarding for personalized targets",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * A vibrant gradient promo rather than another plain white quick-link card - the Food Database
 * hides a lot behind one row (50+ foods, full macro breakdowns, vitamins & minerals per item -
 * see FoodDetailScreen), so the entry point deliberately looks different from every other card
 * on this screen instead of blending in and getting overlooked.
 */
@Composable
private fun FoodDatabasePromoCard(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
            .clickable(onClick = onClick)
    ) {
        Icon(
            Icons.Filled.MenuBook,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 2.dp)
                .size(110.dp)
        )
        Column(Modifier.padding(18.dp)) {
            Text("FOOD DATABASE", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(4.dp))
            Text("50+ Foods, Fully Detailed", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                "Calories, macros, vitamins & minerals for every item",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PromoBadge(Icons.Filled.HealthAndSafety, "Vitamins & Minerals")
                PromoBadge(Icons.Filled.Eco, "Veg & Non-Veg")
            }
        }
    }
}

@Composable
private fun PromoBadge(icon: ImageVector, label: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun MacroColumn(label: String, consumed: Int, target: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$consumed / $target g", style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * A suggested meal plan for the day - what to eat, one meal at a time, with each item's
 * protein/calories computed live from the Food database (not hardcoded), plus a day total
 * against the user's target. "Log this meal" copies the whole meal into the actual log in
 * one tap instead of adding each item by hand.
 */
private fun mealTypeIcon(mealType: String): ImageVector = when {
    mealType.contains("Breakfast", ignoreCase = true) -> Icons.Filled.FreeBreakfast
    mealType.contains("Morning", ignoreCase = true) -> Icons.Filled.Coffee
    mealType.contains("Lunch", ignoreCase = true) -> Icons.Filled.LunchDining
    mealType.contains("Post Workout", ignoreCase = true) -> Icons.Filled.Bolt
    mealType.contains("Workout", ignoreCase = true) -> Icons.Filled.FitnessCenter
    mealType.contains("Dinner", ignoreCase = true) -> Icons.Filled.DinnerDining
    else -> Icons.Filled.Restaurant
}

@Composable
private fun MealPlanSection(state: NutritionUiState, viewModel: NutritionViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("Meal Plan", style = MaterialTheme.typography.titleMedium)
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.dietPreference == "Veg",
            onClick = { viewModel.setDietPreference("Veg") },
            label = { Text("Veg") },
            leadingIcon = { Icon(Icons.Filled.Eco, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        FilterChip(
            selected = state.dietPreference == "Non-Veg",
            onClick = { viewModel.setDietPreference("Non-Veg") },
            label = { Text("Non-Veg") },
            leadingIcon = { Icon(Icons.Filled.Egg, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }

    val plan = state.mealPlan
    val targets = state.targets
    if (plan == null) {
        Spacer(Modifier.height(8.dp))
        Text(
            "No meal plan available for today yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Spacer(Modifier.height(14.dp))
    plan.meals.forEach { meal ->
        MealPlanCard(meal = meal, onLog = { viewModel.logPlannedMeal(meal) })
        Spacer(Modifier.height(10.dp))
    }

    if (targets != null) {
        val planProtein = plan.meals.sumOf { it.totalProtein().toDouble() }.toInt()
        val planCalories = plan.meals.sumOf { it.totalCalories().toDouble() }.toInt()
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
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
                    "$planCalories kcal  •  $planProtein g protein",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (planCalories.toFloat() / targets.calorieTarget).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your target: ${targets.calorieTarget} kcal  •  ${targets.proteinGrams} g protein",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun MealPlanCard(meal: MealPlanMealDetail, onLog: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        mealTypeIcon(meal.mealType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(meal.mealType, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = onLog, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(10.dp))
            meal.items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (item.food.isVeg()) Icons.Filled.Eco else Icons.Filled.Egg,
                        contentDescription = if (item.food.isVeg()) "Vegetarian" else "Non-vegetarian",
                        tint = if (item.food.isVeg()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${item.food.name} (${item.grams.toInt()}g)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${item.protein().toInt()}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroPill(Icons.Filled.LocalFireDepartment, "${meal.totalCalories().toInt()} kcal")
                MacroPill(Icons.Filled.FitnessCenter, "${meal.totalProtein().toInt()}g protein")
            }
        }
    }
}

@Composable
private fun MacroPill(icon: ImageVector, text: String) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodSheet(
    state: NutritionUiState,
    onQueryChange: (String) -> Unit,
    onLog: (Food, String, Float) -> Unit,
    onDismiss: () -> Unit,
    mealTypeOptions: List<String>
) {
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var selectedMealType by remember { mutableStateOf(mealTypeOptions.first()) }
    var grams by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            val food = selectedFood
            if (food == null) {
                Text("Log Food", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.foodSearchQuery, onValueChange = onQueryChange,
                    label = { Text("Search foods...") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(state.foodResults) { result ->
                        ListItem(
                            headlineContent = { Text(result.name) },
                            supportingContent = { Text("${result.category} • ${result.caloriesPer100g.toInt()} kcal/100g") },
                            modifier = Modifier.clickable(onClick = { selectedFood = result })
                        )
                    }
                }
            } else {
                Text(food.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Text("Meal", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    mealTypeOptions.take(3).forEach { m ->
                        FilterChip(selected = selectedMealType == m, onClick = { selectedMealType = m }, label = { Text(m) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    mealTypeOptions.drop(3).forEach { m ->
                        FilterChip(selected = selectedMealType == m, onClick = { selectedMealType = m }, label = { Text(m) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = grams, onValueChange = { grams = it.filter { c -> c.isDigit() } },
                    label = { Text("Serving size (g) — default ${food.defaultServingGrams.toInt()}g") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onLog(food, selectedMealType, grams.toFloatOrNull() ?: food.defaultServingGrams) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add to Log") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { selectedFood = null }, modifier = Modifier.fillMaxWidth()) { Text("Back to search") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
