package com.freegymtrainer.app.presentation.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.MealLogEntry
import com.freegymtrainer.app.data.repository.MealPlanDayDetail
import com.freegymtrainer.app.data.repository.MealPlanMealDetail
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.domain.usecase.NutritionCalculator
import com.freegymtrainer.app.domain.usecase.NutritionTargets
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class NutritionUiState(
    val loading: Boolean = true,
    val targets: NutritionTargets? = null,
    val consumedCalories: Int = 0,
    val consumedProtein: Int = 0,
    val consumedCarbs: Int = 0,
    val consumedFat: Int = 0,
    val waterMl: Int = 0,
    val meals: List<MealLogEntry> = emptyList(),
    val foodSearchQuery: String = "",
    val foodResults: List<Food> = emptyList(),
    val dietPreference: String = "Veg",
    val mealPlan: MealPlanDayDetail? = null
)

private val mealTypes = listOf("Breakfast", "Mid Morning", "Lunch", "Pre Workout", "Post Workout", "Dinner")

class NutritionViewModel(private val container: AppContainer, private val settings: SettingsDataStore) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState
    val mealTypeOptions = mealTypes

    private val searchQuery = MutableStateFlow("")

    init {
        loadTargetsAndLogs()
        viewModelScope.launch {
            searchQuery
                .debounce(300)
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
            }
        }
        viewModelScope.launch {
            container.progressRepository.getWaterForToday().collect { logs ->
                _uiState.value = _uiState.value.copy(waterMl = logs.sumOf { it.amountMl })
            }
        }
        viewModelScope.launch {
            val todayDow = LocalDate.now().dayOfWeek.value
            settings.dietPreference
                .flatMapLatest { pref ->
                    flow { emit(pref to container.nutritionRepository.getMealPlanForDay(pref, todayDow)) }
                }
                .collect { (pref, plan) ->
                    _uiState.value = _uiState.value.copy(dietPreference = pref, mealPlan = plan)
                }
        }
    }

    private fun loadTargetsAndLogs() {
        viewModelScope.launch {
            val profile = container.userRepository.getProfile()
            if (profile != null) {
                val targets = NutritionCalculator.calculateTargets(
                    weightKg = profile.weightKg, heightCm = profile.heightCm,
                    ageYears = 28, gender = profile.gender,
                    activityLevel = profile.activityLevel, goal = profile.goal
                )
                _uiState.value = _uiState.value.copy(loading = false, targets = targets)
            } else {
                _uiState.value = _uiState.value.copy(loading = false)
            }
        }
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
                    dateEpochDay = java.time.LocalDate.now().toEpochDay(),
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

    fun deleteMeal(entry: MealLogEntry) {
        viewModelScope.launch { container.nutritionRepository.deleteMeal(entry) }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch { container.progressRepository.addWater(amountMl) }
    }

    fun setDietPreference(preference: String) {
        viewModelScope.launch { settings.setDietPreference(preference) }
    }

    fun logPlannedMeal(meal: MealPlanMealDetail) {
        viewModelScope.launch { container.nutritionRepository.logPlannedMeal(meal.mealType, meal.items) }
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
