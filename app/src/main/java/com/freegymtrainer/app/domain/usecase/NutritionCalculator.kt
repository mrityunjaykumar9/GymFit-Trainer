package com.freegymtrainer.app.domain.usecase

import com.freegymtrainer.app.domain.nutrition.DietGoal
import com.freegymtrainer.app.domain.nutrition.DietPrefs
import com.freegymtrainer.app.domain.nutrition.DietStyle
import com.freegymtrainer.app.domain.nutrition.MacroStyle
import kotlin.math.roundToInt

data class NutritionTargets(
    val bmr: Int,
    val tdee: Int,
    val calorieTarget: Int,
    val proteinGrams: Int,
    val carbGrams: Int,
    val fatGrams: Int,
    val waterMl: Int,
    val fiberGrams: Int = 25,
    /** Calories above (+) or below (-) maintenance that the target applies. */
    val calorieAdjustment: Int = 0,
    /** Expected body-weight change per week at this target, in kg (negative = loss). */
    val weeklyChangeKg: Float = 0f,
    /** A short explanation shown to the user when a safety limit changed their choice. */
    val safetyNote: String = ""
)

/**
 * Uses the widely-accepted Mifflin-St Jeor equation for BMR and standard activity multipliers
 * for TDEE. These are estimates - individual needs vary. Never presented as medical fact.
 */
object NutritionCalculator {

    /** Adult default used when no age is known (profiles created before age was stored). */
    const val DEFAULT_AGE = 28

    /**
     * Age in years from an ISO yyyy-MM-dd date of birth; [DEFAULT_AGE] when it is blank/invalid.
     * Onboarding only asks for an age, so it stores an approximate birth date (today minus that
     * many years) - see [dateOfBirthForAge].
     */
    fun ageFromDateOfBirth(dateOfBirth: String): Int =
        try {
            java.time.Period.between(java.time.LocalDate.parse(dateOfBirth), java.time.LocalDate.now()).years
                .takeIf { it in 10..100 } ?: DEFAULT_AGE
        } catch (e: java.time.format.DateTimeParseException) {
            DEFAULT_AGE
        }

    fun dateOfBirthForAge(ageYears: Int): String =
        java.time.LocalDate.now().minusYears(ageYears.coerceIn(10, 100).toLong()).toString()


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

    /**
     * Targets for a chosen [DietPrefs]: pace-based deficit/surplus with safety limits (calorie floor,
     * at most 25% below maintenance, no deficit under 18), goal-based protein and a macro style.
     * These are general estimates, not medical advice.
     */
    fun planTargets(
        weightKg: Float, heightCm: Float, ageYears: Int, gender: String, activityLevel: String, prefs: DietPrefs
    ): NutritionTargets {
        val bmr = calculateBmr(weightKg, heightCm, ageYears, gender)
        val tdee = calculateTdee(bmr, activityLevel)
        val female = gender.equals("Female", ignoreCase = true)
        val floor = if (female) 1200 else 1500
        var note = ""

        var adjust = when (prefs.goal) {
            DietGoal.LOSE_FAT -> -(prefs.paceKgPerWeek.coerceIn(0.1f, 0.75f) * 1100f).roundToInt()
            DietGoal.BUILD_MUSCLE -> (prefs.paceKgPerWeek.coerceIn(0.1f, 0.5f) * 1100f).roundToInt()
            else -> 0
        }
        if (adjust < 0) {
            val maxDeficit = (tdee * 0.25).roundToInt()
            if (-adjust > maxDeficit) { adjust = -maxDeficit; note = "Your deficit was limited to 25% of your maintenance calories to stay safe." }
            if (ageYears < 18) { adjust = 0; note = "Under 18 we keep calories at maintenance - growing bodies need the fuel." }
        }
        var calorieTarget = tdee + adjust
        if (adjust < 0 && calorieTarget < floor) {
            calorieTarget = floor
            adjust = floor - tdee
            note = "Calories were raised to a safe minimum of $floor kcal."
        }
        val realAdjust = calorieTarget - tdee

        val proteinPerKg = when (prefs.goal) {
            DietGoal.LOSE_FAT, DietGoal.RECOMP -> 2.0f
            DietGoal.BUILD_MUSCLE -> 1.8f
            DietGoal.MAINTAIN -> 1.5f
        } + (if (prefs.macroStyle == MacroStyle.HIGH_PROTEIN) 0.3f else 0f) - (if (prefs.style == DietStyle.VEGAN) 0.2f else 0f) // plant protein is less dense; 1.6-1.8 g/kg still works
        val proteinGrams = minOf(weightKg * proteinPerKg, calorieTarget * 0.35f / 4f).roundToInt()

        val fatShare = when (prefs.macroStyle) { MacroStyle.LOW_CARB -> 0.40f; MacroStyle.HIGH_PROTEIN -> 0.25f; else -> 0.27f }
        val fatGrams = (calorieTarget * fatShare / 9f).roundToInt()
        val carbGrams = ((calorieTarget - proteinGrams * 4 - fatGrams * 9) / 4f).roundToInt().coerceAtLeast(50)

        val fiber = (calorieTarget / 1000f * 14f).roundToInt().coerceIn(25, 40)
        val water = (weightKg * 35).roundToInt().coerceIn(1500, 5000)
        return NutritionTargets(
            bmr = bmr, tdee = tdee, calorieTarget = calorieTarget, proteinGrams = proteinGrams, carbGrams = carbGrams,
            fatGrams = fatGrams, waterMl = water, fiberGrams = fiber, calorieAdjustment = realAdjust,
            weeklyChangeKg = realAdjust * 7f / 7700f, safetyNote = note
        )
    }

    /** Kept for older callers: derives preferences from the free-text onboarding goal. */
    fun calculateTargets(weightKg: Float, heightCm: Float, ageYears: Int, gender: String, activityLevel: String, goal: String): NutritionTargets =
        planTargets(weightKg, heightCm, ageYears, gender, activityLevel, DietPrefs.defaultFor(goal, "Veg"))
}
