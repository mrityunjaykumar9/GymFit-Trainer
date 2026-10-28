package com.freegymtrainer.app.presentation.nutrition

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.data.repository.ResolvedNutrition
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.domain.nutrition.DietGoal
import com.freegymtrainer.app.domain.nutrition.DietPrefs
import com.freegymtrainer.app.domain.nutrition.DietStyle
import com.freegymtrainer.app.domain.nutrition.MacroStyle
import com.freegymtrainer.app.domain.nutrition.PlanMeal
import com.freegymtrainer.app.domain.nutrition.Portions
import com.freegymtrainer.app.domain.nutrition.ShoppingList
import com.freegymtrainer.app.domain.nutrition.isVegan
import com.freegymtrainer.app.data.local.entity.isVeg
import com.freegymtrainer.app.presentation.components.FoodIcon
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.foodIconFile
import com.freegymtrainer.app.presentation.components.mealIconFile
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/** The Plan tab: choose a goal and diet, see the targets it produces, and follow a 7-day plan built from them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietPlanTab(
    container: AppContainer, settings: SettingsDataStore, snackbar: SnackbarHostState, onOpenGuide: () -> Unit,
    onItemClick: (com.freegymtrainer.app.data.local.entity.Food, Float) -> Unit = { _, _ -> }
) {
    val vm: DietPlanViewModel = viewModel(key = "dietPlan", factory = GenericViewModelFactory { DietPlanViewModel(container, settings) })
    val state by vm.uiState.collectAsState()
    var showShopping by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.messageShown() }
    }

    if (state.loading) { LoadingState(); return }
    val resolved = state.resolved
    if (resolved == null) {
        Text(
            "Complete onboarding to get a diet plan built for you.",
            style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp)
        )
        return
    }
    val targets = resolved.targets
    val prefs = resolved.prefs

    Column(Modifier.padding(16.dp)) {
        PlanSummaryCard(resolved, state.weeksToGoal)
        Spacer(Modifier.height(16.dp))
        PlanSetupCard(
            prefs = prefs,
            currentWeightKg = state.currentWeightKg,
            onChange = { vm.updatePrefs(it) }
        )

        Spacer(Modifier.height(16.dp))
        ProteinSourcesCard(prefs.style)

        Spacer(Modifier.height(20.dp))
        Text("Your 7-day plan", style = MaterialTheme.typography.titleLarge)
        Text(
            "Portions are sized to your ${targets.calorieTarget} kcal and ${targets.proteinGrams} g protein target. Swap anything for a similar food.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        DaySelectorRow(selectedDay = state.selectedDay, today = LocalDate.now().dayOfWeek.value, onSelect = { vm.selectDay(it) })
        Spacer(Modifier.height(14.dp))

        val day = state.selected
        if (day == null) {
            Text("No plan could be built for these choices.", style = MaterialTheme.typography.bodyMedium)
        } else {
            day.meals.forEach { meal ->
                PlanMealCard(meal = meal, onLog = { vm.logMeal(meal) }, onItemClick = onItemClick, proteinTargetGrams = targets.proteinGrams)
                Spacer(Modifier.height(10.dp))
            }
            PlanDayTotalCard(
                planCalories = day.calories.roundToInt(), planProtein = day.protein.roundToInt(),
                calorieTarget = targets.calorieTarget, proteinTarget = targets.proteinGrams
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealPlanMacroPill(Icons.Filled.LocalFireDepartment, "${day.carbs.roundToInt()} g carbs")
                MealPlanMacroPill(Icons.Filled.FitnessCenter, "${day.fat.roundToInt()} g fat")
                MealPlanMacroPill(Icons.Filled.Info, "${day.fiber.roundToInt()} g fiber")
            }
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(onClick = { vm.logDay(day) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Log the whole day")
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { showShopping = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Shopping list")
            }
            OutlinedButton(onClick = onOpenGuide, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Diet guide")
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "General nutrition guidance based on common formulas - not medical advice. If you have a health condition, are pregnant, or have a history of disordered eating, check with a doctor or dietitian first.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showShopping) {
        ShoppingListSheet(
            groups = state.shopping, checked = state.shoppingChecked,
            onToggle = { vm.toggleShopping(it) }, onClear = { vm.clearShopping() },
            onDismiss = { showShopping = false }
        )
    }
}

/** The headline of the plan: calories, macros and what pace of change they produce. */
@Composable
private fun PlanSummaryCard(resolved: ResolvedNutrition, weeksToGoal: Int?) {
    val t = resolved.targets
    val p = resolved.prefs
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "${p.goal.label.uppercase()} PLAN · ${p.style.label.uppercase()}",
                style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%,d".format(t.calorieTarget), style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("kcal / day", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill("Protein ${t.proteinGrams} g")
                SummaryPill("Carbs ${t.carbGrams} g")
                SummaryPill("Fat ${t.fatGrams} g")
                SummaryPill("Fiber ${t.fiberGrams} g")
            }
            Spacer(Modifier.height(12.dp))
            val change = t.weeklyChangeKg
            val line = when {
                p.goal == DietGoal.LOSE_FAT && change < 0 -> "About %.2f kg of fat loss per week · maintenance is %,d kcal".format(abs(change), t.tdee)
                p.goal == DietGoal.BUILD_MUSCLE && change > 0 -> "About %.2f kg of gain per week · maintenance is %,d kcal".format(change, t.tdee)
                else -> "Eating at maintenance (%,d kcal) to hold your weight".format(t.tdee)
            }
            Text(line, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            if (weeksToGoal != null && p.targetWeightKg != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Goal ${"%.1f".format(p.targetWeightKg)} kg in about $weeksToGoal weeks",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold
                )
            }
            if (t.safetyNote.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(t.safetyNote, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.92f))
            }
        }
    }
}

