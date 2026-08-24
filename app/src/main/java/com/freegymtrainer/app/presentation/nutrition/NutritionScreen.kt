package com.freegymtrainer.app.presentation.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.presentation.components.LoadingState

@Composable
fun NutritionScreen(container: AppContainer) {
    val viewModel: NutritionViewModel = viewModel(factory = GenericViewModelFactory { NutritionViewModel(container) })
    val state by viewModel.uiState.collectAsState()
    var showAddFoodSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nutrition") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAddFoodSheet = true }, text = { Text("Log Food") }, icon = {})
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val targets = state.targets

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
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

            Spacer(Modifier.height(20.dp))
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

@Composable
private fun MacroColumn(label: String, consumed: Int, target: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$consumed / $target g", style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
