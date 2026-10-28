package com.freegymtrainer.app.domain.nutrition

import com.freegymtrainer.app.data.local.entity.Food

/** What the person wants their body to do. Drives the calorie adjustment and protein target. */
enum class DietGoal(val label: String, val blurb: String) {
    LOSE_FAT("Lose fat", "A steady calorie deficit with high protein to keep muscle."),
    MAINTAIN("Maintain", "Eat at maintenance to hold your weight and perform well."),
    BUILD_MUSCLE("Build muscle", "A small calorie surplus with plenty of protein for growth."),
    RECOMP("Recomposition", "Maintenance calories, very high protein: lose fat and build muscle slowly.");

    companion object {
        /** The goal that matches the free-text goal chosen during onboarding. */
        fun fromProfileGoal(goal: String): DietGoal = when (goal) {
            "Lose fat", "Lose weight" -> LOSE_FAT
            "Build muscle", "Strength training" -> BUILD_MUSCLE
            else -> MAINTAIN
        }
    }
}

/** What the person eats. Each level includes everything allowed at the levels below it. */
enum class DietStyle(val label: String, val short: String, val level: Int) {
    VEGAN("Vegan", "Vegan", 0),
    VEGETARIAN("Vegetarian", "Veg", 1),
    EGGETARIAN("Veg + eggs", "Veg+Egg", 2),
    NON_VEG("Non-veg", "Non-veg", 3);

    companion object {
        /** Maps the older two-way "Veg"/"Non-Veg" setting. */
        fun fromLegacy(value: String): DietStyle = if (value == "Non-Veg") NON_VEG else VEGETARIAN
    }
}

enum class MacroStyle(val label: String, val blurb: String) {
    BALANCED("Balanced", "A mix of carbs, protein and fat."),
    HIGH_PROTEIN("High protein", "Extra protein - more filling, protects muscle."),
    LOW_CARB("Lower carb", "Fewer carbs, more fat - for people who feel better on less bread and rice.")
}

/** Everything the person chose on the diet-plan setup. */
data class DietPrefs(
    val goal: DietGoal,
    /** Planned change in body weight per week, in kg (always positive; the goal decides the direction). */
    val paceKgPerWeek: Float,
    val style: DietStyle,
    val macroStyle: MacroStyle = MacroStyle.BALANCED,
    val mealsPerDay: Int = 4,
    /** Goal weight in kg, or null when not set. */
    val targetWeightKg: Float? = null
) {
    companion object {
        val PACES_LOSE = listOf(0.25f, 0.5f, 0.75f)
        val PACES_GAIN = listOf(0.25f, 0.5f)

        fun defaultFor(profileGoal: String, legacyDiet: String): DietPrefs {
            val goal = DietGoal.fromProfileGoal(profileGoal)
            return DietPrefs(
                goal = goal,
                paceKgPerWeek = when (goal) { DietGoal.LOSE_FAT -> 0.5f; DietGoal.BUILD_MUSCLE -> 0.25f; else -> 0f },
                style = DietStyle.fromLegacy(legacyDiet)
            )
        }
    }
}

// ---- food classification helpers --------------------------------------------------------------

private val EGG_WORDS = listOf("egg", "omelette")
private val DAIRY_WORDS = listOf(
    "milk", "curd", "yogurt", "paneer", "ghee", "butter", "cheese", "whey", "lassi", "buttermilk", "chaas",
    "ice cream", "kheer", "rasgulla", "gulab", "cream", "dahi", "cottage"
)

fun Food.isEgg(): Boolean = EGG_WORDS.any { name.contains(it, ignoreCase = true) }

/** True for foods with no meat, fish, egg, dairy or honey. Soy milk is vegan even though its name contains "milk". */
fun Food.isVegan(): Boolean {
    if (category == "Non-vegetarian" || category == "Dairy") return false
    if (name.contains("soy milk", ignoreCase = true)) return true
    if (name.equals("Honey", ignoreCase = true)) return false
    return DAIRY_WORDS.none { name.contains(it, ignoreCase = true) } && !isEgg()
}

/** Whether a person following [style] eats this food. */
fun Food.allowedFor(style: DietStyle): Boolean = when (style) {
    DietStyle.VEGAN -> isVegan()
    DietStyle.VEGETARIAN -> category != "Non-vegetarian"
    DietStyle.EGGETARIAN -> category != "Non-vegetarian" || isEgg()
    DietStyle.NON_VEG -> true
}
