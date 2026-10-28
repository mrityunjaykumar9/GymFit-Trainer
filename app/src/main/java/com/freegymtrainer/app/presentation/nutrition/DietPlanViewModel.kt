package com.freegymtrainer.app.presentation.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freegymtrainer.app.data.local.entity.WeightEntry
import com.freegymtrainer.app.data.repository.ResolvedNutrition
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.domain.nutrition.DietGoal
import com.freegymtrainer.app.domain.nutrition.DietPlan
import com.freegymtrainer.app.domain.nutrition.DietPrefs
import com.freegymtrainer.app.domain.nutrition.NutritionInsights
import com.freegymtrainer.app.domain.nutrition.PlanDay
import com.freegymtrainer.app.domain.nutrition.PlanMeal
import com.freegymtrainer.app.domain.nutrition.ShoppingGroup
import com.freegymtrainer.app.domain.nutrition.ShoppingList
import com.freegymtrainer.app.domain.nutrition.WeekSummary
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DietPlanUiState(
    val loading: Boolean = true,
    val resolved: ResolvedNutrition? = null,
    val plan: DietPlan? = null,
    val selectedDay: Int = LocalDate.now().dayOfWeek.value,
    val shopping: List<ShoppingGroup> = emptyList(),
    val shoppingChecked: Set<String> = emptySet(),
    /** Latest logged weight, falling back to the weight from the profile. */
    val currentWeightKg: Float = 0f,
    val weeksToGoal: Int? = null,
    val message: String? = null
) {
    val selected: PlanDay? get() = plan?.days?.firstOrNull { it.dayOfWeek == selectedDay }
}

/** Backs the Plan tab: diet setup, the generated 7-day plan, logging a planned meal, and the shopping list. */
class DietPlanViewModel(private val container: AppContainer, private val settings: SettingsDataStore) : ViewModel() {

    private val _uiState = MutableStateFlow(DietPlanUiState())
    val uiState: StateFlow<DietPlanUiState> = _uiState

    init {
        viewModelScope.launch {
            settings.savedDietPrefs.collectLatest { reload() }
        }
        viewModelScope.launch {
            settings.shoppingChecked.collect { _uiState.value = _uiState.value.copy(shoppingChecked = it) }
        }
    }

    private suspend fun reload() {
        val resolved = container.nutritionPlanner.resolve()
        if (resolved == null) {
            _uiState.value = _uiState.value.copy(loading = false, resolved = null, plan = null)
            return
        }
        val plan = container.nutritionPlanner.buildPlan(resolved)
        val t = resolved.targets
        _uiState.value = _uiState.value.copy(
            loading = false, resolved = resolved, plan = plan,
            shopping = ShoppingList.build(plan),
            currentWeightKg = resolved.profile.weightKg,
            weeksToGoal = NutritionInsights.weeksToGoal(resolved.profile.weightKg, resolved.prefs.targetWeightKg, t.weeklyChangeKg)
        )
    }

    fun selectDay(dow: Int) { _uiState.value = _uiState.value.copy(selectedDay = dow) }

    /** Applies [change] to the current diet choices and saves them; the plan and targets rebuild automatically. */
    fun updatePrefs(change: (DietPrefs) -> DietPrefs) {
        val current = _uiState.value.resolved?.prefs ?: return
        var next = change(current)
        // Choosing a different goal starts from that goal's own sensible pace (not the previous goal's);
        // otherwise the pace must be one of the offered steps, and it is zero for goals that keep weight.
        val goalChanged = next.goal != current.goal
        next = when (next.goal) {
            DietGoal.LOSE_FAT -> next.copy(paceKgPerWeek = if (!goalChanged && next.paceKgPerWeek in DietPrefs.PACES_LOSE) next.paceKgPerWeek else 0.5f)
            DietGoal.BUILD_MUSCLE -> next.copy(paceKgPerWeek = if (!goalChanged && next.paceKgPerWeek in DietPrefs.PACES_GAIN) next.paceKgPerWeek else 0.25f)
            else -> next.copy(paceKgPerWeek = 0f)
        }
        viewModelScope.launch { settings.saveDietPrefs(next) }
    }

    fun logMeal(meal: PlanMeal) {
        viewModelScope.launch {
            container.nutritionRepository.logPlanMeal(meal.slot, meal.items)
            _uiState.value = _uiState.value.copy(message = "Logged ${meal.slot.lowercase()} to today")
        }
    }

    fun logDay(day: PlanDay) {
        viewModelScope.launch {
            day.meals.forEach { container.nutritionRepository.logPlanMeal(it.slot, it.items) }
            _uiState.value = _uiState.value.copy(message = "Logged the whole day to today")
        }
    }

    fun toggleShopping(name: String) {
        val now = _uiState.value.shoppingChecked
        viewModelScope.launch { settings.setShoppingChecked(if (name in now) now - name else now + name) }
    }

    fun clearShopping() {
        viewModelScope.launch { settings.setShoppingChecked(emptySet()) }
    }

    fun messageShown() { _uiState.value = _uiState.value.copy(message = null) }
}

data class InsightsUiState(
    val loading: Boolean = true,
    val resolved: ResolvedNutrition? = null,
    val week: WeekSummary? = null,
    val weights: List<WeightEntry> = emptyList(),
    val weeksToGoal: Int? = null
)

/** Backs the Insights tab: the last seven days of eating and the weight trend. */
class InsightsViewModel(private val container: AppContainer, private val settings: SettingsDataStore) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState

    init {
        viewModelScope.launch {
            settings.savedDietPrefs.flatMapLatest {
                combine(
                    container.nutritionRepository.getRecentMeals(7),
                    container.progressRepository.getWeightHistory()
                ) { meals, weights -> meals to weights }
            }.collect { (meals, weights) ->
                // resolved fresh each time so a newly logged weight updates the targets too
                val resolved = container.nutritionPlanner.resolve()
                val targets = resolved?.targets
                val sorted = weights.sortedBy { it.dateEpochDay }
                val current = sorted.lastOrNull()?.weightKg ?: resolved?.profile?.weightKg
                _uiState.value = InsightsUiState(
                    loading = false,
                    resolved = resolved,
                    week = NutritionInsights.weekSummary(meals, LocalDate.now().toEpochDay(), targets),
                    weights = sorted.takeLast(12),
                    weeksToGoal = if (current != null && resolved != null)
                        NutritionInsights.weeksToGoal(current, resolved.prefs.targetWeightKg, resolved.targets.weeklyChangeKg) else null
                )
            }
        }
    }

    /** Adds today's weight; the trend, targets and goal estimate refresh automatically. */
    fun logWeight(weightKg: Float) {
        viewModelScope.launch { container.progressRepository.addWeightEntry(weightKg) }
    }
}
