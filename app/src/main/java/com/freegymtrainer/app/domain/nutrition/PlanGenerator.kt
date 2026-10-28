package com.freegymtrainer.app.domain.nutrition

import com.freegymtrainer.app.data.local.entity.Food
import com.freegymtrainer.app.domain.usecase.NutritionTargets
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PlanItem(val food: Food, val grams: Float) {
    val calories: Float get() = food.caloriesPer100g * grams / 100f
    val protein: Float get() = food.proteinPer100g * grams / 100f
    val carbs: Float get() = food.carbsPer100g * grams / 100f
    val fat: Float get() = food.fatPer100g * grams / 100f
    val fiber: Float get() = food.fiberPer100g * grams / 100f
}

data class PlanMeal(val slot: String, val title: String, val items: List<PlanItem>) {
    val calories: Float get() = items.sumOf { it.calories.toDouble() }.toFloat()
    val protein: Float get() = items.sumOf { it.protein.toDouble() }.toFloat()
    val carbs: Float get() = items.sumOf { it.carbs.toDouble() }.toFloat()
    val fat: Float get() = items.sumOf { it.fat.toDouble() }.toFloat()
}

data class PlanDay(val dayOfWeek: Int, val meals: List<PlanMeal>) {
    val calories: Float get() = meals.sumOf { it.calories.toDouble() }.toFloat()
    val protein: Float get() = meals.sumOf { it.protein.toDouble() }.toFloat()
    val carbs: Float get() = meals.sumOf { it.carbs.toDouble() }.toFloat()
    val fat: Float get() = meals.sumOf { it.fat.toDouble() }.toFloat()
    val fiber: Float get() = meals.sumOf { m -> m.items.sumOf { it.fiber.toDouble() } }.toFloat()
}

data class DietPlan(val days: List<PlanDay>, val targets: NutritionTargets, val prefs: DietPrefs)

/** The part a food plays in a meal - decides how its portion is scaled to hit the targets. */
private enum class Role { PROTEIN, ENERGY, VEG }

private class Part(val food: String, val grams: Float, val role: Role)

/** [hearty] meals are richer (biryani, stuffed parathas...) and are left out of fat-loss plans. */
private class Archetype(val title: String, val slot: String, val hearty: Boolean, val parts: List<Part>)

private fun p(food: String, grams: Float) = Part(food, grams, Role.PROTEIN)
private fun e(food: String, grams: Float) = Part(food, grams, Role.ENERGY)
private fun v(food: String, grams: Float) = Part(food, grams, Role.VEG)
private fun a(title: String, slot: String, vararg parts: Part, hearty: Boolean = false) = Archetype(title, slot, hearty, parts.toList())

object PlanGenerator {

    private const val BREAKFAST = "Breakfast"
    private const val MID_MORNING = "Mid Morning"
    private const val LUNCH = "Lunch"
    private const val SNACK = "Snack"
    private const val DINNER = "Dinner"

