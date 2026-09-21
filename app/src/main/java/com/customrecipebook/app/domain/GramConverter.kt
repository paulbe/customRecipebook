package com.customrecipebook.app.domain

import kotlin.math.roundToInt

/**
 * Converts household volume (and US weight) to grams for the recipe-page Grams view.
 * Densities are typical baking values, not lab measurements.
 */
object GramConverter {
    data class Result(val grams: Double, val approximate: Boolean)

    fun toGrams(quantity: Double, unit: String, ingredientName: String): Result? {
        if (quantity <= 0.0) return null
        val unitKey = normalizeUnit(unit)
        if (unitKey.isEmpty() || unitKey in nonConvertible) return null

        weightToGrams(quantity, unitKey)?.let { grams ->
            return Result(roundGrams(grams), approximate = false)
        }

        val cups = toCups(quantity, unitKey) ?: return null
        val gramsPerCup = densityPerCup(ingredientName)
        val grams = cups * gramsPerCup
        return Result(roundGrams(grams), approximate = true)
    }

    private fun weightToGrams(quantity: Double, unit: String): Double? = when (unit) {
        "g", "gram", "grams" -> quantity
        "kg" -> quantity * 1000.0
        "oz", "ounce", "ounces" -> quantity * 28.3495
        "lb", "lbs", "pound", "pounds" -> quantity * 453.592
        else -> null
    }

    private fun toCups(quantity: Double, unit: String): Double? = when (unit) {
        "cup", "cups" -> quantity
        "tbsp", "tablespoon", "tablespoons" -> quantity / 16.0
        "tsp", "teaspoon", "teaspoons" -> quantity / 48.0
        "stick", "sticks" -> quantity * 0.5
        "ml", "milliliter", "milliliters", "millilitre" -> quantity / 240.0
        "l", "liter", "liters", "litre" -> quantity * 1000.0 / 240.0
        else -> null
    }

    private fun densityPerCup(name: String): Double {
        val key = name.lowercase()
            .replace('’', '\'')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        val match = densities.firstOrNull { (keyword, _) ->
            key == keyword || key.contains(keyword)
        }
        return match?.second ?: WATER_G_PER_CUP
    }

    private fun roundGrams(grams: Double): Double {
        if (grams >= 10.0) return grams.roundToInt().toDouble()
        return (grams * 10.0).roundToInt() / 10.0
    }

    private fun normalizeUnit(unit: String): String = unit.trim().lowercase().replace(".", "")

    private val nonConvertible = setOf(
        "clove", "cloves", "large", "medium", "small",
        "pinch", "dash", "can", "cans", "slice", "slices",
        "bunch", "sprig", "sprigs", "leaf", "leaves",
    )

    private const val WATER_G_PER_CUP = 240.0

    // Longest keywords first so "all purpose flour" wins over "flour".
    private val densities: List<Pair<String, Double>> = listOf(
        "all purpose flour" to 120.0,
        "plain flour" to 120.0,
        "bread flour" to 127.0,
        "cake flour" to 114.0,
        "whole wheat flour" to 120.0,
        "self rising flour" to 125.0,
        "almond flour" to 96.0,
        "coconut flour" to 112.0,
        "powdered sugar" to 120.0,
        "confectioners sugar" to 120.0,
        "icing sugar" to 120.0,
        "brown sugar" to 220.0,
        "granulated sugar" to 200.0,
        "caster sugar" to 200.0,
        "white sugar" to 200.0,
        "unsweetened cocoa" to 85.0,
        "cocoa powder" to 85.0,
        "chocolate chips" to 170.0,
        "peanut butter" to 258.0,
        "vanilla extract" to 208.0,
        "baking soda" to 230.0,
        "baking powder" to 230.0,
        "rolled oats" to 90.0,
        "olive oil" to 216.0,
        "vegetable oil" to 218.0,
        "canola oil" to 218.0,
        "coconut oil" to 218.0,
        "maple syrup" to 312.0,
        "heavy cream" to 240.0,
        "sour cream" to 240.0,
        "whole milk" to 245.0,
        "kosher salt" to 144.0,
        "sea salt" to 273.0,
        "lemon juice" to 240.0,
        "lime juice" to 240.0,
        "soy sauce" to 255.0,
        "breadcrumbs" to 108.0,
        "breadcrumb" to 108.0,
        "cornmeal" to 160.0,
        "cornstarch" to 128.0,
        "flour" to 120.0,
        "sugar" to 200.0,
        "butter" to 227.0,
        "margarine" to 227.0,
        "shortening" to 205.0,
        "oil" to 218.0,
        "honey" to 340.0,
        "molasses" to 328.0,
        "milk" to 245.0,
        "cream" to 240.0,
        "yogurt" to 245.0,
        "water" to 240.0,
        "stock" to 240.0,
        "broth" to 240.0,
        "cocoa" to 85.0,
        "oats" to 90.0,
        "rice" to 185.0,
        "salt" to 273.0,
        "vanilla" to 208.0,
        "yeast" to 144.0,
        "mayonnaise" to 220.0,
        "ketchup" to 272.0,
        "vinegar" to 240.0,
        "wine" to 236.0,
        "walnuts" to 120.0,
        "almonds" to 140.0,
        "pecans" to 110.0,
        "nuts" to 120.0,
        "chocolate" to 170.0,
        "parmesan" to 100.0,
        "cheese" to 113.0,
        "onion" to 160.0,
        "garlic" to 136.0,
        "carrot" to 128.0,
        "celery" to 120.0,
        "tomato" to 180.0,
        "potato" to 150.0,
        "spinach" to 30.0,
        "panko" to 50.0,
    ).sortedByDescending { it.first.length }
}
