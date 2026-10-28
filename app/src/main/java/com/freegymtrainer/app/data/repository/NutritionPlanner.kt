package com.freegymtrainer.app.data.repository

import com.freegymtrainer.app.data.local.AppDatabase
import com.freegymtrainer.app.data.local.entity.UserProfile
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.domain.nutrition.DietPlan
import com.freegymtrainer.app.domain.nutrition.DietPrefs
import com.freegymtrainer.app.domain.nutrition.PlanGenerator
import com.freegymtrainer.app.domain.nutrition.PlanMeal
import com.freegymtrainer.app.domain.usecase.NutritionCalculator
import com.freegymtrainer.app.domain.usecase.NutritionTargets
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** The profile, the diet choices made on top of it, and the targets they produce. */
data class ResolvedNutrition(val profile: UserProfile, val prefs: DietPrefs, val targets: NutritionTargets)

/**
 * The single place that turns "who is this person and what did they choose" into calorie/macro
 * targets and a 7-day meal plan, so the Nutrition tab, Home and the meal reminders always agree.
 */
class NutritionPlanner(private val db: AppDatabase, private val settings: SettingsDataStore) {

    suspend fun resolve(): ResolvedNutrition? {
        val stored = db.userProfileDao().get() ?: return null
        // Use the most recently logged weight so targets follow the person as they progress.
        val profile = db.weightDao().getLatest()?.weightKg?.let { stored.copy(weightKg = it) } ?: stored
        val saved = settings.savedDietPrefs.first()
        val prefs = saved ?: DietPrefs.defaultFor(profile.goal, settings.dietPreference.first())
        return ResolvedNutrition(profile, prefs, targetsFor(profile, prefs))
    }

    fun targetsFor(profile: UserProfile, prefs: DietPrefs): NutritionTargets =
        NutritionCalculator.planTargets(
            weightKg = profile.weightKg, heightCm = profile.heightCm,
            ageYears = NutritionCalculator.ageFromDateOfBirth(profile.dateOfBirth),
            gender = profile.gender, activityLevel = profile.activityLevel, prefs = prefs
        )

    suspend fun buildPlan(resolved: ResolvedNutrition): DietPlan {
        val foods = db.foodDao().getByNames(PlanGenerator.requiredFoodNames.toList()).associateBy { it.name }
        return PlanGenerator.generate(foods, resolved.targets, resolved.prefs)
    }

    /** Today's planned meal for a reminder (slot names: Breakfast, Lunch, Snack, Dinner), or null. */
    suspend fun plannedMealToday(slot: String): PlanMeal? {
        val resolved = resolve() ?: return null
        val plan = buildPlan(resolved)
        val dow = LocalDate.now().dayOfWeek.value
        return plan.days.firstOrNull { it.dayOfWeek == dow }?.meals?.firstOrNull { it.slot.equals(slot, ignoreCase = true) }
    }
}
