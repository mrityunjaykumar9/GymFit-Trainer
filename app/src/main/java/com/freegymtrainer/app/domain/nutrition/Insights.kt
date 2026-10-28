package com.freegymtrainer.app.domain.nutrition

import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.data.local.entity.MealLogEntry
import com.freegymtrainer.app.domain.usecase.NutritionTargets
import kotlin.math.abs
import kotlin.math.roundToInt

/** One day of logged food, summed. */
data class DayTotals(val epochDay: Long, val calories: Int, val protein: Int, val carbs: Int, val fat: Int) {
    val logged: Boolean get() = calories > 0
}

data class WeekSummary(
    /** Seven days, oldest first, ending today. */
    val days: List<DayTotals>,
    val loggedDays: Int,
    val avgCalories: Int,
    val avgProtein: Int,
    val daysOnTarget: Int,
    /** Consecutive days with food logged, counting back from today (or yesterday if today is still empty). */
    val streak: Int
)

enum class TipKind { GOOD, INFO, WARN }

data class Tip(val title: String, val body: String, val kind: TipKind)

data class FoodSuggestion(val food: Food, val grams: Float, val calories: Int, val protein: Int)

/** Rule-based coaching from what was eaten so far today - no network, no AI, just sensible nutrition rules. */
object NutritionInsights {

    fun weekSummary(entries: List<MealLogEntry>, today: Long, targets: NutritionTargets?): WeekSummary {
        val byDay = entries.groupBy { it.dateEpochDay }
        val days = (6 downTo 0).map { back ->
            val day = today - back
            val list = byDay[day].orEmpty()
            DayTotals(
                day,
                list.sumOf { it.calories.toDouble() }.roundToInt(),
                list.sumOf { it.protein.toDouble() }.roundToInt(),
                list.sumOf { it.carbs.toDouble() }.roundToInt(),
                list.sumOf { it.fat.toDouble() }.roundToInt()
            )
        }
        val logged = days.filter { it.logged }
        val onTarget = if (targets == null) 0 else logged.count { abs(it.calories - targets.calorieTarget) <= targets.calorieTarget * 0.10 }
        var streak = 0
        var cursor = days.size - 1
        if (!days[cursor].logged) cursor-- // today may simply not be logged yet
        while (cursor >= 0 && days[cursor].logged) { streak++; cursor-- }
        return WeekSummary(
            days = days,
            loggedDays = logged.size,
            avgCalories = if (logged.isEmpty()) 0 else logged.sumOf { it.calories } / logged.size,
            avgProtein = if (logged.isEmpty()) 0 else logged.sumOf { it.protein } / logged.size,
            daysOnTarget = onTarget,
            streak = streak
        )
    }

    /** Protein-rich foods to suggest, matched to how the person eats. */
    fun proteinIdeas(style: DietStyle): String = when (style) {
        DietStyle.VEGAN -> "tofu (150 g), soya chunks (40 g dry) or a bowl of dal"
        DietStyle.VEGETARIAN -> "paneer (100 g), Greek yogurt (200 g) or soya chunks (40 g dry)"
        DietStyle.EGGETARIAN -> "3 eggs or egg whites, paneer (100 g) or Greek yogurt (200 g)"
        DietStyle.NON_VEG -> "chicken breast (100 g), fish, eggs or tuna"
    }

