package com.freegymtrainer.app.presentation.nutrition

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freegymtrainer.app.ads.AdBanner
import com.freegymtrainer.app.ads.InterstitialAdManager
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.domain.usecase.NutritionTargets
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.GenericViewModelFactory
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.presentation.components.AppTopBar
import com.freegymtrainer.app.presentation.components.FoodIcon
import com.freegymtrainer.app.presentation.components.LoadingState
import com.freegymtrainer.app.presentation.components.foodIconFile

@Composable
fun NutritionScreen(
    container: AppContainer,
    settings: SettingsDataStore,
    onOpenFoodList: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val viewModel: NutritionViewModel = viewModel(factory = GenericViewModelFactory { NutritionViewModel(container, settings) })
    val state by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAddFoodSheet by remember { mutableStateOf(false) }
    var sheetFood by remember { mutableStateOf<Food?>(null) }
    var detailRequest by remember { mutableStateOf<NutrientDetailRequest?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val activity = LocalContext.current as? Activity

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Nutrition") },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (tab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { sheetFood = null; showAddFoodSheet = true },
                    text = { Text("Log Food") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            NutritionHeroBanner(consumedCalories = state.consumedCalories, consumedProtein = state.consumedProtein, targets = state.targets)
            NutritionTabBar(selected = tab, onSelect = { tab = it })
            when (tab) {
                0 -> TodayTab(
                    state = state, viewModel = viewModel, onOpenFoodList = onOpenFoodList, onOpenGuide = onOpenGuide,
                    onPickSuggestion = { food -> sheetFood = food; showAddFoodSheet = true },
                    onDetailRequest = { req -> detailRequest = req }
                )
                1 -> {
                    DietPlanTab(
                        container, settings, snackbar, onOpenGuide,
                        onItemClick = { food, grams -> detailRequest = NutrientDetailRequest.FromFood(food, grams) }
                    )
                    Column(Modifier.padding(horizontal = 16.dp)) { AdBanner(); Spacer(Modifier.height(24.dp)) }
                }
                else -> {
                    InsightsTab(container, settings)
                    Column(Modifier.padding(horizontal = 16.dp)) { AdBanner(); Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    detailRequest?.let { req ->
        NutrientDetailHost(
            request = req, container = container, proteinTargetGrams = state.targets?.proteinGrams,
            onDismiss = { detailRequest = null }
        )
    }

    if (showAddFoodSheet) {
        AddFoodSheet(
            state = state,
            initialFood = sheetFood,
            onQueryChange = { viewModel.setFoodSearchQuery(it) },
            onLog = { food, mealType, grams ->
                viewModel.logFood(food, mealType, grams)
                showAddFoodSheet = false
                // Same natural-break-point idea as a finished workout - logging a meal is a
                // deliberate, multi-step action just reaching completion.
                if (activity != null) InterstitialAdManager.showIfReady(activity, onDismissed = {})
            },
            onLogCustom = { mealType, name, kcal, p, c, f ->
                viewModel.logCustomFood(mealType, name, kcal, p, c, f)
                showAddFoodSheet = false
            },
            onDismiss = { showAddFoodSheet = false },
            mealTypeOptions = viewModel.mealTypeOptions
        )
    }
}

/** Everything about today: targets, coaching, water, fasting, today's planned meals and what was eaten. */
@Composable
private fun TodayTab(
    state: NutritionUiState,
    viewModel: NutritionViewModel,
    onOpenFoodList: () -> Unit,
    onOpenGuide: () -> Unit,
    onPickSuggestion: (Food) -> Unit,
    onDetailRequest: (NutrientDetailRequest) -> Unit
) {
    if (state.loading) { LoadingState(Modifier.height(200.dp)); return }
    val targets = state.targets
    Column(Modifier.padding(16.dp)) {
        if (targets == null) {
            Text("Complete onboarding to see your personalized nutrition targets.", style = MaterialTheme.typography.bodyLarge)
            return
        }

        CalorieSummaryCard(
            consumed = state.consumedCalories, targets = targets,
            protein = state.consumedProtein, carbs = state.consumedCarbs, fat = state.consumedFat
        )

        if (state.tips.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            CoachCard(state.tips)
        }

        val plan = state.todayPlan
        if (plan != null) {
            Spacer(Modifier.height(26.dp))
            SectionHeading("Today's plan")
            Spacer(Modifier.height(10.dp))
            TodaysPlanHeader(state)
            Spacer(Modifier.height(12.dp))
            plan.meals.forEach { meal ->
                PlanMealCard(
                    meal = meal, onLog = { viewModel.logPlanMeal(meal) },
                    onItemClick = { food, grams -> onDetailRequest(NutrientDetailRequest.FromFood(food, grams)) },
                    proteinTargetGrams = targets.proteinGrams
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        if (state.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            SectionHeading("Quick add, based on what's left")
            Spacer(Modifier.height(10.dp))
            SuggestionsRow(
                suggestions = state.suggestions,
                remainingKcal = (targets.calorieTarget - state.consumedCalories).coerceAtLeast(0),
                remainingProtein = (targets.proteinGrams - state.consumedProtein).coerceAtLeast(0),
                onPick = { onPickSuggestion(it.food) }
            )
        }

        Spacer(Modifier.height(26.dp))
        SectionHeading("Track")
        Spacer(Modifier.height(10.dp))
        WaterCard(
            waterMl = state.waterMl, targetMl = targets.waterMl,
            onAdd = { viewModel.addWater(it) }, onUndo = { viewModel.undoWater() }
        )
        Spacer(Modifier.height(12.dp))
        FastingCard(
            protocol = state.fastingProtocol, startMinute = state.fastingStartMinute,
            onProtocol = { viewModel.setFastingProtocol(it) }, onShiftStart = { viewModel.shiftFastingStart(it) }
        )

        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeading("What you ate today", modifier = Modifier.weight(1f))
            TextButton(onClick = { viewModel.copyYesterday() }) { Text("Copy yesterday") }
        }
        Spacer(Modifier.height(6.dp))
        MealLogSection(
            meals = state.meals, onRemove = { viewModel.deleteMeal(it) },
            onItemClick = { entry -> onDetailRequest(NutrientDetailRequest.FromLog(entry)) }
        )

        Spacer(Modifier.height(26.dp))
        SectionHeading("Explore")
        Spacer(Modifier.height(10.dp))
        FoodDatabasePromoCard(onClick = onOpenFoodList)
        Spacer(Modifier.height(12.dp))
        GuideLinkCard(onClick = onOpenGuide)

        Spacer(Modifier.height(20.dp))
        AdBanner()
        Spacer(Modifier.height(80.dp)) // room for FAB
    }
}

/**
 * The Today / Plan / Insights switcher, redesigned as a bold pill-shaped segmented control
 * (icon + label, a solid filled pill for the selected segment) instead of a plain text TabRow -
 * the old thin underline tabs read as barely-there against the screen below them.
 */
@Composable
private fun NutritionTabBar(selected: Int, onSelect: (Int) -> Unit) {
    val segments = listOf(Triple("Today", Icons.Filled.Today, 0), Triple("Plan", Icons.Filled.CalendarMonth, 1), Triple("Insights", Icons.Filled.Insights, 2))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEach { (label, icon, index) ->
            val isSelected = selected == index
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon, contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label, style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                )
            }
        }
    }
}

/** A consistent, quiet section label - the Today tab stacks several distinct groups (plan,
 * quick-add, tracking, log, explore), and without a visible seam between them it used to read as
 * one long wall of cards. */
@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = modifier
    )
}

@Composable
private fun TodaysPlanHeader(state: NutritionUiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Today, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Today's meals from your plan", style = MaterialTheme.typography.titleMedium)
            val todayName = DAY_FULL.getOrElse(java.time.LocalDate.now().dayOfWeek.value - 1) { "" }
            val goal = state.prefs?.goal?.label ?: ""
            Text(
                "$todayName · $goal · ${state.prefs?.style?.label ?: ""}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Gradient header leading with calories AND protein - protein gets equal billing here (a small
 * ring, not just a number buried in a pill below) since it's the macro that actually drives
 * whether a goal (fat loss without losing muscle, or building muscle at all) succeeds, and it's
 * the one most diets in India under-deliver on by default.
 */
@Composable
private fun NutritionHeroBanner(consumedCalories: Int, consumedProtein: Int, targets: NutritionTargets?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primary, Color.Black, 0.6f)))
            )
        )
        if (targets == null) {
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                Text("Fuel Your Progress", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Text("Complete onboarding for personalized targets", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
        } else {
            Row(
                Modifier.fillMaxSize().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("TODAY'S PROTEIN", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$consumedProtein", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(" / ${targets.proteinGrams} g", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    HeroStatBar("Calories", consumedCalories, targets.calorieTarget, "kcal")
                }
                Spacer(Modifier.width(14.dp))
                HeroProteinRing(consumedProtein, targets.proteinGrams)
            }
        }
    }
}

