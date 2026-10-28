package com.freegymtrainer.app.presentation.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.MealLogEntry
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.domain.nutrition.DayTotals
import com.freegymtrainer.app.domain.nutrition.DietPrefs
import com.freegymtrainer.app.domain.nutrition.FastingProtocol
import com.freegymtrainer.app.domain.nutrition.FoodSuggestion
import com.freegymtrainer.app.domain.nutrition.NutritionInsights
import com.freegymtrainer.app.domain.nutrition.PlanDay
import com.freegymtrainer.app.domain.nutrition.PlanMeal
import com.freegymtrainer.app.domain.nutrition.Tip
import com.freegymtrainer.app.domain.usecase.NutritionTargets
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class NutritionUiState(
    val loading: Boolean = true,
    val targets: NutritionTargets? = null,
    val prefs: DietPrefs? = null,
    val consumedCalories: Int = 0,
    val consumedProtein: Int = 0,
    val consumedCarbs: Int = 0,
    val consumedFat: Int = 0,
    val waterMl: Int = 0,
    val meals: List<MealLogEntry> = emptyList(),
    val foodSearchQuery: String = "",
    val foodResults: List<Food> = emptyList(),
    val recentFoods: List<Food> = emptyList(),
    /** Today's meals from the generated plan for the person's goal and diet. */
    val todayPlan: PlanDay? = null,
    val tips: List<Tip> = emptyList(),
    val suggestions: List<FoodSuggestion> = emptyList(),
    val fastingProtocol: FastingProtocol = FastingProtocol.OFF,
    val fastingStartMinute: Int = 12 * 60,
    /** One-shot message for a snackbar (e.g. "Copied 5 items from yesterday"). */
    val message: String? = null
)

private val mealTypes = listOf("Breakfast", "Mid Morning", "Lunch", "Snack", "Pre Workout", "Post Workout", "Dinner")

