package com.freegymtrainer.app.presentation.components

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Food illustrations: "Food Kit" by Kenney (www.kenney.nl), released under CC0 1.0 (public
 * domain) - free for commercial use with no attribution required; we credit it in Settings
 * anyway. Files live in assets/food_icons/ (see LICENSE-Kenney-FoodKit-CC0.txt there).
 */

private val FOOD_ICON_BY_NAME = mapOf(
    "Cooked White Rice" to "rice-ball", "Roti (Whole Wheat)" to "pancakes", "Dal (Cooked Lentils)" to "bowl-soup",
    "Paneer" to "cheese-cut", "Chicken Breast (Cooked)" to "meat-cooked", "Egg (Whole, Boiled)" to "egg-half",
    "Milk (Whole)" to "carton", "Curd / Yogurt (Plain)" to "pudding", "Rolled Oats (Dry)" to "bowl-cereal",
    "Banana" to "banana", "Peanuts" to "bag", "Soya Chunks (Dry)" to "soy", "Rajma (Kidney Beans, Cooked)" to "bowl",
    "Chickpeas (Cooked, Chana)" to "bowl", "Almonds" to "bag", "Sweet Potato (Boiled)" to "pumpkin-basic",
    "Broccoli (Steamed)" to "broccoli", "Spinach (Cooked)" to "cabbage", "Apple" to "apple", "Orange" to "orange",
    "Brown Rice (Cooked)" to "bowl-cereal", "Whey Protein (generic scoop)" to "carton-small", "Tofu" to "soy",
    "Salmon (Cooked)" to "fish", "Mutton/Lamb (Cooked)" to "meat-ribs", "Peanut Butter" to "peanut-butter",
    "Idli" to "rice-ball", "Dosa (Plain)" to "pancakes", "Poha" to "plate-deep", "Sprouts (Mixed, Boiled)" to "leek",
    "Walnuts" to "bag", "Cottage Cheese (Low Fat)" to "cheese", "Ghee" to "honey", "Cucumber" to "celery-stick",
    "Tomato" to "tomato", "Watermelon" to "watermelon", "Green Peas (Boiled)" to "cabbage", "Quinoa (Cooked)" to "bowl-cereal",
    "Cashews" to "bag", "Dates (Dried)" to "coconut", "Pineapple" to "pineapple", "Guava" to "pear",
    "Pomegranate" to "cherries", "Carrot (Raw)" to "carrot", "Beetroot (Boiled)" to "beet", "Corn (Boiled)" to "corn",
    "Butter" to "whipped-cream", "Cheddar Cheese" to "cheese-cut", "Moong Dal (Cooked)" to "bowl-soup",
    "Brown Bread" to "loaf", "Prawns (Cooked)" to "mussel", "Rohu Fish (Cooked)" to "fish", "Turkey Breast (Cooked)" to "turkey",
    // second batch of foods
    "Paratha (Plain)" to "pancakes", "Aloo Paratha" to "pancakes", "Upma" to "bowl-cereal", "Vegetable Khichdi" to "bowl",
    "Curd Rice" to "bowl-cereal", "Sambar" to "bowl-soup", "Rasam" to "bowl-broth", "Chole (Chickpea Curry)" to "bowl",
    "Palak Paneer" to "bowl", "Paneer Bhurji" to "cheese-cut", "Mixed Vegetable Curry" to "bowl", "Aloo Gobi" to "bowl",
    "Bhindi Masala (Okra)" to "bowl", "Dhokla" to "cheese-cut", "Moong Dal Chilla" to "pancakes", "Besan Chilla" to "pancakes",
    "Oats Porridge (Cooked)" to "bowl-cereal", "Vegetable Sandwich" to "sandwich", "Chicken Curry" to "bowl", "Butter Chicken" to "bowl",
    "Tandoori Chicken" to "meat-cooked", "Chicken Biryani" to "plate-deep", "Veg Biryani" to "plate-deep", "Mutton Curry" to "meat-ribs",
    "Fish Curry" to "fish", "Fish (Grilled, White)" to "fish", "Tuna (Canned in Water)" to "fish", "Chicken Thigh (Cooked, Skinless)" to "meat-cooked",
    "Egg Bhurji" to "egg-cooked", "Omelette (Plain)" to "egg-cooked", "Egg White (Boiled)" to "egg-half",
    "Naan" to "loaf-baguette", "White Bread" to "loaf", "Ragi Roti" to "pancakes", "Jowar Roti" to "pancakes", "Bajra Roti" to "pancakes",
    "White Pasta (Cooked)" to "plate-deep", "Instant Noodles (Cooked)" to "bowl-broth", "Muesli (No Added Sugar)" to "bowl-cereal",
    "Cornflakes" to "bowl-cereal", "Boiled Potato" to "pumpkin-basic", "Murmura (Puffed Rice)" to "bag",
    "Besan (Gram Flour, Dry)" to "bag", "Sattu (Roasted Gram Flour)" to "bag", "Roasted Chana" to "bag", "Makhana (Roasted)" to "bag",
    "Flax Seeds" to "bag", "Chia Seeds" to "bag", "Pumpkin Seeds" to "bag", "Sunflower Seeds" to "bag", "Pistachios" to "bag",
    "Coconut (Fresh)" to "coconut", "Raisins" to "grapes",
    "Milk (Low Fat)" to "carton", "Skimmed Milk" to "carton", "Greek Yogurt (Plain, Low Fat)" to "pudding", "Buttermilk (Chaas)" to "glass",
    "Lassi (Sweet)" to "glass", "Soy Milk (Unsweetened)" to "carton",
    "Mushrooms (Cooked)" to "mushroom", "Cauliflower (Cooked)" to "broccoli", "Cabbage (Cooked)" to "cabbage",
    "Bottle Gourd (Lauki, Cooked)" to "celery-stick", "Brinjal (Cooked)" to "eggplant", "Green Beans (Boiled)" to "leek",
    "Onion (Raw)" to "onion", "Salad Greens" to "salad", "Avocado" to "avocado", "Mango" to "orange", "Papaya" to "pear",
    "Grapes" to "grapes", "Pear" to "pear", "Strawberries" to "strawberry", "Kiwi" to "lemon", "Muskmelon" to "watermelon",
    "Chikoo (Sapota)" to "coconut", "Custard Apple (Sitaphal)" to "pear",
    "Olive Oil" to "bottle-oil", "Honey" to "honey", "Sugar" to "cookie", "Jaggery" to "cookie",
    "Tea with Milk & Sugar" to "cup-tea", "Coffee with Milk & Sugar" to "cup-coffee", "Black Coffee" to "cup-coffee",
    "Coconut Water" to "coconut", "Orange Juice (Fresh)" to "glass", "Cola" to "soda-can",
    "Samosa" to "dim-sum", "Pakora (Mixed Veg)" to "dim-sum", "Khakhra" to "cookie", "Peanut Chikki" to "cookie",
    "Protein Bar (generic)" to "cookie", "Dark Chocolate (70%)" to "cookie", "Ice Cream (Vanilla)" to "ice-cream", "Gulab Jamun" to "donut",
    "Jalebi" to "donut", "Rasgulla" to "pudding", "Kheer (Rice Pudding)" to "pudding", "Pizza (Cheese, Slice)" to "pizza",
    "Burger (Veg)" to "burger", "Burger (Chicken)" to "burger", "French Fries" to "fries"
)

