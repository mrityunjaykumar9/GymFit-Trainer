package com.freegymtrainer.app.domain.usecase

import kotlin.math.roundToInt

data class NutritionTargets(
    val bmr: Int,
    val tdee: Int,
    val calorieTarget: Int,
    val proteinGrams: Int,
    val carbGrams: Int,
    val fatGrams: Int,
    val waterMl: Int
)

/**
 * Uses the widely-accepted Mifflin-St Jeor equation for BMR and standard activity multipliers
 * for TDEE. These are estimates - individual needs vary. Never presented as medical fact.
 */
object NutritionCalculator {

    private val activityMultipliers = mapOf(
        "Sedentary" to 1.2,
        "Lightly active" to 1.375,
        "Moderately active" to 1.55,
        "Very active" to 1.725,
        "Extremely active" to 1.9
    )

    fun calculateBmr(weightKg: Float, heightCm: Float, ageYears: Int, gender: String): Int {
        val base = 10 * weightKg + 6.25f * heightCm - 5 * ageYears
        val bmr = if (gender.equals("Female", ignoreCase = true)) base - 161 else base + 5
        return bmr.roundToInt().coerceAtLeast(800)
    }

    fun calculateTdee(bmr: Int, activityLevel: String): Int {
        val multiplier = activityMultipliers[activityLevel] ?: 1.55
        return (bmr * multiplier).roundToInt()
    }

    /** Applies a moderate, sustainable surplus/deficit based on goal - never extreme values. */
    fun calculateTargets(weightKg: Float, heightCm: Float, ageYears: Int, gender: String, activityLevel: String, goal: String): NutritionTargets {
        val bmr = calculateBmr(weightKg, heightCm, ageYears, gender)
        val tdee = calculateTdee(bmr, activityLevel)

        val calorieTarget = when (goal) {
            "Lose fat", "Lose weight" -> tdee - 500
            "Build muscle" -> tdee + 300
            "Maintain weight" -> tdee
            "Strength training" -> tdee + 150
            else -> tdee
        }.coerceAtLeast(1200)

        // Flat 1.5 g protein per kg of body weight, regardless of goal.
        val proteinPerKg = 1.5
        val proteinGrams = (weightKg * proteinPerKg).roundToInt()
        val proteinCalories = proteinGrams * 4

        val fatGrams = (calorieTarget * 0.25 / 9).roundToInt()
        val fatCalories = fatGrams * 9

        val remainingCalories = (calorieTarget - proteinCalories - fatCalories).coerceAtLeast(0)
        val carbGrams = (remainingCalories / 4.0).roundToInt()

        val waterMl = (weightKg * 35).roundToInt().coerceIn(1500, 5000)

        return NutritionTargets(bmr, tdee, calorieTarget, proteinGrams, carbGrams, fatGrams, waterMl)
    }
}
