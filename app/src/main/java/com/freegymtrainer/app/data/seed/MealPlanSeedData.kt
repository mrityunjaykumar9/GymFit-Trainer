package com.freegymtrainer.app.data.seed

/**
 * Plain seed models (not Room entities) - resolved to real Food IDs by SeedRepository on
 * first launch, the same way WorkoutTemplateSeedData resolves exercise names. Grams are
 * chosen so the app computes calories/protein/carbs/fat at display time from each Food's
 * per-100g values (Food.caloriesPer100g etc.) - no nutrition numbers are hardcoded here.
 */
data class SeedMealItem(val foodName: String, val grams: Float)
data class SeedMeal(val mealType: String, val items: List<SeedMealItem>)
data class SeedPlanDay(val dayOfWeek: Int, val title: String, val meals: List<SeedMeal>)

object MealPlanSeedData {

    private fun i(foodName: String, grams: Float) = SeedMealItem(foodName, grams)
    private fun meal(type: String, vararg items: SeedMealItem) = SeedMeal(type, items.toList())

    val vegPlan: List<SeedPlanDay> = listOf(
        SeedPlanDay(1, "Veg - Monday", listOf(
            meal("Breakfast", i("Rolled Oats (Dry)", 60f), i("Milk (Whole)", 250f), i("Banana", 120f)),
            meal("Lunch", i("Roti (Whole Wheat)", 120f), i("Dal (Cooked Lentils)", 200f), i("Paneer", 100f), i("Tomato", 100f)),
            meal("Snack", i("Curd / Yogurt (Plain)", 200f), i("Almonds", 20f)),
            meal("Dinner", i("Brown Rice (Cooked)", 150f), i("Rajma (Kidney Beans, Cooked)", 200f), i("Spinach (Cooked)", 100f))
        )),
        SeedPlanDay(2, "Veg - Tuesday", listOf(
            meal("Breakfast", i("Poha", 150f), i("Peanuts", 20f), i("Orange", 150f)),
            meal("Lunch", i("Roti (Whole Wheat)", 120f), i("Chickpeas (Cooked, Chana)", 200f), i("Cottage Cheese (Low Fat)", 100f), i("Cucumber", 100f)),
            meal("Snack", i("Apple", 180f), i("Peanut Butter", 32f)),
            meal("Dinner", i("Quinoa (Cooked)", 150f), i("Dal (Cooked Lentils)", 200f), i("Broccoli (Steamed)", 100f))
        )),
        SeedPlanDay(3, "Veg - Wednesday", listOf(
            meal("Breakfast", i("Idli", 200f), i("Dal (Cooked Lentils)", 100f)),
            meal("Lunch", i("Roti (Whole Wheat)", 120f), i("Rajma (Kidney Beans, Cooked)", 200f), i("Paneer", 100f), i("Cucumber", 100f)),
            meal("Snack", i("Milk (Whole)", 250f), i("Walnuts", 20f)),
            meal("Dinner", i("Brown Rice (Cooked)", 150f), i("Chickpeas (Cooked, Chana)", 200f), i("Spinach (Cooked)", 100f))
        )),
        SeedPlanDay(4, "Veg - Thursday", listOf(
            meal("Breakfast", i("Dosa (Plain)", 200f), i("Curd / Yogurt (Plain)", 100f)),
            meal("Lunch", i("Roti (Whole Wheat)", 120f), i("Dal (Cooked Lentils)", 200f), i("Tofu", 150f), i("Tomato", 100f)),
            meal("Snack", i("Banana", 120f), i("Peanuts", 30f)),
            meal("Dinner", i("Quinoa (Cooked)", 150f), i("Rajma (Kidney Beans, Cooked)", 200f), i("Broccoli (Steamed)", 100f))
        )),
        SeedPlanDay(5, "Veg - Friday", listOf(
            meal("Breakfast", i("Rolled Oats (Dry)", 60f), i("Curd / Yogurt (Plain)", 200f), i("Apple", 180f)),
            meal("Lunch", i("Roti (Whole Wheat)", 120f), i("Chickpeas (Cooked, Chana)", 200f), i("Paneer", 100f), i("Spinach (Cooked)", 100f)),
            meal("Snack", i("Whey Protein (generic scoop)", 30f), i("Milk (Whole)", 250f)),
            meal("Dinner", i("Brown Rice (Cooked)", 150f), i("Dal (Cooked Lentils)", 200f), i("Sweet Potato (Boiled)", 150f))
        )),
        SeedPlanDay(6, "Veg - Saturday", listOf(
            meal("Breakfast", i("Poha", 150f), i("Milk (Whole)", 250f)),
            meal("Lunch", i("Roti (Whole Wheat)", 120f), i("Rajma (Kidney Beans, Cooked)", 200f), i("Cottage Cheese (Low Fat)", 100f), i("Cucumber", 100f)),
            meal("Snack", i("Sprouts (Mixed, Boiled)", 100f), i("Almonds", 20f)),
            meal("Dinner", i("Quinoa (Cooked)", 150f), i("Dal (Cooked Lentils)", 200f), i("Tomato", 100f))
        )),
        SeedPlanDay(7, "Veg - Sunday", listOf(
            meal("Breakfast", i("Idli", 200f), i("Peanut Butter", 32f)),
            meal("Lunch", i("Roti (Whole Wheat)", 120f), i("Soya Chunks (Dry)", 50f), i("Paneer", 100f), i("Broccoli (Steamed)", 100f)),
            meal("Snack", i("Curd / Yogurt (Plain)", 200f), i("Walnuts", 20f)),
            meal("Dinner", i("Brown Rice (Cooked)", 150f), i("Chickpeas (Cooked, Chana)", 200f), i("Spinach (Cooked)", 100f))
        ))
    )

