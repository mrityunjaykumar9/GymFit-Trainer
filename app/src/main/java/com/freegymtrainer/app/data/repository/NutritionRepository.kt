package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.MealLogEntry
import com.freegymtrainer.app.domain.nutrition.PlanItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Food database search and the daily meal log. Diet plans themselves come from [NutritionPlanner]. */
class NutritionRepository(private val db: AppDatabase) {
    fun searchFoods(query: String?): Flow<List<Food>> = db.foodDao().search(query?.takeIf { it.isNotBlank() })
    suspend fun getFood(id: Long): Food? = db.foodDao().getById(id)

    fun getMealsForToday(): Flow<List<MealLogEntry>> = db.mealLogDao().getForDay(LocalDate.now().toEpochDay())
    suspend fun logMeal(entry: MealLogEntry) = db.mealLogDao().insert(entry)
    suspend fun deleteMeal(entry: MealLogEntry) = db.mealLogDao().delete(entry)

    /** Everything logged in the last [days] days (today included), for the weekly insights. */
    fun getRecentMeals(days: Int = 7): Flow<List<MealLogEntry>> {
        val today = LocalDate.now().toEpochDay()
        return db.mealLogDao().getRange(today - (days - 1), today)
    }

    /** The foods eaten most recently, newest first. */
    fun recentFoods(limit: Int = 10): Flow<List<Food>> =
        combine(db.mealLogDao().recentFoodIds(limit), db.mealLogDao().recentFoodsUnordered(limit)) { ids, foods ->
            val byId = foods.associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }

    /** Copies everything logged yesterday into today. Returns how many entries were copied. */
    suspend fun copyYesterdayToToday(): Int {
        val today = LocalDate.now().toEpochDay()
        val yesterday = db.mealLogDao().getForDayOnce(today - 1)
        if (yesterday.isEmpty()) return 0
        db.mealLogDao().insertAll(yesterday.map { it.copy(id = 0, dateEpochDay = today) })
        return yesterday.size
    }

    suspend fun getFoodsByNames(names: Collection<String>): Map<String, Food> =
        db.foodDao().getByNames(names.toList()).associateBy { it.name }

    /** Logs a generated-plan meal into today. */
    suspend fun logPlanMeal(slot: String, items: List<PlanItem>) {
        val today = LocalDate.now().toEpochDay()
        db.mealLogDao().insertAll(items.map { (food, grams) ->
            val factor = grams / 100f
            MealLogEntry(
                dateEpochDay = today, mealType = slot, foodId = food.id, foodName = food.name, servingGrams = grams,
                calories = food.caloriesPer100g * factor, protein = food.proteinPer100g * factor,
                carbs = food.carbsPer100g * factor, fat = food.fatPer100g * factor
            )
        })
    }
}
