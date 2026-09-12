package com.customrecipebook.app.domain

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

    fun formatPair(
        primaryAmount: Double,
        primaryUnit: String,
        secondaryAmount: Double,
        secondaryUnit: String,
        secondaryApprox: Boolean = false,
    ): String {
        val primary = "${formatAmount(primaryAmount)} $primaryUnit".trim()
        val prefix = if (secondaryApprox) "~" else ""
        val secondary = "$prefix${formatAmount(secondaryAmount)} $secondaryUnit".trim()
        return "$primary ($secondary)"
    }

    fun ingredientLine(ingredient: Ingredient, system: UnitSystem, scale: Int): String {
        if (ingredient.unitUs.isBlank() && ingredient.unitMetric.isBlank()) {
            return ingredient.name
        }
        val usQty = ingredient.quantityUs * scale
        val metricQty = ingredient.quantityMetric * scale
        val pair = when (system) {
            UnitSystem.US -> formatPair(
                usQty,
                ingredient.unitUs,
                metricQty,
                ingredient.unitMetric,
                ingredient.metricApprox,
            )
            UnitSystem.METRIC -> formatPair(
                metricQty,
                ingredient.unitMetric,
                usQty,
                ingredient.unitUs,
                secondaryApprox = false,
            )
        }
        return "$pair ${ingredient.name}".trim()
    }

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