    private val ARCHETYPES: List<Archetype> = listOf(
        // ---- Breakfast
        a("Oats, milk & banana", BREAKFAST, e("Rolled Oats (Dry)", 50f), p("Milk (Low Fat)", 250f), e("Banana", 100f), e("Almonds", 15f)),
        a("Poha with peanuts & curd", BREAKFAST, e("Poha", 150f), e("Peanuts", 15f), p("Curd / Yogurt (Plain)", 150f)),
        a("Idli with sambar", BREAKFAST, e("Idli", 200f), p("Sambar", 150f), p("Sprouts (Mixed, Boiled)", 60f)),
        a("Moong dal chilla", BREAKFAST, p("Moong Dal Chilla", 200f), v("Tomato", 80f), v("Cucumber", 80f), e("Banana", 80f)),
        a("Paneer bhurji & roti", BREAKFAST, p("Paneer Bhurji", 100f), e("Roti (Whole Wheat)", 60f), v("Cucumber", 80f)),
        a("Upma with sprouts", BREAKFAST, e("Upma", 200f), p("Sprouts (Mixed, Boiled)", 100f), e("Peanuts", 12f)),
        a("Besan chilla & curd", BREAKFAST, p("Besan Chilla", 150f), p("Curd / Yogurt (Plain)", 150f), v("Tomato", 60f)),
        a("Egg omelette & toast", BREAKFAST, p("Omelette (Plain)", 120f), e("Brown Bread", 60f), v("Tomato", 60f), e("Banana", 80f)),
        a("Egg bhurji & roti", BREAKFAST, p("Egg Bhurji", 120f), e("Roti (Whole Wheat)", 60f), v("Cucumber", 60f)),
        a("Boiled eggs & oats", BREAKFAST, p("Egg (Whole, Boiled)", 100f), e("Oats Porridge (Cooked)", 250f), e("Banana", 100f)),
        a("Egg-white scramble & toast", BREAKFAST, p("Egg White (Boiled)", 150f), e("Brown Bread", 60f), v("Mushrooms (Cooked)", 80f), e("Orange", 120f)),
        a("Greek yogurt parfait", BREAKFAST, p("Greek Yogurt (Plain, Low Fat)", 200f), e("Muesli (No Added Sugar)", 40f), v("Strawberries", 100f), e("Almonds", 10f)),
        a("Tofu scramble & toast", BREAKFAST, p("Tofu", 150f), e("Brown Bread", 60f), v("Tomato", 80f), e("Banana", 80f)),
        a("Soy-milk oats", BREAKFAST, p("Soy Milk (Unsweetened)", 250f), e("Rolled Oats (Dry)", 50f), e("Banana", 100f), e("Walnuts", 10f)),
        a("Aloo paratha & curd", BREAKFAST, e("Aloo Paratha", 100f), p("Curd / Yogurt (Plain)", 150f), hearty = true),

        a("Soy-milk oats with seeds", BREAKFAST, p("Soy Milk (Unsweetened)", 300f), e("Rolled Oats (Dry)", 50f), p("Pumpkin Seeds", 15f), e("Banana", 80f)),

        // ---- Mid morning / evening snacks
        a("Greek yogurt, apple & almonds", SNACK, p("Greek Yogurt (Plain, Low Fat)", 150f), e("Apple", 120f), e("Almonds", 12f)),
        a("Roasted chana & coconut water", SNACK, p("Roasted Chana", 40f), e("Coconut Water", 250f)),
        a("Sprouts chaat", SNACK, p("Sprouts (Mixed, Boiled)", 150f), v("Tomato", 50f), v("Onion (Raw)", 30f), e("Peanuts", 10f)),
        a("Whey shake & banana", SNACK, p("Whey Protein (generic scoop)", 30f), e("Milk (Low Fat)", 250f), e("Banana", 100f)),
        a("Fruit & nuts", SNACK, e("Apple", 150f), e("Almonds", 20f), p("Soy Milk (Unsweetened)", 150f)),
        a("Makhana & buttermilk", SNACK, e("Makhana (Roasted)", 25f), p("Buttermilk (Chaas)", 250f), e("Pistachios", 10f)),
        a("Boiled egg & fruit", SNACK, p("Egg (Whole, Boiled)", 100f), e("Orange", 150f), e("Walnuts", 8f)),
        a("Peanut-butter toast", SNACK, e("Brown Bread", 60f), e("Peanut Butter", 20f), e("Banana", 80f), p("Soy Milk (Unsweetened)", 150f)),
        a("Paneer cubes & cucumber", SNACK, p("Cottage Cheese (Low Fat)", 100f), v("Cucumber", 100f), e("Walnuts", 10f), e("Pear", 120f)),
        a("Tuna & cucumber bites", SNACK, p("Tuna (Canned in Water)", 80f), v("Cucumber", 100f), e("Brown Bread", 40f)),

        a("Soy-milk and roasted chana", SNACK, p("Soy Milk (Unsweetened)", 250f), p("Roasted Chana", 40f), e("Banana", 80f)),

        // ---- Lunch
        a("Soya and dal power plate", LUNCH, p("Soya Chunks (Dry)", 40f), p("Dal (Cooked Lentils)", 150f), e("Roti (Whole Wheat)", 80f), v("Salad Greens", 80f), v("Cucumber", 60f)),
        a("Dal, roti & mixed veg", LUNCH, p("Dal (Cooked Lentils)", 200f), e("Roti (Whole Wheat)", 100f), v("Mixed Vegetable Curry", 150f), v("Cucumber", 80f)),
        a("Rajma & rice", LUNCH, p("Rajma (Kidney Beans, Cooked)", 200f), e("Cooked White Rice", 150f), v("Salad Greens", 60f), v("Cucumber", 80f)),
        a("Chole, roti & salad", LUNCH, p("Chole (Chickpea Curry)", 200f), e("Roti (Whole Wheat)", 80f), v("Salad Greens", 60f), v("Tomato", 60f)),
        a("Paneer, roti & cauliflower", LUNCH, p("Paneer", 100f), e("Roti (Whole Wheat)", 100f), v("Cauliflower (Cooked)", 150f), v("Cucumber", 80f)),
        a("Palak paneer & brown rice", LUNCH, p("Palak Paneer", 200f), e("Brown Rice (Cooked)", 150f), v("Cucumber", 80f), v("Tomato", 60f)),
        a("Soya curry & roti", LUNCH, p("Soya Chunks (Dry)", 40f), e("Roti (Whole Wheat)", 100f), v("Mixed Vegetable Curry", 150f), v("Tomato", 80f)),
        a("Khichdi, curd & salad", LUNCH, e("Vegetable Khichdi", 300f), p("Curd / Yogurt (Plain)", 150f), v("Cucumber", 80f)),
        a("Tofu stir-fry & quinoa", LUNCH, p("Tofu", 150f), e("Quinoa (Cooked)", 150f), v("Broccoli (Steamed)", 120f), e("Olive Oil", 5f)),
        a("Moong dal, roti & lauki", LUNCH, p("Moong Dal (Cooked)", 200f), e("Roti (Whole Wheat)", 100f), v("Bottle Gourd (Lauki, Cooked)", 150f), v("Salad Greens", 60f)),
        a("Curd rice & bhindi", LUNCH, e("Curd Rice", 250f), v("Bhindi Masala (Okra)", 120f), p("Moong Dal (Cooked)", 150f)),
        a("Veg biryani & raita", LUNCH, e("Veg Biryani", 250f), p("Curd / Yogurt (Plain)", 150f), v("Cucumber", 80f), hearty = true),
        a("Chicken curry & roti", LUNCH, p("Chicken Curry", 200f), e("Roti (Whole Wheat)", 100f), v("Salad Greens", 80f), v("Cucumber", 80f)),
        a("Grilled chicken, rice & broccoli", LUNCH, p("Chicken Breast (Cooked)", 150f), e("Brown Rice (Cooked)", 150f), v("Broccoli (Steamed)", 120f), e("Olive Oil", 5f)),
        a("Fish curry & rice", LUNCH, p("Fish Curry", 250f), e("Cooked White Rice", 150f), v("Salad Greens", 80f)),
        a("Egg, dal & roti", LUNCH, p("Egg (Whole, Boiled)", 100f), p("Dal (Cooked Lentils)", 150f), e("Roti (Whole Wheat)", 100f), v("Cucumber", 80f)),
        a("Tuna quinoa bowl", LUNCH, p("Tuna (Canned in Water)", 120f), e("Quinoa (Cooked)", 120f), v("Salad Greens", 100f), v("Tomato", 60f), e("Olive Oil", 5f)),
        a("Tandoori chicken, roti & salad", LUNCH, p("Tandoori Chicken", 150f), e("Roti (Whole Wheat)", 80f), v("Salad Greens", 100f), v("Onion (Raw)", 30f)),
        a("Salmon & sweet potato", LUNCH, p("Salmon (Cooked)", 130f), e("Sweet Potato (Boiled)", 150f), v("Broccoli (Steamed)", 120f)),
        a("Prawn stir-fry & rice", LUNCH, p("Prawns (Cooked)", 150f), e("Brown Rice (Cooked)", 150f), v("Green Beans (Boiled)", 120f), e("Olive Oil", 5f)),
        a("Chicken biryani", LUNCH, p("Chicken Biryani", 280f), p("Curd / Yogurt (Plain)", 100f), v("Cucumber", 80f), hearty = true),
        a("Mutton curry & roti", LUNCH, p("Mutton Curry", 150f), e("Roti (Whole Wheat)", 100f), v("Cauliflower (Cooked)", 120f), hearty = true),

        // ---- Dinner (a little lighter and protein-led)
        a("Tofu and dal bowl", DINNER, p("Tofu", 150f), p("Moong Dal (Cooked)", 150f), e("Roti (Whole Wheat)", 60f), v("Cauliflower (Cooked)", 120f)),
        a("Dal, roti & sabzi", DINNER, p("Dal (Cooked Lentils)", 200f), e("Roti (Whole Wheat)", 60f), v("Aloo Gobi", 120f), v("Salad Greens", 60f)),
        a("Paneer bhurji, roti & salad", DINNER, p("Paneer Bhurji", 100f), e("Roti (Whole Wheat)", 60f), v("Salad Greens", 80f), v("Cucumber", 80f)),
        a("Tofu veg stir-fry & roti", DINNER, p("Tofu", 150f), e("Roti (Whole Wheat)", 60f), v("Broccoli (Steamed)", 120f), v("Mushrooms (Cooked)", 80f)),
        a("Grilled fish, roti & veg", DINNER, p("Fish (Grilled, White)", 150f), e("Roti (Whole Wheat)", 60f), v("Cauliflower (Cooked)", 150f), v("Salad Greens", 60f)),
        a("Chicken, roti & mixed veg", DINNER, p("Chicken Breast (Cooked)", 150f), e("Roti (Whole Wheat)", 60f), v("Mixed Vegetable Curry", 150f), v("Cucumber", 60f)),
        a("Egg bhurji, roti & sabzi", DINNER, p("Egg Bhurji", 120f), e("Roti (Whole Wheat)", 60f), v("Cabbage (Cooked)", 120f), v("Salad Greens", 60f)),
        a("Khichdi & curd", DINNER, e("Vegetable Khichdi", 300f), p("Curd / Yogurt (Plain)", 100f), v("Salad Greens", 60f), p("Moong Dal (Cooked)", 80f)),
        a("Soya & quinoa bowl", DINNER, p("Soya Chunks (Dry)", 40f), e("Quinoa (Cooked)", 120f), v("Cauliflower (Cooked)", 150f), v("Tomato", 60f)),
        a("Chole & salad", DINNER, p("Chole (Chickpea Curry)", 200f), e("Roti (Whole Wheat)", 60f), v("Salad Greens", 80f), v("Onion (Raw)", 30f)),
        a("Tandoori chicken & roasted veg", DINNER, p("Tandoori Chicken", 150f), v("Mushrooms (Cooked)", 100f), e("Roti (Whole Wheat)", 60f), v("Salad Greens", 60f)),
        a("Paneer & mushroom roti", DINNER, p("Paneer", 100f), v("Mushrooms (Cooked)", 100f), e("Roti (Whole Wheat)", 60f), v("Cucumber", 80f)),
        a("Turkey & sweet potato", DINNER, p("Turkey Breast (Cooked)", 150f), e("Sweet Potato (Boiled)", 150f), v("Green Beans (Boiled)", 120f)),
        a("Moong dal & lauki", DINNER, p("Moong Dal (Cooked)", 220f), e("Roti (Whole Wheat)", 60f), v("Bottle Gourd (Lauki, Cooked)", 150f), v("Tomato", 60f)),
        a("Fish curry & roti", DINNER, p("Fish Curry", 220f), e("Roti (Whole Wheat)", 60f), v("Salad Greens", 80f)),
        a("Rajma & brown rice", DINNER, p("Rajma (Kidney Beans, Cooked)", 200f), e("Brown Rice (Cooked)", 100f), v("Salad Greens", 80f), v("Cucumber", 60f))
    )

