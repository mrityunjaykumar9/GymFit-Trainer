package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.MealLogEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class NutritionRepository(private val db: AppDatabase) {
    fun searchFoods(query: String?): Flow<List<Food>> = db.foodDao().search(query?.takeIf { it.isNotBlank() })
    suspend fun getFood(id: Long): Food? = db.foodDao().getById(id)

    fun getMealsForToday(): Flow<List<MealLogEntry>> = db.mealLogDao().getForDay(LocalDate.now().toEpochDay())
    suspend fun logMeal(entry: MealLogEntry) = db.mealLogDao().insert(entry)
    suspend fun deleteMeal(entry: MealLogEntry) = db.mealLogDao().delete(entry)
}
