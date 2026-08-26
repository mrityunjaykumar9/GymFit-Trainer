package com.freegymtrainer.app.data.seed

import com.freegymtrainer.app.data.local.entity.Food

/** All values are per 100g and are typical/approximate reference figures, not lab-measured. */
object FoodSeedData {

    private fun f(name: String, category: String, kcal: Float, protein: Float, carbs: Float, fat: Float, fiber: Float, serving: Float) =
        Food(name = name, category = category, caloriesPer100g = kcal, proteinPer100g = protein,
            carbsPer100g = carbs, fatPer100g = fat, fiberPer100g = fiber, defaultServingGrams = serving)

    val all: List<Food> = listOf(
        // Indian staples
        f("Cooked White Rice", "Grains", 130f, 2.7f, 28f, 0.3f, 0.4f, 150f),
        f("Roti (Whole Wheat)", "Grains", 297f, 11f, 58f, 3.7f, 9.6f, 40f),
        f("Dal (Cooked Lentils)", "Legumes", 116f, 9f, 20f, 0.4f, 8f, 150f),
        f("Paneer", "Dairy", 265f, 18f, 3.4f, 20f, 0f, 100f),
        f("Chicken Breast (Cooked)", "Non-vegetarian", 165f, 31f, 0f, 3.6f, 0f, 150f),
        f("Egg (Whole, Boiled)", "Non-vegetarian", 155f, 13f, 1.1f, 11f, 0f, 50f),
        f("Milk (Whole)", "Dairy", 61f, 3.2f, 4.8f, 3.3f, 0f, 250f),
        f("Curd / Yogurt (Plain)", "Dairy", 60f, 3.5f, 4.7f, 3.3f, 0f, 200f),
        f("Rolled Oats (Dry)", "Grains", 389f, 17f, 66f, 7f, 10.6f, 40f),
        f("Banana", "Fruits", 89f, 1.1f, 23f, 0.3f, 2.6f, 120f),
        f("Peanuts", "Nuts", 567f, 26f, 16f, 49f, 8.5f, 30f),
        f("Soya Chunks (Dry)", "Vegan", 345f, 52f, 33f, 0.5f, 13f, 50f),
        f("Rajma (Kidney Beans, Cooked)", "Legumes", 127f, 8.7f, 23f, 0.5f, 6.4f, 150f),
        f("Chickpeas (Cooked, Chana)", "Legumes", 164f, 8.9f, 27f, 2.6f, 7.6f, 150f),
        f("Almonds", "Nuts", 579f, 21f, 22f, 50f, 12.5f, 28f),
        f("Sweet Potato (Boiled)", "Vegetables", 86f, 1.6f, 20f, 0.1f, 3f, 150f),
        f("Broccoli (Steamed)", "Vegetables", 35f, 2.4f, 7.2f, 0.4f, 3.3f, 100f),
        f("Spinach (Cooked)", "Vegetables", 23f, 2.9f, 3.6f, 0.4f, 2.2f, 100f),
        f("Apple", "Fruits", 52f, 0.3f, 14f, 0.2f, 2.4f, 180f),
        f("Orange", "Fruits", 47f, 0.9f, 12f, 0.1f, 2.4f, 150f),
        f("Brown Rice (Cooked)", "Grains", 112f, 2.6f, 24f, 0.9f, 1.8f, 150f),
        f("Whey Protein (generic scoop)", "Supplements", 380f, 78f, 8f, 5f, 1f, 30f),
        f("Tofu", "Vegan", 76f, 8f, 1.9f, 4.8f, 0.3f, 100f),
        f("Salmon (Cooked)", "Non-vegetarian", 208f, 20f, 0f, 13f, 0f, 150f),
        f("Mutton/Lamb (Cooked)", "Non-vegetarian", 258f, 25f, 0f, 17f, 0f, 100f),
        f("Peanut Butter", "Nuts", 588f, 25f, 20f, 50f, 6f, 32f),
        f("Idli", "Indian food", 58f, 2f, 12f, 0.2f, 0.5f, 100f),
        f("Dosa (Plain)", "Indian food", 168f, 3.9f, 28f, 3.7f, 1.4f, 100f),
        f("Poha", "Indian food", 130f, 2.5f, 27f, 1.5f, 1.1f, 150f),
        f("Sprouts (Mixed, Boiled)", "Legumes", 30f, 3f, 6f, 0.2f, 1.8f, 100f),
        f("Walnuts", "Nuts", 654f, 15f, 14f, 65f, 6.7f, 28f),
        f("Cottage Cheese (Low Fat)", "Dairy", 98f, 11f, 3.4f, 4.3f, 0f, 100f),
        f("Ghee", "Dairy", 900f, 0f, 0f, 100f, 0f, 5f),
        f("Cucumber", "Vegetables", 15f, 0.7f, 3.6f, 0.1f, 0.5f, 100f),
        f("Tomato", "Vegetables", 18f, 0.9f, 3.9f, 0.2f, 1.2f, 100f),
        f("Watermelon", "Fruits", 30f, 0.6f, 8f, 0.2f, 0.4f, 200f),
        f("Green Peas (Boiled)", "Vegetables", 84f, 5.4f, 14f, 0.4f, 5.5f, 100f),
        f("Quinoa (Cooked)", "Grains", 120f, 4.4f, 21f, 1.9f, 2.8f, 150f),

        // Additional items
        f("Cashews", "Nuts", 553f, 18f, 30f, 44f, 3.3f, 30f),
        f("Dates (Dried)", "Fruits", 277f, 1.8f, 75f, 0.2f, 6.7f, 40f),
        f("Pineapple", "Fruits", 50f, 0.5f, 13f, 0.1f, 1.4f, 150f),
        f("Guava", "Fruits", 68f, 2.6f, 14f, 1f, 5.4f, 150f),
        f("Pomegranate", "Fruits", 83f, 1.7f, 19f, 1.2f, 4f, 150f),
        f("Carrot (Raw)", "Vegetables", 41f, 0.9f, 10f, 0.2f, 2.8f, 100f),
        f("Beetroot (Boiled)", "Vegetables", 44f, 1.7f, 10f, 0.2f, 2f, 100f),
        f("Corn (Boiled)", "Vegetables", 96f, 3.4f, 21f, 1.5f, 2.4f, 100f),
        f("Butter", "Dairy", 717f, 0.9f, 0.1f, 81f, 0f, 10f),
        f("Cheddar Cheese", "Dairy", 402f, 25f, 1.3f, 33f, 0f, 30f),
        f("Moong Dal (Cooked)", "Legumes", 105f, 7.5f, 19f, 0.4f, 7.6f, 150f),
        f("Brown Bread", "Grains", 247f, 13f, 41f, 3.4f, 7f, 60f),
        f("Prawns (Cooked)", "Non-vegetarian", 99f, 24f, 0.2f, 0.3f, 0f, 100f),
        f("Rohu Fish (Cooked)", "Non-vegetarian", 97f, 17f, 0f, 2.6f, 0f, 150f),
        f("Turkey Breast (Cooked)", "Non-vegetarian", 135f, 30f, 0f, 0.7f, 0f, 150f)
    )
}