    /** Every food name the generator can use - the caller loads these from the database. */
    val requiredFoodNames: Set<String> by lazy { ARCHETYPES.flatMap { it.parts.map { part -> part.food } }.toSet() }

    private fun slotsFor(meals: Int): List<Pair<String, Float>> = when (meals) {
        3 -> listOf(BREAKFAST to 0.30f, LUNCH to 0.40f, DINNER to 0.30f)
        5 -> listOf(BREAKFAST to 0.22f, MID_MORNING to 0.10f, LUNCH to 0.30f, SNACK to 0.13f, DINNER to 0.25f)
        else -> listOf(BREAKFAST to 0.25f, LUNCH to 0.35f, SNACK to 0.15f, DINNER to 0.25f)
    }

    /** MID_MORNING reuses the snack archetypes. */
    private fun archetypeSlot(slot: String) = if (slot == MID_MORNING) SNACK else slot

    /**
     * Builds a 7-day plan. [foods] is keyed by food name ([requiredFoodNames]); archetypes with a
     * missing food are skipped, and any food the chosen [DietPrefs.style] does not allow rules the
     * whole meal out - so a vegan plan can never contain egg, dairy, fish or meat.
     */
    fun generate(foods: Map<String, Food>, targets: NutritionTargets, prefs: DietPrefs): DietPlan {
        val slots = slotsFor(prefs.mealsPerDay)
        fun baseKcal(arch: Archetype) = arch.parts.sumOf { foods.getValue(it.food).caloriesPer100g * it.grams / 100.0 }.toFloat()
        fun density(arch: Archetype): Float {
            var kcal = 0f; var prot = 0f
            for (part in arch.parts) { val f = foods.getValue(part.food); kcal += f.caloriesPer100g * part.grams / 100f; prot += f.proteinPer100g * part.grams / 100f }
            return if (kcal > 0f) prot / kcal else 0f
        }
        val need = targets.proteinGrams.toFloat() / targets.calorieTarget.coerceAtLeast(1)

        val pools = HashMap<String, List<Archetype>>()
        for ((slot, share) in slots) {
            val aSlot = archetypeSlot(slot)
            val usable = ARCHETYPES.filter { arch ->
                arch.slot == aSlot &&
                    arch.parts.all { part -> foods[part.food]?.allowedFor(prefs.style) == true } &&
                    (prefs.goal != DietGoal.LOSE_FAT || !arch.hearty)
            }
            if (usable.isEmpty()) { pools[slot] = emptyList(); continue }
            // Non-veg eaters get meat/fish at lunch and dinner whenever there is enough choice.
            val meatBased = usable.filter { arch -> arch.parts.any { foods[it.food]?.let { f -> f.category == "Non-vegetarian" && !f.isEgg() } == true } }
            val chosen = if (prefs.style == DietStyle.NON_VEG && (aSlot == LUNCH || aSlot == DINNER) && meatBased.size >= 4) meatBased else usable
            // Keep meals whose natural size is close to the calorie budget of this meal, so portions stay realistic.
            val budget = targets.calorieTarget * share
            val sized = chosen.filter { baseKcal(it) / budget in 0.55f..1.9f }
            val sizedPool = if (sized.size >= 4) sized else chosen.sortedBy { abs(Math.log((baseKcal(it) / budget).toDouble())) }.take(max(4, sized.size))
            // Favour meals that can realistically deliver the day protein; keep at least 4 for variety.
            val strong = sizedPool.filter { density(it) >= need * 0.6f }
            pools[slot] = if (strong.size >= 4) strong else sizedPool.sortedByDescending { density(it) }.take(max(4, strong.size))
        }

        val days = (1..7).map { dow ->
            // (slot index, meal) pairs so a slot with no usable meal never shifts the others
            val built = ArrayList<Pair<Int, PlanMeal>>()
            slots.forEachIndexed { slotIndex, (slot, share) ->
                val pool = pools[slot].orEmpty()
                if (pool.isEmpty()) return@forEachIndexed
                // Rotate through the pool; mid-morning starts later so it differs from the evening snack.
                val offset = if (slot == MID_MORNING) 3 else slotIndex
                val arch = pool[((dow - 1) + offset * 2) % pool.size]
                built.add(slotIndex to buildMeal(arch, slot, share, foods, targets, prefs))
            }
            // If the day is short on protein, swap its weakest meal for the most protein-dense unused alternative.
            repeat(2) {
                if (built.sumOf { it.second.protein.toDouble() } >= targets.proteinGrams * 0.9) return@repeat
                val weakest = built.indices.minByOrNull { built[it].second.protein / slots[built[it].first].second } ?: return@repeat
                val (slotIndex, old) = built[weakest]
                val (slot, share) = slots[slotIndex]
                val best = pools[slot].orEmpty()
                    .filter { cand -> built.none { it.second.title == cand.title } }
                    .maxByOrNull { density(it) } ?: return@repeat
                val rebuilt = buildMeal(best, slot, share, foods, targets, prefs)
                if (rebuilt.protein > old.protein) built[weakest] = slotIndex to rebuilt
            }
            PlanDay(dow, built.map { it.second })
        }
        return DietPlan(days, targets, prefs)
    }

