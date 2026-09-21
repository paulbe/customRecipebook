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
     * Display shape: `quantity measurement: ingredient`.
     * Missing unit is omitted (`3: eggs`). A missing quantity is not invented.
     * Density-based gram conversions are marked with a leading `~`.
     */
    fun structuredLine(
        quantity: Double,
        unit: String,
        name: String,
        scale: Int = 1,
        approximate: Boolean = false,
    ): String {
        val qtyText = if (quantity > 0.0) formatAmount(quantity * scale) else ""
        val unitText = unit.trim()
        val nameText = name.trim()
        val lead = listOf(qtyText, unitText).filter { it.isNotEmpty() }.joinToString(" ")
        val core = when {
            lead.isNotEmpty() && nameText.isNotEmpty() -> "$lead: $nameText"
            lead.isNotEmpty() -> "$lead:"
            else -> nameText
        }
        return if (approximate && core.isNotEmpty()) "~$core" else core
    }

    fun ingredientLine(ingredient: Ingredient, system: UnitSystem, scale: Int): String {
        val display = resolveDisplay(
            name = ingredient.name,
            quantityUs = ingredient.quantityUs,
            unitUs = ingredient.unitUs,
            quantityMetric = ingredient.quantityMetric,
            unitMetric = ingredient.unitMetric,
            system = system,
        )
        return structuredLine(display.quantity, display.unit, ingredient.name, scale, display.approximate)
    }

    fun ingredientLine(ingredient: DraftIngredient, system: UnitSystem, scale: Int = 1): String {
        val display = resolveDisplay(
            name = ingredient.name,
            quantityUs = ingredient.quantityUs,
            unitUs = ingredient.unitUs,
            quantityMetric = ingredient.quantityMetric,
            unitMetric = ingredient.unitMetric,
            system = system,
        )
        return structuredLine(display.quantity, display.unit, ingredient.name, scale, display.approximate)
    }

    private data class DisplayMeasurement(
        val quantity: Double,
        val unit: String,
        val approximate: Boolean = false,
    )

    private fun resolveDisplay(
        name: String,
        quantityUs: Double,
        unitUs: String,
        quantityMetric: Double,
        unitMetric: String,
        system: UnitSystem,
    ): DisplayMeasurement {
        val usPresent = hasMeasurement(quantityUs, unitUs)
        val metricPresent = hasMeasurement(quantityMetric, unitMetric)
        return when (system) {
            UnitSystem.US -> when {
                usPresent -> DisplayMeasurement(quantityUs, unitUs)
                metricPresent -> DisplayMeasurement(quantityMetric, unitMetric)
                else -> DisplayMeasurement(0.0, "")
            }
            UnitSystem.METRIC -> when {
                metricPresent -> gramsOrStored(name, quantityMetric, unitMetric)
                usPresent -> convertToGrams(quantityUs, unitUs, name)
                    ?: DisplayMeasurement(quantityUs, unitUs)
                else -> DisplayMeasurement(0.0, "")
            }
        }
    }

    private fun gramsOrStored(name: String, quantity: Double, unit: String): DisplayMeasurement {
        val converted = convertToGrams(quantity, unit, name)
        if (converted != null && converted.unit == "g") return converted
        return DisplayMeasurement(quantity, unit)
    }

    private fun convertToGrams(quantity: Double, unit: String, name: String): DisplayMeasurement? {
        val result = GramConverter.toGrams(quantity, unit, name) ?: return null
        return DisplayMeasurement(result.grams, "g", result.approximate)
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