private val FOOD_ICON_BY_CATEGORY = mapOf(
    "Grains" to "bread", "Legumes" to "bowl", "Dairy" to "carton", "Non-vegetarian" to "meat-cooked",
    "Vegetables" to "carrot", "Fruits" to "apple", "Nuts" to "bag", "Vegan" to "soy",
    "Supplements" to "carton-small", "Indian food" to "bowl-cereal", "Snacks" to "cookie", "Beverages" to "cup-tea",
    "Sweets" to "ice-cream", "Oils & Fats" to "bottle-oil"
)

private val MEAL_ICON = mapOf(
    "Breakfast" to "egg-cooked", "Mid Morning" to "apple", "Lunch" to "salad",
    "Pre Workout" to "banana", "Post Workout" to "meat-cooked", "Dinner" to "bowl-soup", "Snack" to "cookie"
)

/** File name (no extension) of the illustration to use for a food. */
fun foodIconFile(foodName: String, category: String? = null): String =
    FOOD_ICON_BY_NAME[foodName] ?: FOOD_ICON_BY_CATEGORY[category] ?: "plate-dinner"

fun mealIconFile(mealType: String): String = MEAL_ICON[mealType] ?: "plate-dinner"

private val iconCache = LruCache<String, ImageBitmap>(96)

@Composable
fun rememberFoodIconBitmap(file: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(file) {
        iconCache.get(file) ?: decodeSampledAssetBitmap(context, "food_icons/$file.png", 128, 128)?.also { iconCache.put(file, it) }
    }
}

/** A food illustration on a soft rounded tile. Purely decorative (the name is always shown next to it). */
@Composable
fun FoodIcon(
    file: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    // The illustrations are drawn for light backgrounds, so the tile stays light in dark theme too.
    background: Color = Color(0xFFFFF1E6)
) {
    val bitmap = rememberFoodIconBitmap(file)
    Box(
        modifier.size(size).clip(RoundedCornerShape(size / 3.2f)).background(background),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(size).padding(size / 11))
        }
    }
}