    private fun buildMeal(arch: Archetype, slot: String, share: Float, foods: Map<String, Food>, targets: NutritionTargets, prefs: DietPrefs): PlanMeal {
        val kcalBudget = targets.calorieTarget * share
        val proteinBudget = targets.proteinGrams * share

        fun kcal(part: Part, g: Float) = foods.getValue(part.food).caloriesPer100g * g / 100f
        fun prot(part: Part, g: Float) = foods.getValue(part.food).proteinPer100g * g / 100f
        val ps = arch.parts.filter { it.role == Role.PROTEIN }
        val es = arch.parts.filter { it.role == Role.ENERGY }
        val vs = arch.parts.filter { it.role == Role.VEG }
        val kP = ps.sumOf { kcal(it, it.grams).toDouble() }.toFloat(); val pP = ps.sumOf { prot(it, it.grams).toDouble() }.toFloat()
        val kE = es.sumOf { kcal(it, it.grams).toDouble() }.toFloat(); val pE = es.sumOf { prot(it, it.grams).toDouble() }.toFloat()
        val kV = vs.sumOf { kcal(it, it.grams).toDouble() }.toFloat(); val pV = vs.sumOf { prot(it, it.grams).toDouble() }.toFloat()
        val baseK = (kP + kE + kV).coerceAtLeast(1f)

        // 1) scale the whole meal evenly to the calorie budget (vegetables move less than the rest)
        val f = (kcalBudget / baseK).coerceIn(0.5f, 2.2f)
        val sV = Math.sqrt(f.toDouble()).toFloat().coerceIn(0.85f, 1.3f)
        var sP = f
        var sE = f
        // 2) if protein is short, shift calories from the carb/fat items toward the protein items
        fun protein(sp: Float, se: Float) = pP * sp + pE * se + pV * sV
        if (protein(sP, sE) < proteinBudget * 0.95f && kP > 0f && kE > 0f) {
            var g = 1f
            while (g < 1.9f && protein(f * g, sE) < proteinBudget) {
                g += 0.05f
                sP = f * g
                sE = ((kcalBudget - kV * sV - kP * sP) / kE).coerceAtLeast(0.55f * f)
            }
        }

        var items = arch.parts.map { part ->
            val scale = when (part.role) { Role.PROTEIN -> sP; Role.ENERGY -> sE; Role.VEG -> sV }
            PlanItem(foods.getValue(part.food), roundPortion(part.food, part.grams * scale))
        }
        // 3) caps can leave a meal short: top it up with a simple allowed extra; trim it if rounding overshot
        var total = items.sumOf { it.calories.toDouble() }.toFloat()
        if (total < kcalBudget * 0.92f) {
            val have = items.map { it.food.name }.toSet()
            val extras = listOf("Banana" to 100f, "Almonds" to 15f, "Peanut Butter" to 15f, "Roasted Chana" to 30f)
            for ((name, step) in extras) {
                if (total >= kcalBudget * 0.95f) break
                val food = foods[name] ?: continue
                if (name in have || !food.allowedFor(prefs.style)) continue
                val gap = kcalBudget - total
                val grams = (gap / food.caloriesPer100g * 100f).coerceIn(step * 0.5f, step * 2f)
                val extra = PlanItem(food, roundPortion(name, grams))
                items = items + extra
                total += extra.calories
            }
        } else if (total > kcalBudget * 1.10f) {
            val k = (kcalBudget / total).coerceIn(0.7f, 1f)
            items = items.map { PlanItem(it.food, roundPortion(it.food.name, it.grams * k)) }
        }
        return PlanMeal(slot, arch.title, items)
    }

