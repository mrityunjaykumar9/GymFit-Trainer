package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.MealLogEntry
import com.freegymtrainer.app.data.local.entity.MealPlanDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class MealPlanItemDetail(val food: Food, val grams: Float)
data class MealPlanMealDetail(val mealType: String, val items: List<MealPlanItemDetail>)
data class MealPlanDayDetail(val day: MealPlanDay, val meals: List<MealPlanMealDetail>)

class NutritionRepository(private val db: AppDatabase) {
    fun searchFoods(query: String?): Flow<List<Food>> = db.foodDao().search(query?.takeIf { it.isNotBlank() })
    suspend fun getFood(id: Long): Food? = db.foodDao().getById(id)

    fun getMealsForToday(): Flow<List<MealLogEntry>> = db.mealLogDao().getForDay(LocalDate.now().toEpochDay())
    suspend fun logMeal(entry: MealLogEntry) = db.mealLogDao().insert(entry)
    suspend fun deleteMeal(entry: MealLogEntry) = db.mealLogDao().delete(entry)

    suspend fun getMealPlanForDay(dietType: String, dayOfWeek: Int): MealPlanDayDetail? {
        val day = db.mealPlanDao().getDay(dietType, dayOfWeek) ?: return null
        val items = db.mealPlanDao().getItemsForDay(day.id)
        val foodsById = items.map { it.foodId }.distinct().associateWith { db.foodDao().getById(it) }

        val meals = items
            .groupBy { it.mealType }
            .map { (mealType, mealItems) ->
                MealPlanMealDetail(
                    mealType = mealType,
                    items = mealItems.sortedBy { it.orderIndex }.mapNotNull { item ->
                        foodsById[item.foodId]?.let { food -> MealPlanItemDetail(food, item.grams) }
                    }
                )
            }
        return MealPlanDayDetail(day, meals)
    }

    /** Copies a planned meal's items into today's actual log in one action. */
    suspend fun logPlannedMeal(mealType: String, items: List<MealPlanItemDetail>) {
        val entries = items.map { (food, grams) ->
            val factor = grams / 100f
            MealLogEntry(
                dateEpochDay = LocalDate.now().toEpochDay(),
                mealType = mealType,
                foodId = food.id,
                foodName = food.name,
                servingGrams = grams,
                calories = food.caloriesPer100g * factor,
                protein = food.proteinPer100g * factor,
                carbs = food.carbsPer100g * factor,
                fat = food.fatPer100g * factor
            )
        }
        db.mealLogDao().insertAll(entries)
    }
}