@Composable
private fun HeroStatBar(label: String, consumed: Int, target: Int, unit: String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
            Text("$consumed / $target $unit", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (consumed.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.25f)
        )
    }
}

/** A small ring, the same visual language as the calorie ring on the detail card below, so protein reads as equally important at a glance. */
@Composable
private fun HeroProteinRing(consumed: Int, target: Int) {
    val progress = (consumed.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(78.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.25f), 0f, 360f, false, androidx.compose.ui.geometry.Offset(inset, inset), arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            drawArc(Color.White, -90f, 360f * progress, false, androidx.compose.ui.geometry.Offset(inset, inset), arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
        Row(
            Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy((-14).dp)
        ) {
            listOf("apple", "broccoli", "egg-half").forEach { FoodIcon(it, size = 52.dp, background = Color.White.copy(alpha = 0.92f)) }
        }
        Column(Modifier.padding(18.dp)) {
            Spacer(Modifier.height(18.dp))
            Spacer(Modifier.height(14.dp))
            Text("FOOD DATABASE", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(4.dp))
            Text("150+ Foods, Fully Detailed", style = MaterialTheme.typography.headlineSmall, color = Color.White)
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

private fun defaultMealType(options: List<String>): String {
    val hour = java.time.LocalTime.now().hour
    val guess = when {
        hour < 10 -> "Breakfast"
        hour < 12 -> "Mid Morning"
        hour < 16 -> "Lunch"
        hour < 19 -> "Pre Workout"
        else -> "Dinner"
    }
    return if (guess in options) guess else options.first()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodSheet(
    state: NutritionUiState,
    initialFood: Food?,
    onQueryChange: (String) -> Unit,
    onLog: (Food, String, Float) -> Unit,
    onLogCustom: (mealType: String, name: String, kcal: Float, protein: Float, carbs: Float, fat: Float) -> Unit,
    onDismiss: () -> Unit,
    mealTypeOptions: List<String>
) {
    // The search text lives in the ViewModel, so start each time the sheet opens from a clean slate.
    LaunchedEffect(Unit) { onQueryChange("") }
    var customMode by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customKcal by remember { mutableStateOf("") }
    var customProtein by remember { mutableStateOf("") }
    var customCarbs by remember { mutableStateOf("") }
    var customFat by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf(initialFood) }
    var selectedMealType by remember { mutableStateOf(defaultMealType(mealTypeOptions)) }
    var grams by remember { mutableStateOf(initialFood?.defaultServingGrams?.toInt()?.toString() ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp).fillMaxWidth()
        ) {
            val food = selectedFood
            if (customMode) {
                Text("Add your own food", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Enter the numbers for the amount you ate (check the packet label if you have one).",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = customName, onValueChange = { customName = it.take(60) },
                    label = { Text("Food name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                val numberKeys = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                OutlinedTextField(
                    value = customKcal, onValueChange = { customKcal = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Calories (kcal) *") }, singleLine = true, keyboardOptions = numberKeys, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customProtein, onValueChange = { customProtein = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Protein g") }, singleLine = true, keyboardOptions = numberKeys, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customCarbs, onValueChange = { customCarbs = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Carbs g") }, singleLine = true, keyboardOptions = numberKeys, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customFat, onValueChange = { customFat = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Fat g") }, singleLine = true, keyboardOptions = numberKeys, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Meal", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mealTypeOptions.forEach { m ->
                        FilterChip(selected = selectedMealType == m, onClick = { selectedMealType = m }, label = { Text(m) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                val kcal = customKcal.toFloatOrNull() ?: 0f
                Button(
                    onClick = {
                        onLogCustom(
                            selectedMealType, customName, kcal,
                            customProtein.toFloatOrNull() ?: 0f, customCarbs.toFloatOrNull() ?: 0f, customFat.toFloatOrNull() ?: 0f
                        )
                    },
                    enabled = kcal > 0f,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add to $selectedMealType") }
                TextButton(onClick = { customMode = false }, modifier = Modifier.fillMaxWidth()) { Text("Back to search") }
            } else if (food == null) {
                Text("Log Food", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.foodSearchQuery, onValueChange = onQueryChange,
                    placeholder = { Text("Search foods...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.foodSearchQuery.isBlank() && state.recentFoods.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Recent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.recentFoods.forEach { recent ->
                            AssistChip(
                                onClick = { selectedFood = recent; grams = recent.defaultServingGrams.toInt().toString() },
                                label = { Text(recent.name, maxLines = 1) },
                                leadingIcon = { FoodIcon(foodIconFile(recent.name, recent.category), size = 22.dp) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { customMode = true }) { Text("Can't find it? Add your own food") }
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(state.foodResults, key = { it.id }) { result ->
                        ListItem(
                            leadingContent = { FoodIcon(foodIconFile(result.name, result.category), size = 44.dp) },
                            headlineContent = { Text(result.name) },
                            supportingContent = {
                                val f = result.defaultServingGrams / 100f
                                Text("${result.category}  ·  ${result.defaultServingGrams.toInt()} g = ${(result.caloriesPer100g * f).toInt()} kcal")
                            },
                            modifier = Modifier.clickable {
                                selectedFood = result
                                grams = result.defaultServingGrams.toInt().toString()
                            }
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FoodIcon(foodIconFile(food.name, food.category), size = 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(food.name, style = MaterialTheme.typography.titleLarge)
                        Text(food.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Meal", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mealTypeOptions.forEach { m ->
                        FilterChip(selected = selectedMealType == m, onClick = { selectedMealType = m }, label = { Text(m) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Portion", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                val servingChoices = listOf(0.5f to "½×", 1f to "1×", 1.5f to "1½×", 2f to "2×")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    servingChoices.forEach { (mult, label) ->
                        val g = (food.defaultServingGrams * mult).toInt()
                        FilterChip(
                            selected = grams == g.toString(),
                            onClick = { grams = g.toString() },
                            label = { Text("$label  ${g}g") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = grams, onValueChange = { grams = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Amount (g)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                val amount = grams.toFloatOrNull() ?: 0f
                val factor = amount / 100f
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PreviewStat("Calories", "${(food.caloriesPer100g * factor).toInt()}", "kcal")
                        PreviewStat("Protein", "${(food.proteinPer100g * factor).toInt()}", "g")
                        PreviewStat("Carbs", "${(food.carbsPer100g * factor).toInt()}", "g")
                        PreviewStat("Fat", "${(food.fatPer100g * factor).toInt()}", "g")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onLog(food, selectedMealType, amount.coerceIn(1f, 5000f)) },
                    enabled = amount >= 1f,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add to $selectedMealType") }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { selectedFood = null }, modifier = Modifier.fillMaxWidth()) { Text("Back to search") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PreviewStat(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value, style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text("$label ($unit)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
    }
}