@Composable
private fun SummaryPill(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun PlanSetupCard(prefs: DietPrefs, currentWeightKg: Float, onChange: ((DietPrefs) -> DietPrefs) -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Customize your plan", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            SetupLabel("Goal")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DietGoal.values().forEach { g ->
                    FilterChip(selected = prefs.goal == g, onClick = { onChange { it.copy(goal = g) } }, label = { Text(g.label) })
                }
            }
            Text(prefs.goal.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (prefs.goal == DietGoal.LOSE_FAT || prefs.goal == DietGoal.BUILD_MUSCLE) {
                Spacer(Modifier.height(12.dp))
                SetupLabel(if (prefs.goal == DietGoal.LOSE_FAT) "How fast?" else "How fast?")
                val paces = if (prefs.goal == DietGoal.LOSE_FAT) DietPrefs.PACES_LOSE else DietPrefs.PACES_GAIN
                val names = if (prefs.goal == DietGoal.LOSE_FAT) listOf("Gentle", "Steady", "Faster") else listOf("Lean gain", "Standard")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    paces.forEachIndexed { i, pace ->
                        FilterChip(
                            selected = abs(prefs.paceKgPerWeek - pace) < 0.01f,
                            onClick = { onChange { it.copy(paceKgPerWeek = pace) } },
                            label = { Text("${names[i]} · ${"%.2f".format(pace)} kg/wk") }
                        )
                    }
                }
                if (prefs.goal == DietGoal.LOSE_FAT && prefs.paceKgPerWeek >= 0.75f) {
                    Text(
                        "A faster pace is harder to stick to and can cost muscle. Steady is the best choice for most people.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            SetupLabel("What do you eat?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DietStyle.values().reversed().forEach { s ->
                    FilterChip(selected = prefs.style == s, onClick = { onChange { it.copy(style = s) } }, label = { Text(s.label) })
                }
            }

            Spacer(Modifier.height(12.dp))
            SetupLabel("Macro style")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroStyle.values().forEach { m ->
                    FilterChip(selected = prefs.macroStyle == m, onClick = { onChange { it.copy(macroStyle = m) } }, label = { Text(m.label) })
                }
            }
            Text(prefs.macroStyle.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(12.dp))
            SetupLabel("Meals per day")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 4, 5).forEach { n ->
                    FilterChip(selected = prefs.mealsPerDay == n, onClick = { onChange { it.copy(mealsPerDay = n) } }, label = { Text("$n meals") })
                }
            }

            if (prefs.goal != DietGoal.MAINTAIN && prefs.goal != DietGoal.RECOMP) {
                Spacer(Modifier.height(12.dp))
                SetupLabel("Goal weight (optional)")
                var text by remember(prefs.targetWeightKg) { mutableStateOf(prefs.targetWeightKg?.let { "%.1f".format(it) } ?: "") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                        label = { Text("kg") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    val value = text.toFloatOrNull()
                    FilledTonalButton(
                        onClick = { onChange { it.copy(targetWeightKg = value?.takeIf { v -> v in 30f..300f }) } },
                        enabled = text.isBlank() || (value != null && value in 30f..300f)
                    ) { Text(if (text.isBlank()) "Clear" else "Set") }
                }
                if (currentWeightKg > 0f) {
                    Text("Current weight: ${"%.1f".format(currentWeightKg)} kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private class ProteinSource(val food: String, val perServing: String, val icon: String)

/** Best protein foods for the chosen diet style, highest-protein first. Kept as a short,
 * curated list (not pulled from the full food database) so it reads as a clear "lean on these"
 * recommendation rather than another long scrollable table - that already exists in the guide. */
private fun proteinSourcesFor(style: DietStyle): List<ProteinSource> = when (style) {
    DietStyle.NON_VEG -> listOf(
        ProteinSource("Chicken breast", "31 g / 100 g cooked", "meat-cooked"),
        ProteinSource("Fish (white/tuna)", "22-26 g / 100 g", "fish"),
        ProteinSource("Eggs", "6 g each", "egg-half"),
        ProteinSource("Greek yogurt", "20 g / 200 g", "pudding"),
        ProteinSource("Paneer", "18 g / 100 g", "cheese-cut"),
        ProteinSource("Whey protein", "22-24 g / scoop", "carton-small")
    )
    DietStyle.EGGETARIAN -> listOf(
        ProteinSource("Eggs", "6 g each", "egg-half"),
        ProteinSource("Paneer", "18 g / 100 g", "cheese-cut"),
        ProteinSource("Greek yogurt", "20 g / 200 g", "pudding"),
        ProteinSource("Soya chunks", "21 g / 40 g dry", "soy"),
        ProteinSource("Dal / lentils", "18 g / bowl cooked", "bowl-soup"),
        ProteinSource("Whey protein", "22-24 g / scoop", "carton-small")
    )
    DietStyle.VEGETARIAN -> listOf(
        ProteinSource("Paneer", "18 g / 100 g", "cheese-cut"),
        ProteinSource("Greek yogurt", "20 g / 200 g", "pudding"),
        ProteinSource("Soya chunks", "21 g / 40 g dry", "soy"),
        ProteinSource("Dal / rajma / chole", "14-18 g / bowl", "bowl-soup"),
        ProteinSource("Tofu", "12 g / 150 g", "soy"),
        ProteinSource("Whey protein", "22-24 g / scoop", "carton-small")
    )
    DietStyle.VEGAN -> listOf(
        ProteinSource("Soya chunks", "21 g / 40 g dry", "soy"),
        ProteinSource("Tofu", "12 g / 150 g", "soy"),
        ProteinSource("Dal / rajma / chole", "14-18 g / bowl", "bowl-soup"),
        ProteinSource("Peanuts / peanut butter", "7 g / 2 tbsp", "peanut-butter"),
        ProteinSource("Soy milk", "7 g / 250 ml", "carton"),
        ProteinSource("Pumpkin / chia seeds", "5-7 g / 2 tbsp", "bag")
    )
}

/** "Lean on these" protein foods for the diet style currently selected above - the single
 * clearest way to answer "I'm veg/non-veg, what should I actually eat for protein?" without
 * making the person dig through the full food database or the separate guide screen. */
@Composable
private fun ProteinSourcesCard(style: DietStyle) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Best protein sources for ${style.label}", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Lean on these to hit your protein target without overshooting calories.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            proteinSourcesFor(style).forEach { source ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FoodIcon(source.icon, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(source.food, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(source.perServing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SetupLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
}

/**
 * One meal of the generated plan: collapsed to a single summary row by default (tap to expand
 * and see each item), protein leading over calories since that's the number this app wants
 * front and centre. Tapping an item opens its full nutrient breakdown; tapping "Log" works
 * either collapsed or expanded.
 */
@Composable
fun PlanMealCard(
    meal: PlanMeal,
    onLog: () -> Unit,
    onItemClick: (com.freegymtrainer.app.data.local.entity.Food, Float) -> Unit = { _, _ -> },
    proteinTargetGrams: Int? = null,
    startExpanded: Boolean = false
) {
    var expanded by rememberSaveable(meal.slot) { mutableStateOf(startExpanded) }
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.animateContentSize()) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FoodIcon(mealIconFile(meal.slot), size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(meal.slot, style = MaterialTheme.typography.titleMedium)
                    Text(meal.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${meal.protein.roundToInt()} g protein", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "  ·  ${meal.calories.roundToInt()} kcal", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onLog, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log", style = MaterialTheme.typography.labelLarge)
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
            if (expanded) {
                Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    meal.items.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onItemClick(item.food, item.grams) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FoodIcon(foodIconFile(item.food.name, item.food.category), size = 34.dp)
                            Spacer(Modifier.width(10.dp))
                            val veg = item.food.isVeg()
                            Box(
                                Modifier.size(16.dp).clip(CircleShape).background((if (veg) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(if (veg) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.food.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    Portions.describe(item.food.name, item.grams, Portions.isLiquid(item.food.name, item.food.category)),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${item.protein.roundToInt()} g", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold
                                )
                                Text("${item.calories.roundToInt()} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MealPlanMacroPill(Icons.Filled.FitnessCenter, "${meal.protein.roundToInt()} g protein")
                        MealPlanMacroPill(Icons.Filled.LocalFireDepartment, "${meal.calories.roundToInt()} kcal")
                        if (proteinTargetGrams != null && proteinTargetGrams > 0) {
                            MealPlanMacroPill(Icons.Filled.FitnessCenter, "${((meal.protein / proteinTargetGrams) * 100).roundToInt()}% of daily protein")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingListSheet(
    groups: List<com.freegymtrainer.app.domain.nutrition.ShoppingGroup>,
    checked: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text("Shopping list", style = MaterialTheme.typography.titleLarge)
            Text(
                "Everything for the 7-day plan. Quantities are cooked / ready-to-eat weights.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, ShoppingList.asText(groups))
                    }
                    context.startActivity(Intent.createChooser(send, "Share shopping list"))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
                TextButton(onClick = onClear, enabled = checked.isNotEmpty()) { Text("Clear ticks") }
            }
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                groups.forEach { group ->
                    item(key = "h-${group.title}") {
                        Text(
                            group.title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                        )
                    }
                    items(group.items, key = { "i-${it.name}" }) { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onToggle(item.name) }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = item.name in checked, onCheckedChange = { onToggle(item.name) })
                            Text(
                                item.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                                color = if (item.name in checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            Text("${item.grams} g", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
