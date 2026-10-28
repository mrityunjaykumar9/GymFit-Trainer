package com.freegymtrainer.app.domain.nutrition

import kotlin.math.roundToInt

data class ShoppingItem(val name: String, val grams: Int, val category: String)

data class ShoppingGroup(val title: String, val items: List<ShoppingItem>)

/** Adds up every food in the 7-day plan into a grocery checklist grouped by aisle. */
object ShoppingList {

    private val GROUPS = listOf(
        "Vegetables & salad" to setOf("Vegetables"),
        "Fruits" to setOf("Fruits"),
        "Meat, fish & eggs" to setOf("Non-vegetarian"),
        "Dairy" to setOf("Dairy"),
        "Pulses & plant protein" to setOf("Legumes", "Vegan"),
        "Grains, breads & cooked dishes" to setOf("Grains", "Indian food"),
        "Nuts, seeds & extras" to setOf("Nuts", "Oils & Fats", "Supplements", "Snacks", "Sweets", "Beverages")
    )

    fun build(plan: DietPlan): List<ShoppingGroup> {
        val totals = LinkedHashMap<String, Pair<Float, String>>() // name -> (grams, category)
        for (day in plan.days) for (meal in day.meals) for (item in meal.items) {
            val prev = totals[item.food.name]
            totals[item.food.name] = ((prev?.first ?: 0f) + item.grams) to item.food.category
        }
        val items = totals.map { (name, v) -> ShoppingItem(name, (v.first / 5f).roundToInt() * 5, v.second) }
        return GROUPS.mapNotNull { (title, categories) ->
            val inGroup = items.filter { it.category in categories }.sortedBy { it.name }
            if (inGroup.isEmpty()) null else ShoppingGroup(title, inGroup)
        } + items.filter { item -> GROUPS.none { item.category in it.second } }.let { rest ->
            if (rest.isEmpty()) emptyList() else listOf(ShoppingGroup("Other", rest.sortedBy { it.name }))
        }
    }

    /** Plain-text version of the list, for sharing. */
    fun asText(groups: List<ShoppingGroup>): String = buildString {
        append("Shopping list for the week\n")
        for (g in groups) {
            append("\n").append(g.title.uppercase()).append("\n")
            for (i in g.items) append("- ").append(i.name).append(": ").append(i.grams).append(" g\n")
        }
        append("\nQuantities are cooked/ready-to-eat weights for 7 days.")
    }
}
