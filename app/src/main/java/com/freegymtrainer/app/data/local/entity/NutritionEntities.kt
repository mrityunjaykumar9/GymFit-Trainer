package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,          // Indian / Vegetarian / Non-vegetarian / Vegan / Fruits / ...
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float,
    val defaultServingGrams: Float
)

/** Every category except "Non-vegetarian" is vegetarian-safe in this food database. */
fun Food.isVeg(): Boolean = category != "Non-vegetarian"

@Entity(tableName = "meal_log_entries")
data class MealLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val mealType: String,          // Breakfast / Mid Morning / Lunch / Pre Workout / Post Workout / Dinner
    val foodId: Long,
    val foodName: String,
    val servingGrams: Float,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)