    /**
     * Up to three short coaching tips for right now. [hour] is the hour of day (0-23) and
     * [mealsLogged] the number of food entries so far today.
     */
    fun tips(
        consumed: DayTotals, waterMl: Int, targets: NutritionTargets, prefs: DietPrefs, hour: Int, mealsLogged: Int
    ): List<Tip> {
        val tips = ArrayList<Tip>()
        val kcal = consumed.calories
        val target = targets.calorieTarget
        val proteinLeft = targets.proteinGrams - consumed.protein

        if (mealsLogged == 0) {
            if (hour >= 10) tips += Tip("Start logging", "Nothing logged yet today. Add your first meal - even a rough entry makes your numbers useful.", TipKind.INFO)
            return tips
        }
        if (kcal > target * 1.10) {
            val over = kcal - target
            tips += Tip(
                "A little over today",
                "You are about $over kcal above your target. One day does not change your progress - keep your next meal light and skip the guilt. Do not skip meals to \"make up\" for it.",
                TipKind.WARN
            )
        }
        if (proteinLeft > 25 && hour >= 14) {
            tips += Tip("Protein is behind", "About $proteinLeft g of protein to go. Easy options: ${proteinIdeas(prefs.style)}.", TipKind.INFO)
        } else if (consumed.protein >= targets.proteinGrams) {
            tips += Tip("Protein target hit", "You have reached your protein goal - great for ${if (prefs.goal == DietGoal.LOSE_FAT) "keeping muscle while you lose fat" else "recovery and growth"}.", TipKind.GOOD)
        }
        if (hour >= 19 && kcal < target * 0.6) {
            tips += Tip(
                "You may be under-eating",
                "Only ${kcal} of ${target} kcal so far. Eating far below target slows recovery and makes you hungrier tomorrow - have a proper, protein-rich dinner.",
                TipKind.WARN
            )
        }
        if (hour >= 15 && waterMl < targets.waterMl * 0.5) {
            tips += Tip("Drink more water", "You have had ${waterMl} of ${targets.waterMl} ml. A glass with every meal gets you there.", TipKind.INFO)
        }
        if (kcal in (target * 0.9).toInt()..(target * 1.1).toInt() && consumed.protein >= targets.proteinGrams * 0.9 && tips.none { it.kind == TipKind.WARN }) {
            tips += Tip("On track", "Calories and protein are both close to target. This is what consistency looks like.", TipKind.GOOD)
        }
        return tips.sortedBy { when (it.kind) { TipKind.WARN -> 0; TipKind.INFO -> 1; TipKind.GOOD -> 2 } }.take(3)
    }

    private val SUGGEST_CATEGORIES = setOf("Grains", "Legumes", "Dairy", "Non-vegetarian", "Vegetables", "Fruits", "Nuts", "Vegan", "Indian food")
    private val NEVER_SUGGEST = setOf("Ghee", "Butter", "Cheddar Cheese", "Olive Oil")

    /**
     * Foods that fit what is left of today. With protein to go, the most protein per calorie wins;
     * otherwise the lightest, most filling choices. At most one food per category so the list varies.
     */
    fun suggestions(remainingKcal: Int, remainingProtein: Int, foods: List<Food>, style: DietStyle, goal: DietGoal): List<FoodSuggestion> {
        if (remainingKcal < 60) return emptyList()
        val wantProtein = remainingProtein > 12
        return foods.asSequence()
            .filter { it.category in SUGGEST_CATEGORIES && it.name !in NEVER_SUGGEST && it.allowedFor(style) }
            .map { food ->
                val grams = food.defaultServingGrams
                FoodSuggestion(food, grams, (food.caloriesPer100g * grams / 100f).roundToInt(), (food.proteinPer100g * grams / 100f).roundToInt())
            }
            .filter { it.calories in 50..remainingKcal.coerceAtMost(450) }
            .sortedByDescending { s ->
                val perKcal = s.protein.toFloat() / s.calories.coerceAtLeast(1)
                if (wantProtein) perKcal else -s.calories.toFloat() + s.food.fiberPer100g * 4f
            }
            .distinctBy { it.food.category }
            .take(4)
            .toList()
    }

    /** Weeks until [targetKg] is reached at the planned pace, or null when there is no sensible estimate. */
    fun weeksToGoal(currentKg: Float, targetKg: Float?, weeklyChangeKg: Float): Int? {
        if (targetKg == null || weeklyChangeKg == 0f) return null
        val diff = targetKg - currentKg
        if (diff == 0f || (diff < 0f) != (weeklyChangeKg < 0f)) return null // goal and plan point opposite ways
        return (diff / weeklyChangeKg).roundToInt().coerceIn(1, 104)
    }
}
