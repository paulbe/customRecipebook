package com.customrecipebook.app

import com.customrecipebook.app.data.Ingredient
import com.customrecipebook.app.data.UnitSystem
import com.customrecipebook.app.domain.QuantityFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantityFormatterTest {
    private val flour = Ingredient(
        id = "1",
        recipeId = "cookies",
        name = "all-purpose flour",
        quantityUs = 2.0,
        unitUs = "cups",
        quantityMetric = 240.0,
        unitMetric = "g",
        sortOrder = 0,
    )

    @Test
    fun wholeAmountsStayIntegers() {
        assertEquals("2", QuantityFormatter.formatAmount(2.0))
        assertEquals("12", QuantityFormatter.formatAmount(12.0))
    }

    @Test
    fun commonFractionsUseUnicode() {
        assertEquals("½", QuantityFormatter.formatAmount(0.5))
        assertEquals("¼", QuantityFormatter.formatAmount(0.25))
        assertEquals("1½", QuantityFormatter.formatAmount(1.5))
    }

    @Test
    fun usLineShowsMetricInParentheses() {
        val line = QuantityFormatter.ingredientLine(flour, UnitSystem.US, scale = 1)
        assertEquals("2 cups (240 g) all-purpose flour", line)
    }

    @Test
    fun metricLineShowsUsInParentheses() {
        val line = QuantityFormatter.ingredientLine(flour, UnitSystem.METRIC, scale = 1)
        assertEquals("240 g (2 cups) all-purpose flour", line)
    }

    @Test
    fun batchScaleDoublesBothUnits() {
        val line = QuantityFormatter.ingredientLine(flour, UnitSystem.US, scale = 2)
        assertEquals("4 cups (480 g) all-purpose flour", line)
    }

    @Test
    fun minutesFormatHours() {
        assertEquals("35 min", QuantityFormatter.formatMinutes(35))
        assertEquals("1 hr", QuantityFormatter.formatMinutes(60))
        assertEquals("3 hr", QuantityFormatter.formatMinutes(180))
    }

    @Test
    fun approximateMetricKeepsTilde() {
        val eggs = flour.copy(
            name = "eggs",
            quantityUs = 2.0,
            unitUs = "large",
            quantityMetric = 100.0,
            unitMetric = "g",
            metricApprox = true,
        )
        val line = QuantityFormatter.ingredientLine(eggs, UnitSystem.US, 1)
        assertTrue(line.contains("~100 g"))
    }
}