    /** Practical kitchen portions: whole pieces for countable foods, otherwise the nearest 5 g (1 g for tiny amounts). */
    private fun roundPortion(food: String, grams: Float): Float {
        val unit = Portions.unitGrams(food)
        val r = if (unit != null) max(1f, (grams / unit).roundToInt().toFloat()) * unit
        else if (grams < 25f) max(3f, grams.roundToInt().toFloat())
        else max(5f, (grams / 5f).roundToInt() * 5f)
        return min(r, Portions.maxGrams(food))
    }
}

/** Household units for countable foods, and sanity caps so a plan never asks for 400 g of nuts. */
object Portions {
    private val UNIT = mapOf(
        "Roti (Whole Wheat)" to ("roti" to 40f), "Idli" to ("idli" to 50f), "Egg (Whole, Boiled)" to ("egg" to 50f),
        "Egg White (Boiled)" to ("egg white" to 33f), "Banana" to ("banana" to 100f), "Apple" to ("apple" to 150f),
        "Orange" to ("orange" to 120f), "Brown Bread" to ("slice" to 30f), "White Bread" to ("slice" to 30f),
        "Dosa (Plain)" to ("dosa" to 100f), "Aloo Paratha" to ("paratha" to 100f), "Paratha (Plain)" to ("paratha" to 80f),
        "Samosa" to ("samosa" to 60f), "Pear" to ("pear" to 150f)
    )

