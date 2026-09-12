package com.customrecipebook.app.domain

import com.customrecipebook.app.data.DraftIngredient
import com.customrecipebook.app.data.Ingredient
import com.customrecipebook.app.data.UnitSystem
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

object QuantityFormatter {
    private val fractions = listOf(
        0.25 to "¼",
        1.0 / 3.0 to "⅓",
        0.5 to "½",
        2.0 / 3.0 to "⅔",
        0.75 to "¾",
    )

    fun formatAmount(amount: Double): String {
        if (amount <= 0.0) return "0"
        val rounded = (amount * 100.0).roundToInt() / 100.0
        val whole = floor(rounded + 1e-6).toInt()
        val frac = rounded - whole
        if (frac < 0.03) return whole.toString()
        val match = fractions.minBy { abs(it.first - frac) }
        if (abs(match.first - frac) < 0.04) {
            return if (whole == 0) match.second else "$whole${match.second}"
        }
        return if (abs(rounded - rounded.roundToInt()) < 0.05) {
            rounded.roundToInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", rounded)
        }
    }

    /**
     * Display shape: `qty: unit ingredient name`.
     * Missing unit is omitted (`2: eggs`). A missing quantity is not invented.
     */
    fun structuredLine(quantity: Double, unit: String, name: String, scale: Int = 1): String {
        val qtyText = if (quantity > 0.0) formatAmount(quantity * scale) else ""
        val rest = listOf(unit.trim(), name.trim()).filter { it.isNotEmpty() }.joinToString(" ")
        return when {
            qtyText.isNotEmpty() && rest.isNotEmpty() -> "$qtyText: $rest"
            qtyText.isNotEmpty() -> "$qtyText:"
            else -> rest
        }
    }

    fun ingredientLine(ingredient: Ingredient, system: UnitSystem, scale: Int): String {
        val (quantity, unit) = pickFields(
            quantityUs = ingredient.quantityUs,
            unitUs = ingredient.unitUs,
            quantityMetric = ingredient.quantityMetric,
            unitMetric = ingredient.unitMetric,
            system = system,
        )
        return structuredLine(quantity, unit, ingredient.name, scale)
    }

    fun ingredientLine(ingredient: DraftIngredient, system: UnitSystem, scale: Int = 1): String {
        val (quantity, unit) = pickFields(
            quantityUs = ingredient.quantityUs,
            unitUs = ingredient.unitUs,
            quantityMetric = ingredient.quantityMetric,
            unitMetric = ingredient.unitMetric,
            system = system,
        )
        return structuredLine(quantity, unit, ingredient.name, scale)
    }

    private fun pickFields(
        quantityUs: Double,
        unitUs: String,
        quantityMetric: Double,
        unitMetric: String,
        system: UnitSystem,
    ): Pair<Double, String> {
        val us = quantityUs to unitUs
        val metric = quantityMetric to unitMetric
        val usPresent = hasMeasurement(quantityUs, unitUs)
        val metricPresent = hasMeasurement(quantityMetric, unitMetric)
        return when (system) {
            UnitSystem.US -> if (usPresent) us else if (metricPresent) metric else 0.0 to ""
            UnitSystem.METRIC -> if (metricPresent) metric else if (usPresent) us else 0.0 to ""
        }
    }

    private fun hasMeasurement(quantity: Double, unit: String): Boolean =
        quantity > 0.0 || unit.isNotBlank()

    fun formatMinutes(minutes: Int): String {
        return if (minutes >= 60 && minutes % 60 == 0) {
            val hours = minutes / 60
            if (hours == 1) "1 hr" else "$hours hr"
        } else if (minutes >= 60) {
            val hours = minutes / 60
            val rem = minutes % 60
            "${hours} hr ${rem} min"
        } else {
            "$minutes min"
        }
    }
}