class NutritionViewModel(private val container: AppContainer, private val settings: SettingsDataStore) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState
    val mealTypeOptions = mealTypes

    private val searchQuery = MutableStateFlow("")
    private var allFoods: List<Food> = emptyList()

    init {
        viewModelScope.launch {
            allFoods = container.nutritionRepository.searchFoods(null).first()
            // Targets and the plan follow the diet setup, so refresh whenever it changes.
            settings.savedDietPrefs.collectLatest { loadTargetsAndPlan() }
        }
        viewModelScope.launch {
            searchQuery
                .debounce(250)
                .flatMapLatest { q -> container.nutritionRepository.searchFoods(q) }
                .collect { results -> _uiState.value = _uiState.value.copy(foodResults = results) }
        }
        viewModelScope.launch {
            container.nutritionRepository.getMealsForToday().collect { meals ->
                _uiState.value = _uiState.value.copy(
                    meals = meals,
                    consumedCalories = meals.sumOf { it.calories.toDouble() }.toInt(),
                    consumedProtein = meals.sumOf { it.protein.toDouble() }.toInt(),
                    consumedCarbs = meals.sumOf { it.carbs.toDouble() }.toInt(),
                    consumedFat = meals.sumOf { it.fat.toDouble() }.toInt()
                )
                refreshCoaching()
            }
        }
        viewModelScope.launch {
            container.progressRepository.getWaterForToday().collect { logs ->
                _uiState.value = _uiState.value.copy(waterMl = logs.sumOf { it.amountMl })
                refreshCoaching()
            }
        }
        viewModelScope.launch {
            container.nutritionRepository.recentFoods(10).collect { _uiState.value = _uiState.value.copy(recentFoods = it) }
        }
        viewModelScope.launch {
            combine(settings.fastingProtocol, settings.fastingStartMinute) { p, m -> p to m }
                .collect { (p, m) -> _uiState.value = _uiState.value.copy(fastingProtocol = p, fastingStartMinute = m) }
        }
    }

    private suspend fun loadTargetsAndPlan() {
        val resolved = container.nutritionPlanner.resolve()
        if (resolved == null) {
            _uiState.value = _uiState.value.copy(loading = false)
            return
        }
        val plan = container.nutritionPlanner.buildPlan(resolved)
        val today = LocalDate.now().dayOfWeek.value
        _uiState.value = _uiState.value.copy(
            loading = false, targets = resolved.targets, prefs = resolved.prefs,
            todayPlan = plan.days.firstOrNull { it.dayOfWeek == today }
        )
        refreshCoaching()
    }

    /** Recomputes the coach tips and "what to eat next" from today's totals. */
    private fun refreshCoaching() {
        val s = _uiState.value
        val targets = s.targets ?: return
        val prefs = s.prefs ?: return
        val totals = DayTotals(LocalDate.now().toEpochDay(), s.consumedCalories, s.consumedProtein, s.consumedCarbs, s.consumedFat)
        val hour = LocalTime.now().hour
        _uiState.value = s.copy(
            tips = NutritionInsights.tips(totals, s.waterMl, targets, prefs, hour, s.meals.size),
            suggestions = if (allFoods.isEmpty()) emptyList() else NutritionInsights.suggestions(
                remainingKcal = targets.calorieTarget - s.consumedCalories,
                remainingProtein = targets.proteinGrams - s.consumedProtein,
                foods = allFoods, style = prefs.style, goal = prefs.goal
            )
        )
    }

    fun setFoodSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(foodSearchQuery = query)
        searchQuery.value = query
    }

    fun logFood(food: Food, mealType: String, servingGrams: Float) {
        viewModelScope.launch {
            val factor = servingGrams / 100f
            container.nutritionRepository.logMeal(
                MealLogEntry(
                    dateEpochDay = LocalDate.now().toEpochDay(),
                    mealType = mealType,
                    foodId = food.id,
                    foodName = food.name,
                    servingGrams = servingGrams,
                    calories = food.caloriesPer100g * factor,
                    protein = food.proteinPer100g * factor,
                    carbs = food.carbsPer100g * factor,
                    fat = food.fatPer100g * factor
                )
            )
        }
    }

    /** For foods that are not in the built-in database: the user types the numbers for what they ate. */
    fun logCustomFood(mealType: String, name: String, calories: Float, protein: Float, carbs: Float, fat: Float) {
        viewModelScope.launch {
            container.nutritionRepository.logMeal(
                MealLogEntry(
                    dateEpochDay = LocalDate.now().toEpochDay(),
                    mealType = mealType,
                    foodId = 0L,
                    foodName = name.trim().ifBlank { "Custom food" }.take(60),
                    servingGrams = 0f,
                    calories = calories, protein = protein, carbs = carbs, fat = fat
                )
            )
        }
    }

    fun deleteMeal(entry: MealLogEntry) {
        viewModelScope.launch { container.nutritionRepository.deleteMeal(entry) }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch { container.progressRepository.addWater(amountMl) }
    }

    fun undoWater() {
        viewModelScope.launch { container.progressRepository.undoLastWater() }
    }

    fun logPlanMeal(meal: PlanMeal) {
        viewModelScope.launch {
            container.nutritionRepository.logPlanMeal(meal.slot, meal.items)
            _uiState.value = _uiState.value.copy(message = "Logged ${meal.slot.lowercase()} from your plan")
        }
    }

    fun copyYesterday() {
        viewModelScope.launch {
            val n = container.nutritionRepository.copyYesterdayToToday()
            _uiState.value = _uiState.value.copy(
                message = if (n == 0) "Nothing was logged yesterday" else "Copied $n item${if (n == 1) "" else "s"} from yesterday"
            )
        }
    }

    fun messageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun setFastingProtocol(p: FastingProtocol) {
        viewModelScope.launch { settings.setFastingProtocol(p) }
    }

    fun shiftFastingStart(deltaMinutes: Int) {
        viewModelScope.launch { settings.setFastingStartMinute(_uiState.value.fastingStartMinute + deltaMinutes) }
    }
}

data class FoodListUiState(
    val loading: Boolean = true,
    val foods: List<Food> = emptyList()
)

/** Backs the full Food Database screen - every seeded food, browsable and filterable. */
class FoodListViewModel(container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(FoodListUiState())
    val uiState: StateFlow<FoodListUiState> = _uiState

    init {
        viewModelScope.launch {
            container.nutritionRepository.searchFoods(null).collect { foods ->
                _uiState.value = FoodListUiState(loading = false, foods = foods)
            }
        }
    }
}
