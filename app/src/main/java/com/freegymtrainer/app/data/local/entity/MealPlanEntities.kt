package com.freegymtrainer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * LEGACY: one day of the fixed veg / non-veg weekly plan used by older app versions. Diet plans are
 * now generated on the device from the person's goal (see domain/nutrition/PlanGenerator), so nothing
 * reads or writes these tables any more; they stay in the schema so existing databases keep opening
 * without a migration.
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
