package com.freegymtrainer.app

import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.seed.FoodSeedData
import com.freegymtrainer.app.domain.nutrition.DietGoal
import com.freegymtrainer.app.domain.nutrition.DietPrefs
import com.freegymtrainer.app.domain.nutrition.DietStyle
import com.freegymtrainer.app.domain.nutrition.MacroStyle
import com.freegymtrainer.app.domain.nutrition.PlanGenerator
import com.freegymtrainer.app.domain.nutrition.allowedFor
import com.freegymtrainer.app.domain.nutrition.isEgg
import com.freegymtrainer.app.domain.nutrition.isVegan
import com.freegymtrainer.app.domain.usecase.NutritionCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NutritionPlanTest {

    private val foods: Map<String, Food> = FoodSeedData.all.associateBy { it.name }

    private fun targets(prefs: DietPrefs, weight: Float = 70f, gender: String = "Male", age: Int = 28) =
        NutritionCalculator.planTargets(weight, 172f, age, gender, "Moderately active", prefs)

    private fun prefs(goal: DietGoal, style: DietStyle, pace: Float = 0.5f, meals: Int = 4, macro: MacroStyle = MacroStyle.BALANCED) =
        DietPrefs(goal, pace, style, macro, meals)

    // ---- seed data ----------------------------------------------------------------------------

    @Test fun foodNamesAreUniqueAndEveryGeneratorFoodExists() {
        val names = FoodSeedData.all.map { it.name }
        assertEquals("duplicate food names", names.size, names.toSet().size)
        val missing = PlanGenerator.requiredFoodNames.filter { it !in foods }
        assertTrue("plan foods missing from the database: $missing", missing.isEmpty())
    }

    @Test fun foodMacrosArePlausible() {
        for (f in FoodSeedData.all) {
            val kcalFromMacros = f.proteinPer100g * 4 + f.carbsPer100g * 4 + f.fatPer100g * 9
            assertTrue(
                "${f.name}: ${f.caloriesPer100g} kcal vs ~$kcalFromMacros from macros",
                abs(f.caloriesPer100g - kcalFromMacros) <= maxOf(25f, 0.35f * f.caloriesPer100g)
            )
            assertTrue("${f.name} serving", f.defaultServingGrams > 0f)
        }
    }

    // ---- calculator safety --------------------------------------------------------------------

    @Test fun weightLossNeverGoesBelowTheCalorieFloorOrBeyond25Percent() {
        val t = targets(prefs(DietGoal.LOSE_FAT, DietStyle.VEGETARIAN, 0.75f), weight = 50f, gender = "Female")
        assertTrue("floor", t.calorieTarget >= 1200)
        assertTrue("25% cap", t.calorieTarget >= t.tdee * 0.75 - 1)
        assertTrue(t.weeklyChangeKg <= 0f)
    }

    @Test fun underEighteenKeepsMaintenanceCalories() {
        val t = targets(prefs(DietGoal.LOSE_FAT, DietStyle.NON_VEG, 0.5f), age = 16)
        assertEquals(t.tdee, t.calorieTarget)
        assertTrue(t.safetyNote.isNotBlank())
    }

    @Test fun goalsMoveCaloriesInTheRightDirection() {
        val lose = targets(prefs(DietGoal.LOSE_FAT, DietStyle.NON_VEG, 0.5f))
        val keep = targets(prefs(DietGoal.MAINTAIN, DietStyle.NON_VEG, 0f))
        val gain = targets(prefs(DietGoal.BUILD_MUSCLE, DietStyle.NON_VEG, 0.25f))
        assertTrue(lose.calorieTarget < keep.calorieTarget && keep.calorieTarget < gain.calorieTarget)
        assertTrue(lose.proteinGrams >= gain.proteinGrams)
        assertTrue(lose.weeklyChangeKg < 0f && gain.weeklyChangeKg > 0f)
    }

    @Test fun macrosAddUpToTheCalorieTarget() {
        for (macro in MacroStyle.values()) {
            val t = targets(prefs(DietGoal.LOSE_FAT, DietStyle.NON_VEG, macro = macro))
            val kcal = t.proteinGrams * 4 + t.carbGrams * 4 + t.fatGrams * 9
            assertTrue("$macro: $kcal vs ${t.calorieTarget}", abs(kcal - t.calorieTarget) <= t.calorieTarget * 0.06)
        }
        val low = targets(prefs(DietGoal.MAINTAIN, DietStyle.NON_VEG, 0f, macro = MacroStyle.LOW_CARB))
        val bal = targets(prefs(DietGoal.MAINTAIN, DietStyle.NON_VEG, 0f))
        assertTrue(low.carbGrams < bal.carbGrams)
    }

    // ---- plan generator -----------------------------------------------------------------------

    @Test fun plansHitCalorieAndProteinTargetsForEveryCombination() {
        for (style in DietStyle.values()) for (goal in listOf(DietGoal.LOSE_FAT, DietGoal.MAINTAIN, DietGoal.BUILD_MUSCLE)) for (meals in 3..5) {
            val pr = prefs(goal, style, if (goal == DietGoal.MAINTAIN) 0f else 0.5f, meals)
            val t = targets(pr)
            val plan = PlanGenerator.generate(foods, t, pr)
            assertEquals(7, plan.days.size)
            for (day in plan.days) {
                assertEquals("$style/$goal/$meals meals", meals, day.meals.size)
                assertTrue(
                    "$style/$goal/$meals d${day.dayOfWeek} kcal ${day.calories} vs ${t.calorieTarget}",
                    abs(day.calories - t.calorieTarget) <= t.calorieTarget * 0.08
                )
                assertTrue(
                    "$style/$goal/$meals d${day.dayOfWeek} protein ${day.protein} vs ${t.proteinGrams}",
                    day.protein >= t.proteinGrams * 0.78f
                )
            }
        }
    }

    @Test fun dietStylesAreRespected() {
        for (style in DietStyle.values()) {
            val pr = prefs(DietGoal.MAINTAIN, style, 0f)
            val plan = PlanGenerator.generate(foods, targets(pr), pr)
            for (item in plan.days.flatMap { d -> d.meals.flatMap { it.items } }) {
                assertTrue("${item.food.name} not allowed for $style", item.food.allowedFor(style))
                if (style == DietStyle.VEGAN) assertTrue(item.food.isVegan())
                if (style == DietStyle.VEGETARIAN) assertFalse(item.food.isEgg())
            }
        }
    }

    @Test fun nonVegPlansIncludeMeatOrFish() {
        val nv = prefs(DietGoal.MAINTAIN, DietStyle.NON_VEG, 0f)
        val nvPlan = PlanGenerator.generate(foods, targets(nv), nv)
        assertTrue(nvPlan.days.flatMap { d -> d.meals.flatMap { it.items } }.any { it.food.category == "Non-vegetarian" && !it.food.isEgg() })
    }

    @Test fun weeksHaveVariety() {
        for (style in DietStyle.values()) {
            val pr = prefs(DietGoal.MAINTAIN, style, 0f)
            val plan = PlanGenerator.generate(foods, targets(pr), pr)
            for (slot in listOf("Breakfast", "Lunch", "Dinner")) {
                val titles = plan.days.map { d -> d.meals.first { it.slot == slot }.title }.toSet()
                assertTrue("$style $slot only has ${titles.size} different meals", titles.size >= 4)
            }
        }
    }

    @Test fun fatLossPlansLeaveOutHeartyMeals() {
        val pr = prefs(DietGoal.LOSE_FAT, DietStyle.NON_VEG, 0.5f)
        val plan = PlanGenerator.generate(foods, targets(pr), pr)
        val titles = plan.days.flatMap { d -> d.meals.map { it.title } }
        assertFalse(titles.any { it.contains("biryani", true) || it.contains("Aloo paratha", true) })
    }
}