    private val INVARIANT_UNITS = setOf("roti", "idli")

    private val MAX = mapOf(
        "Olive Oil" to 15f, "Ghee" to 15f, "Butter" to 20f, "Almonds" to 40f, "Walnuts" to 30f, "Peanuts" to 40f,
        "Pistachios" to 30f, "Cashews" to 30f, "Peanut Butter" to 35f, "Whey Protein (generic scoop)" to 60f,
        "Soya Chunks (Dry)" to 90f, "Makhana (Roasted)" to 40f, "Roasted Chana" to 60f, "Rolled Oats (Dry)" to 90f,
        "Muesli (No Added Sugar)" to 70f, "Chicken Breast (Cooked)" to 260f, "Tandoori Chicken" to 260f,
        "Paneer" to 160f, "Roti (Whole Wheat)" to 160f, "Cooked White Rice" to 320f, "Brown Rice (Cooked)" to 320f, "Idli" to 250f, "Poha" to 300f, "Upma" to 350f, "Quinoa (Cooked)" to 280f, "Dal (Cooked Lentils)" to 320f, "Moong Dal (Cooked)" to 320f, "Rajma (Kidney Beans, Cooked)" to 320f, "Chole (Chickpea Curry)" to 320f, "Banana" to 200f, "Curd / Yogurt (Plain)" to 300f, "Brown Bread" to 120f, "Tofu" to 300f, "Paneer Bhurji" to 180f, "Salmon (Cooked)" to 220f
    )

    /** Grams in one household unit, or null when the food is portioned by weight. */
    fun unitGrams(foodName: String): Float? = UNIT[foodName]?.second
    fun maxGrams(foodName: String): Float = MAX[foodName] ?: 600f

    /** "2 roti · 80 g" for countable foods, "150 g" otherwise. */
    fun describe(foodName: String, grams: Float, liquid: Boolean = false): String {
        val u = UNIT[foodName]
        val amount = if (liquid) "${grams.roundToInt()} ml" else "${grams.roundToInt()} g"
        if (u == null) return amount
        val n = grams / u.second
        val count = if (abs(n - n.roundToInt()) < 0.15f) n.roundToInt().toString() else "%.1f".format(n)
        val plural = if (count == "1" || u.first in INVARIANT_UNITS) u.first else u.first + "s"
        return "$count $plural · $amount"
    }

    private val LIQUIDS = listOf("milk", "buttermilk", "chaas", "lassi", "juice", "water", "coffee", "tea", "cola", "soy milk")
    fun isLiquid(foodName: String, category: String): Boolean =
        category == "Beverages" || LIQUIDS.any { foodName.contains(it, ignoreCase = true) } && !foodName.contains("Rice", true)
}
