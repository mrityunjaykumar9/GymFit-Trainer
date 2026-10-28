package com.freegymtrainer.app.data.seed

import com.freegymtrainer.app.data.local.entity.Food

/**
 * Second batch of foods: everyday Indian dishes, snacks, drinks, fruit/vegetables, treats and
 * fast food. Values are typical per-100g reference figures (cooked/ready-to-eat unless the name
 * says "Dry"/"Raw"), not lab analysis - good enough for tracking, not for medical decisions.
 * New foods are added to existing installs by [com.freegymtrainer.app.data.repository.SeedRepository]
 * (inserted by name, never deleting or changing anything a user already has).
 */
object MoreFoodSeedData {

    private fun f(
        name: String, category: String, kcal: Float, protein: Float, carbs: Float, fat: Float, fiber: Float, serving: Float,
        nutrients: List<String> = emptyList(), highlight: String = ""
    ) = Food(
        name = name, category = category, caloriesPer100g = kcal, proteinPer100g = protein,
        carbsPer100g = carbs, fatPer100g = fat, fiberPer100g = fiber, defaultServingGrams = serving,
        keyNutrients = nutrients, highlight = highlight
    )

    val all: List<Food> = listOf(
        // ---- Indian meals & breakfasts
        f("Paratha (Plain)", "Indian food", 326f, 7.5f, 46f, 12f, 4f, 80f, listOf("Iron", "Fiber"), "Tasty but oil-rich - one is usually plenty, enjoyed with curd or sabzi."),
        f("Aloo Paratha", "Indian food", 250f, 5.5f, 38f, 9f, 3f, 100f, listOf("Potassium", "Fiber"), "A hearty stuffed bread; pair it with curd for protein."),
        f("Upma", "Indian food", 120f, 3f, 18f, 4f, 1.5f, 200f, listOf("Manganese", "Thiamine (B1)"), "A light semolina breakfast; add vegetables and peanuts for more nutrition."),
        f("Vegetable Khichdi", "Indian food", 110f, 4.2f, 19f, 2f, 2f, 250f, listOf("Folate", "Iron", "Fiber"), "Rice and lentils together give a gentle, balanced one-pot meal."),
        f("Curd Rice", "Indian food", 125f, 3f, 20f, 3.5f, 0.5f, 200f, listOf("Calcium", "Probiotics"), "Soothing and easy to digest; a cooling comfort meal."),
        f("Sambar", "Indian food", 55f, 3f, 8f, 1.5f, 2f, 150f, listOf("Folate", "Iron", "Vitamin C"), "Lentil-and-vegetable stew that adds protein and fiber to idli or dosa."),
        f("Rasam", "Indian food", 30f, 1f, 5f, 0.8f, 0.8f, 200f, listOf("Vitamin C", "Potassium"), "A light, tangy soup - very low in calories and easy on digestion."),
        f("Chole (Chickpea Curry)", "Indian food", 150f, 6.5f, 20f, 5f, 6f, 150f, listOf("Folate", "Iron", "Fiber"), "Fiber and plant protein in one dish keep you full for hours."),
        f("Palak Paneer", "Indian food", 150f, 8f, 6f, 11f, 2f, 150f, listOf("Calcium", "Iron", "Vitamin A"), "Spinach plus paneer: iron, calcium and protein together."),
        f("Paneer Bhurji", "Indian food", 220f, 14f, 5f, 16f, 1f, 100f, listOf("Calcium", "Vitamin B12"), "A high-protein vegetarian scramble; go easy on the oil."),
        f("Mixed Vegetable Curry", "Indian food", 70f, 2f, 8f, 3.5f, 2.5f, 150f, listOf("Vitamin A", "Vitamin C", "Fiber"), "A low-calorie way to fill your plate with vegetables."),
        f("Aloo Gobi", "Indian food", 90f, 2.5f, 12f, 4f, 3f, 150f, listOf("Vitamin C", "Vitamin K", "Potassium"), "Potato and cauliflower sabzi - filling and rich in vitamin C."),
        f("Bhindi Masala (Okra)", "Indian food", 80f, 2f, 7f, 5f, 3f, 120f, listOf("Vitamin K", "Folate", "Fiber"), "Okra is high in fiber and has a gentle effect on blood sugar."),
        f("Dhokla", "Indian food", 160f, 7f, 24f, 4f, 2f, 100f, listOf("Protein", "Folate"), "Steamed and fermented gram-flour snack - lighter than fried options."),
        f("Moong Dal Chilla", "Indian food", 150f, 9f, 18f, 4.5f, 3f, 100f, listOf("Protein", "Folate", "Fiber"), "A protein-packed savory pancake, great for breakfast."),
        f("Besan Chilla", "Indian food", 180f, 8f, 22f, 6f, 3.5f, 100f, listOf("Protein", "Iron"), "Gram-flour pancake with a good protein and fiber content."),
        f("Oats Porridge (Cooked)", "Indian food", 71f, 2.5f, 12f, 1.5f, 1.7f, 250f, listOf("Beta-glucan Fiber", "Manganese"), "Cooked oats are very filling for few calories - top with fruit and nuts."),
        f("Vegetable Sandwich", "Indian food", 200f, 6f, 30f, 6f, 3f, 150f, listOf("Fiber", "B Vitamins"), "A light snack; whole-grain bread and extra veggies improve it."),
        f("Chicken Curry", "Non-vegetarian", 150f, 14f, 4f, 8.5f, 1f, 200f, listOf("Niacin (B3)", "Vitamin B6"), "Protein-rich; choose less oil and more gravy vegetables."),
        f("Butter Chicken", "Non-vegetarian", 150f, 11f, 6f, 9f, 1f, 200f, listOf("Vitamin B12", "Niacin"), "Delicious but creamy - keep portions modest if you are cutting."),
        f("Tandoori Chicken", "Non-vegetarian", 160f, 24f, 2f, 6.5f, 0.3f, 150f, listOf("Niacin (B3)", "Selenium"), "Grilled, not fried - one of the best restaurant protein choices."),
        f("Chicken Biryani", "Non-vegetarian", 170f, 9f, 22f, 5.5f, 1f, 250f, listOf("Vitamin B12", "Niacin"), "A big plate adds up quickly - treat it as a full meal."),
        f("Veg Biryani", "Indian food", 150f, 3.5f, 24f, 4.5f, 2f, 250f, listOf("Fiber", "Manganese"), "Fragrant rice with vegetables; best with a side of raita."),
        f("Mutton Curry", "Non-vegetarian", 190f, 17f, 4f, 12f, 1f, 150f, listOf("Iron", "Zinc", "Vitamin B12"), "Rich in iron and zinc; keep to small portions as it is calorie-dense."),
        f("Fish Curry", "Non-vegetarian", 120f, 13f, 3f, 6f, 0.5f, 200f, listOf("Omega-3", "Vitamin B12"), "A good everyday fish dish with healthy fats."),
        f("Fish (Grilled, White)", "Non-vegetarian", 130f, 22f, 0f, 4.5f, 0f, 150f, listOf("Iodine", "Vitamin B12", "Selenium"), "Lean, high-protein and easy to cook - great when losing fat."),
        f("Tuna (Canned in Water)", "Non-vegetarian", 116f, 26f, 0f, 0.8f, 0f, 100f, listOf("Selenium", "Niacin (B3)", "Vitamin D"), "Almost pure protein with very little fat - convenient and cheap."),
        f("Chicken Thigh (Cooked, Skinless)", "Non-vegetarian", 209f, 26f, 0f, 10.9f, 0f, 120f, listOf("Zinc", "Iron", "Vitamin B6"), "A juicier cut than breast with more iron and zinc."),
        f("Egg Bhurji", "Non-vegetarian", 170f, 11f, 3f, 12.5f, 0.5f, 100f, listOf("Vitamin B12", "Choline"), "Indian-style scrambled eggs - quick protein for breakfast."),
        f("Omelette (Plain)", "Non-vegetarian", 154f, 10.6f, 0.6f, 12f, 0f, 100f, listOf("Vitamin B12", "Choline", "Selenium"), "Fast, filling protein; add vegetables for fiber."),
        f("Egg White (Boiled)", "Non-vegetarian", 52f, 11f, 0.7f, 0.2f, 0f, 33f, listOf("Selenium", "Riboflavin (B2)"), "Pure protein with almost no fat - ideal when you need protein with few calories."),

        // ---- Breads & grains
        f("Naan", "Grains", 290f, 9f, 50f, 5.5f, 2f, 90f, listOf("Manganese", "Selenium"), "Soft refined-flour bread - enjoy occasionally; roti is the lighter everyday choice."),
        f("White Bread", "Grains", 265f, 9f, 49f, 3.2f, 2.7f, 60f, listOf("Thiamine (B1)", "Folate"), "Quick energy but low in fiber - brown bread is the better daily pick."),
        f("Ragi Roti", "Grains", 260f, 6f, 55f, 1.5f, 8f, 50f, listOf("Calcium", "Iron", "Fiber"), "Finger millet is unusually rich in calcium and fiber."),
        f("Jowar Roti", "Grains", 250f, 7f, 53f, 1.5f, 7f, 50f, listOf("Fiber", "Iron", "Magnesium"), "Gluten-free sorghum bread with a lot of fiber."),
        f("Bajra Roti", "Grains", 280f, 9f, 50f, 5f, 6f, 50f, listOf("Iron", "Magnesium", "Fiber"), "Pearl millet is rich in iron and keeps you full."),
        f("White Pasta (Cooked)", "Grains", 158f, 5.8f, 31f, 0.9f, 1.8f, 180f, listOf("Manganese", "Selenium"), "Easy to overeat - measure a portion and add vegetables and protein."),
        f("Instant Noodles (Cooked)", "Grains", 138f, 3.3f, 20f, 4.9f, 1f, 200f, listOf("Sodium"), "Convenient but high in salt and low in nutrients - an occasional meal."),
        f("Muesli (No Added Sugar)", "Grains", 360f, 10f, 65f, 6f, 8f, 50f, listOf("Fiber", "Magnesium", "Iron"), "A fiber-rich breakfast base; watch the portion as it is calorie-dense."),
        f("Cornflakes", "Grains", 357f, 7.5f, 84f, 0.4f, 3.3f, 30f, listOf("Iron (fortified)", "B Vitamins (fortified)"), "Quick but mostly fast carbs - pair with milk and fruit."),
        f("Boiled Potato", "Vegetables", 87f, 1.9f, 20f, 0.1f, 1.8f, 150f, listOf("Potassium", "Vitamin C", "Vitamin B6"), "Very filling for the calories when boiled and not fried."),
        f("Murmura (Puffed Rice)", "Grains", 402f, 6f, 90f, 0.5f, 1f, 20f, listOf("Thiamine (B1)"), "Light and crunchy; fine as a low-fat snack in a small bowl."),

        // ---- Legumes, seeds, nuts & flours
        f("Besan (Gram Flour, Dry)", "Legumes", 387f, 22f, 58f, 6.7f, 11f, 30f, listOf("Folate", "Iron", "Protein"), "Higher in protein than wheat flour - good for chilla and dhokla."),
        f("Sattu (Roasted Gram Flour)", "Legumes", 413f, 22f, 65f, 6f, 18f, 30f, listOf("Fiber", "Iron", "Protein"), "A traditional protein-and-fiber drink base, mixed with water and spices."),
        f("Roasted Chana", "Legumes", 370f, 20f, 58f, 6f, 17f, 30f, listOf("Protein", "Fiber", "Folate"), "A crunchy, high-protein, high-fiber snack."),
        f("Makhana (Roasted)", "Nuts", 360f, 10f, 75f, 1.5f, 12f, 30f, listOf("Magnesium", "Potassium"), "Light, crunchy fox nuts - a low-fat snack that keeps hunger away."),
        f("Flax Seeds", "Nuts", 534f, 18f, 29f, 42f, 27f, 10f, listOf("Omega-3 (ALA)", "Fiber", "Magnesium"), "A spoonful adds omega-3 and a lot of fiber."),
        f("Chia Seeds", "Nuts", 486f, 17f, 42f, 31f, 34f, 15f, listOf("Omega-3 (ALA)", "Fiber", "Calcium"), "Soak them in milk or water - a high-fiber pudding base."),
        f("Pumpkin Seeds", "Nuts", 559f, 30f, 11f, 49f, 6f, 20f, listOf("Magnesium", "Zinc", "Iron"), "One of the best nut-and-seed sources of magnesium and zinc."),
        f("Sunflower Seeds", "Nuts", 584f, 21f, 20f, 51f, 8.6f, 20f, listOf("Vitamin E", "Selenium"), "Excellent for vitamin E; calorie-dense so keep it to a handful."),
        f("Pistachios", "Nuts", 562f, 20f, 28f, 45f, 10f, 30f, listOf("Vitamin B6", "Potassium"), "Among the lower-calorie nuts and rich in B6."),
        f("Coconut (Fresh)", "Nuts", 354f, 3.3f, 15f, 33f, 9f, 30f, listOf("Manganese", "Copper"), "Rich in fiber and manganese; high in fat, so use small amounts."),
        f("Raisins", "Fruits", 299f, 3.1f, 79f, 0.5f, 3.7f, 20f, listOf("Potassium", "Iron"), "Concentrated sweetness - a few go a long way."),

        // ---- Dairy & alternatives
        f("Milk (Low Fat)", "Dairy", 42f, 3.4f, 5f, 1f, 0f, 250f, listOf("Calcium", "Vitamin B12"), "Same protein and calcium as whole milk with fewer calories."),
        f("Skimmed Milk", "Dairy", 34f, 3.4f, 5f, 0.1f, 0f, 250f, listOf("Calcium", "Vitamin B12"), "The lightest milk option when cutting calories."),
        f("Greek Yogurt (Plain, Low Fat)", "Dairy", 59f, 10f, 3.6f, 0.4f, 0f, 150f, listOf("Calcium", "Vitamin B12", "Probiotics"), "Twice the protein of regular curd - great for fat loss and recovery."),
        f("Buttermilk (Chaas)", "Dairy", 40f, 3.3f, 4.8f, 0.9f, 0f, 250f, listOf("Calcium", "Probiotics"), "A light, hydrating drink with gut-friendly cultures."),
        f("Lassi (Sweet)", "Dairy", 100f, 3f, 15f, 2.5f, 0f, 250f, listOf("Calcium", "Probiotics"), "Tasty but sugary - treat it as dessert, not as water."),
        f("Soy Milk (Unsweetened)", "Vegan", 33f, 2.9f, 1.8f, 1.8f, 0.4f, 250f, listOf("Calcium (fortified)", "Vitamin B12 (fortified)"), "A plant milk with real protein - a solid vegan swap for milk."),

        // ---- Vegetables & fruit
        f("Mushrooms (Cooked)", "Vegetables", 28f, 2.2f, 5.3f, 0.5f, 2f, 100f, listOf("Selenium", "Riboflavin (B2)", "Vitamin D"), "Meaty texture for very few calories."),
        f("Cauliflower (Cooked)", "Vegetables", 23f, 1.8f, 4.1f, 0.5f, 2.3f, 100f, listOf("Vitamin C", "Vitamin K", "Folate"), "A low-calorie, high-volume vegetable - a staple for fat loss."),
        f("Cabbage (Cooked)", "Vegetables", 25f, 1.3f, 5.5f, 0.1f, 1.9f, 100f, listOf("Vitamin C", "Vitamin K"), "Cheap, filling and very low in calories."),
        f("Bottle Gourd (Lauki, Cooked)", "Vegetables", 15f, 0.6f, 3.4f, 0.1f, 0.5f, 150f, listOf("Vitamin C", "Potassium"), "Mostly water; one of the lightest vegetables you can eat."),
        f("Brinjal (Cooked)", "Vegetables", 35f, 0.8f, 8.7f, 0.2f, 2.5f, 100f, listOf("Manganese", "Antioxidants"), "Fiber-rich and low in calories when not deep fried."),
        f("Green Beans (Boiled)", "Vegetables", 35f, 1.9f, 7.9f, 0.3f, 3.2f, 100f, listOf("Vitamin K", "Vitamin C", "Folate"), "A crunchy low-calorie side with decent fiber."),
        f("Onion (Raw)", "Vegetables", 40f, 1.1f, 9.3f, 0.1f, 1.7f, 50f, listOf("Vitamin C", "Quercetin"), "Adds flavor for almost no calories."),
        f("Salad Greens", "Vegetables", 15f, 1.4f, 2.9f, 0.2f, 1.3f, 60f, listOf("Vitamin K", "Folate", "Vitamin A"), "Fill half your plate with these for volume at almost no calories."),
        f("Avocado", "Fruits", 160f, 2f, 8.5f, 15f, 6.7f, 100f, listOf("Potassium", "Vitamin K", "Folate"), "Healthy monounsaturated fats and lots of fiber."),
        f("Mango", "Fruits", 60f, 0.8f, 15f, 0.4f, 1.6f, 150f, listOf("Vitamin C", "Vitamin A"), "Delicious and rich in vitamins - mind the portion on a diet."),
        f("Papaya", "Fruits", 43f, 0.5f, 11f, 0.3f, 1.7f, 150f, listOf("Vitamin C", "Vitamin A", "Folate"), "Low in calories and supports digestion."),
        f("Grapes", "Fruits", 69f, 0.7f, 18f, 0.2f, 0.9f, 100f, listOf("Vitamin K", "Antioxidants"), "Easy to overeat - portion a bowl rather than eating from the bunch."),
        f("Pear", "Fruits", 57f, 0.4f, 15f, 0.1f, 3.1f, 150f, listOf("Fiber", "Vitamin C"), "High in fiber for the calories, so it keeps you full."),
        f("Strawberries", "Fruits", 32f, 0.7f, 7.7f, 0.3f, 2f, 100f, listOf("Vitamin C", "Manganese"), "Very low in calories with a big vitamin C hit."),
        f("Kiwi", "Fruits", 61f, 1.1f, 15f, 0.5f, 3f, 75f, listOf("Vitamin C", "Vitamin K"), "More vitamin C than an orange, plus fiber."),
        f("Muskmelon", "Fruits", 34f, 0.8f, 8.2f, 0.2f, 0.9f, 150f, listOf("Vitamin A", "Vitamin C", "Potassium"), "Hydrating and low in calories."),
        f("Chikoo (Sapota)", "Fruits", 83f, 0.4f, 20f, 1.1f, 5.3f, 100f, listOf("Fiber", "Vitamin C"), "Naturally sweet and high in fiber."),
        f("Custard Apple (Sitaphal)", "Fruits", 94f, 2.1f, 24f, 0.3f, 4.4f, 100f, listOf("Vitamin C", "Potassium", "Fiber"), "A seasonal sweet treat that is surprisingly high in fiber."),

        // ---- Fats, sweeteners & pantry
        f("Olive Oil", "Oils & Fats", 884f, 0f, 0f, 100f, 0f, 5f, listOf("Vitamin E", "Monounsaturated Fat"), "Heart-friendly fat - measure it with a spoon, it is very calorie-dense."),
        f("Honey", "Sweets", 304f, 0.3f, 82f, 0f, 0.2f, 21f, listOf("Antioxidants"), "Natural sugar - still counts as added sugar, so keep it small."),
        f("Sugar", "Sweets", 387f, 0f, 100f, 0f, 0f, 5f, emptyList(), "Pure carbohydrate with no nutrients - easy to overdo in tea and coffee."),
        f("Jaggery", "Sweets", 383f, 0.4f, 98f, 0.1f, 0f, 10f, listOf("Iron", "Potassium"), "Traditional sweetener with trace iron - but still mostly sugar."),

        // ---- Drinks
        f("Tea with Milk & Sugar", "Beverages", 45f, 1.2f, 7f, 1.5f, 0f, 150f, listOf("Calcium"), "Calories add up with several cups a day - cut the sugar to save more."),
        f("Coffee with Milk & Sugar", "Beverages", 50f, 1.5f, 7f, 1.8f, 0f, 200f, listOf("Calcium"), "Count it as a snack if you take it sweet and creamy."),
        f("Black Coffee", "Beverages", 1f, 0.1f, 0f, 0f, 0f, 200f, listOf("Antioxidants"), "Practically zero calories - a handy pre-workout drink."),
        f("Coconut Water", "Beverages", 19f, 0.7f, 3.7f, 0.2f, 1.1f, 250f, listOf("Potassium", "Magnesium"), "A natural electrolyte drink with few calories."),
        f("Orange Juice (Fresh)", "Beverages", 45f, 0.7f, 10f, 0.2f, 0.2f, 250f, listOf("Vitamin C", "Folate"), "Juice has far less fiber than the whole fruit - eat the orange when you can."),
        f("Cola", "Beverages", 42f, 0f, 10.6f, 0f, 0f, 330f, emptyList(), "About 8 teaspoons of sugar per can with no nutrition."),

        // ---- Snacks, sweets & fast food (so real-life eating can be logged honestly)
        f("Samosa", "Snacks", 262f, 4.5f, 28f, 15f, 2.3f, 60f, listOf("Fiber"), "Deep-fried and calorie-dense - one is a treat, not a snack to graze on."),
        f("Pakora (Mixed Veg)", "Snacks", 290f, 7f, 28f, 16f, 4f, 50f, listOf("Protein", "Fiber"), "Fried in oil, so the calories add up fast."),
        f("Khakhra", "Snacks", 380f, 12f, 60f, 9f, 8f, 20f, listOf("Fiber", "Iron"), "A crisp, dry roasted flatbread - a lighter packaged snack."),
        f("Peanut Chikki", "Snacks", 500f, 12f, 60f, 24f, 4f, 30f, listOf("Protein", "Iron"), "Energy-dense; handy before a hard workout."),
        f("Protein Bar (generic)", "Snacks", 380f, 30f, 35f, 12f, 6f, 60f, listOf("Protein"), "A convenient emergency protein hit - check the label as brands vary."),
        f("Dark Chocolate (70%)", "Sweets", 600f, 8f, 46f, 43f, 11f, 20f, listOf("Magnesium", "Iron", "Antioxidants"), "A small square satisfies cravings with less sugar than milk chocolate."),
        f("Ice Cream (Vanilla)", "Sweets", 207f, 3.5f, 24f, 11f, 0.7f, 100f, listOf("Calcium"), "Fine as an occasional treat - log it honestly and move on."),
        f("Gulab Jamun", "Sweets", 320f, 4f, 52f, 11f, 0.5f, 50f, listOf("Calcium"), "Fried and soaked in syrup - one piece is plenty."),
        f("Jalebi", "Sweets", 380f, 2f, 72f, 9f, 0f, 30f, emptyList(), "Very high in sugar and fat - a festival treat."),
        f("Rasgulla", "Sweets", 186f, 4f, 38f, 1f, 0f, 50f, listOf("Calcium"), "Lower in fat than most Indian sweets, but still sugary."),
        f("Kheer (Rice Pudding)", "Sweets", 140f, 3.5f, 22f, 4.5f, 0.3f, 150f, listOf("Calcium"), "A creamy dessert - a small bowl is enough."),
        f("Pizza (Cheese, Slice)", "Snacks", 266f, 11f, 33f, 10f, 2.3f, 110f, listOf("Calcium"), "A slice or two is fine; thin crust and veggie toppings help."),
        f("Burger (Veg)", "Snacks", 230f, 6f, 30f, 10f, 2.5f, 150f, listOf("Fiber"), "Convenient fast food - skip the fries and soda to keep it reasonable."),
        f("Burger (Chicken)", "Non-vegetarian", 250f, 13f, 28f, 10f, 1.5f, 160f, listOf("Niacin (B3)"), "Decent protein, but sauces and buns raise the calories."),
        f("French Fries", "Snacks", 312f, 3.4f, 41f, 15f, 3.8f, 100f, listOf("Potassium"), "Deep-fried potato - share a small portion rather than a large one.")
    )
}
