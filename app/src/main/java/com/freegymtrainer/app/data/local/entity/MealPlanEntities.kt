package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One day of a reference meal plan (a suggestion, not a log) - seeded once from
 * MealPlanSeedData and never edited by the user, the same way workout templates work.
 */
@Entity(tableName = "meal_plan_days")
data class MealPlanDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dietType: String, // "Veg" | "Non-Veg"
    val dayOfWeek: Int,   // 1=Mon..7=Sun
    val title: String
)

@Entity(tableName = "meal_plan_items")
data class MealPlanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planDayId: Long,
    val mealType: String, // Breakfast / Lunch / Snack / Dinner
    val orderIndex: Int,
    val foodId: Long,
    val grams: Float
)