    val nonVegPlan: List<SeedPlanDay> = listOf(
        SeedPlanDay(1, "Non-Veg - Monday", listOf(
            meal("Breakfast", i("Egg (Whole, Boiled)", 100f), i("Roti (Whole Wheat)", 40f), i("Banana", 120f)),
            meal("Lunch", i("Chicken Breast (Cooked)", 150f), i("Brown Rice (Cooked)", 150f), i("Broccoli (Steamed)", 100f)),
            meal("Snack", i("Whey Protein (generic scoop)", 30f), i("Milk (Whole)", 250f)),
            meal("Dinner", i("Salmon (Cooked)", 150f), i("Sweet Potato (Boiled)", 150f), i("Spinach (Cooked)", 100f))
        )),
        SeedPlanDay(2, "Non-Veg - Tuesday", listOf(
            meal("Breakfast", i("Egg (Whole, Boiled)", 100f), i("Rolled Oats (Dry)", 60f), i("Milk (Whole)", 250f)),
            meal("Lunch", i("Chicken Breast (Cooked)", 150f), i("Roti (Whole Wheat)", 120f), i("Dal (Cooked Lentils)", 150f), i("Tomato", 100f)),
            meal("Snack", i("Curd / Yogurt (Plain)", 200f), i("Peanuts", 20f)),
            meal("Dinner", i("Mutton/Lamb (Cooked)", 100f), i("Quinoa (Cooked)", 150f), i("Spinach (Cooked)", 100f))
        )),
        SeedPlanDay(3, "Non-Veg - Wednesday", listOf(
            meal("Breakfast", i("Egg (Whole, Boiled)", 100f), i("Poha", 150f)),
            meal("Lunch", i("Chicken Breast (Cooked)", 150f), i("Brown Rice (Cooked)", 150f), i("Chickpeas (Cooked, Chana)", 100f), i("Cucumber", 100f)),
            meal("Snack", i("Milk (Whole)", 250f), i("Almonds", 20f)),
            meal("Dinner", i("Salmon (Cooked)", 150f), i("Roti (Whole Wheat)", 80f), i("Broccoli (Steamed)", 100f))
        )),
        SeedPlanDay(4, "Non-Veg - Thursday", listOf(
            meal("Breakfast", i("Egg (Whole, Boiled)", 100f), i("Idli", 100f)),
            meal("Lunch", i("Mutton/Lamb (Cooked)", 100f), i("Roti (Whole Wheat)", 120f), i("Dal (Cooked Lentils)", 150f), i("Spinach (Cooked)", 100f)),
            meal("Snack", i("Whey Protein (generic scoop)", 30f), i("Banana", 120f)),
            meal("Dinner", i("Chicken Breast (Cooked)", 150f), i("Sweet Potato (Boiled)", 150f), i("Tomato", 100f))
        )),
        SeedPlanDay(5, "Non-Veg - Friday", listOf(
            meal("Breakfast", i("Egg (Whole, Boiled)", 100f), i("Dosa (Plain)", 100f)),
            meal("Lunch", i("Chicken Breast (Cooked)", 150f), i("Quinoa (Cooked)", 150f), i("Broccoli (Steamed)", 100f)),
            meal("Snack", i("Curd / Yogurt (Plain)", 200f), i("Walnuts", 20f)),
            meal("Dinner", i("Salmon (Cooked)", 150f), i("Roti (Whole Wheat)", 80f), i("Dal (Cooked Lentils)", 150f))
        )),
        SeedPlanDay(6, "Non-Veg - Saturday", listOf(
            meal("Breakfast", i("Egg (Whole, Boiled)", 100f), i("Rolled Oats (Dry)", 60f), i("Banana", 120f)),
            meal("Lunch", i("Mutton/Lamb (Cooked)", 100f), i("Brown Rice (Cooked)", 150f), i("Spinach (Cooked)", 100f)),
            meal("Snack", i("Milk (Whole)", 250f), i("Peanut Butter", 20f)),
            meal("Dinner", i("Chicken Breast (Cooked)", 150f), i("Roti (Whole Wheat)", 80f), i("Chickpeas (Cooked, Chana)", 100f))
        )),
        SeedPlanDay(7, "Non-Veg - Sunday", listOf(
            meal("Breakfast", i("Egg (Whole, Boiled)", 100f), i("Idli", 100f), i("Curd / Yogurt (Plain)", 100f)),
            meal("Lunch", i("Salmon (Cooked)", 150f), i("Roti (Whole Wheat)", 120f), i("Dal (Cooked Lentils)", 150f), i("Tomato", 100f)),
            meal("Snack", i("Whey Protein (generic scoop)", 30f), i("Milk (Whole)", 250f)),
            meal("Dinner", i("Chicken Breast (Cooked)", 150f), i("Brown Rice (Cooked)", 150f), i("Broccoli (Steamed)", 100f))
        ))
    )
}
