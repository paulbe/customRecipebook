package com.customrecipebook.app

import com.customrecipebook.app.data.DraftIngredient
import com.customrecipebook.app.data.Ingredient
import com.customrecipebook.app.data.UnitSystem
import com.customrecipebook.app.domain.QuantityFormatter
import org.junit.Assert.assertEquals
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
    fun usLineUsesQtyUnitNameShape() {
        val line = QuantityFormatter.ingredientLine(flour, UnitSystem.US, scale = 1)
        assertEquals("2 cups: all-purpose flour", line)
    }

    @Test
    fun metricLineUsesQtyUnitNameShape() {
        val line = QuantityFormatter.ingredientLine(flour, UnitSystem.METRIC, scale = 1)
        assertEquals("240 g: all-purpose flour", line)
    }

    @Test
    fun gramsViewConvertsCupsAndSpoonsWhenMetricNotStored() {
        val imported = flour.copy(quantityMetric = 0.0, unitMetric = "")
        assertEquals("2 cups: all-purpose flour", QuantityFormatter.ingredientLine(imported, UnitSystem.US, 1))
        assertEquals("~240 g: all-purpose flour", QuantityFormatter.ingredientLine(imported, UnitSystem.METRIC, 1))
        assertEquals("~480 g: all-purpose flour", QuantityFormatter.ingredientLine(imported, UnitSystem.METRIC, 2))

        val oil = flour.copy(
            name = "oil",
            quantityUs = 1.0,
            unitUs = "tbsp",
            quantityMetric = 0.0,
            unitMetric = "",
        )
        assertEquals("1 tbsp: oil", QuantityFormatter.ingredientLine(oil, UnitSystem.US, 1))
        assertEquals("~14 g: oil", QuantityFormatter.ingredientLine(oil, UnitSystem.METRIC, 1))
    }

    @Test
    fun gramsViewKeepsCountsAndUsesStoredGramsWithoutTilde() {
        val eggs = flour.copy(
            name = "eggs",
            quantityUs = 2.0,
            unitUs = "",
            quantityMetric = 0.0,
            unitMetric = "",
        )
        assertEquals("2: eggs", QuantityFormatter.ingredientLine(eggs, UnitSystem.METRIC, 1))

        assertEquals("240 g: all-purpose flour", QuantityFormatter.ingredientLine(flour, UnitSystem.METRIC, 1))
    }

    @Test
    fun batchScaleUsesStructuredQtyNotReparse() {
        val line = QuantityFormatter.ingredientLine(flour, UnitSystem.US, scale = 2)
        assertEquals("4 cups: all-purpose flour", line)
        val metric = QuantityFormatter.ingredientLine(flour, UnitSystem.METRIC, scale = 2)
        assertEquals("480 g: all-purpose flour", metric)
    }

    @Test
    fun minutesFormatHours() {
        assertEquals("35 min", QuantityFormatter.formatMinutes(35))
        assertEquals("1 hr", QuantityFormatter.formatMinutes(60))
        assertEquals("3 hr", QuantityFormatter.formatMinutes(180))
        assertEquals("", QuantityFormatter.formatMinutes(0))
    }

    @Test
    fun missingQuantityIsNotInvented() {
        val line = QuantityFormatter.ingredientLine(
            flour.copy(name = "a pinch of love", quantityUs = 0.0, unitUs = "", quantityMetric = 0.0, unitMetric = ""),
            UnitSystem.US,
            1,
        )
        assertEquals("a pinch of love", line)
    }

    @Test
    fun quantityWithoutUnitOmitsEmptyUnit() {
        val eggs = flour.copy(
            name = "eggs",
            quantityUs = 2.0,
            unitUs = "",
            quantityMetric = 0.0,
            unitMetric = "",
        )
        assertEquals("2: eggs", QuantityFormatter.ingredientLine(eggs, UnitSystem.US, 1))
        assertEquals("2: eggs", QuantityFormatter.ingredientLine(eggs, UnitSystem.METRIC, 1))
    }

    @Test
    fun sourceUnitIsKeptWhenPresent() {
        val eggs = flour.copy(
            name = "eggs",
            quantityUs = 2.0,
            unitUs = "large",
            quantityMetric = 0.0,
            unitMetric = "",
        )
        assertEquals("2 large: eggs", QuantityFormatter.ingredientLine(eggs, UnitSystem.US, 1))
    }

    @Test
    fun draftPreviewMatchesSavedLineShape() {
        val draft = DraftIngredient(
            name = "baking soda",
            quantityUs = 1.0,
            unitUs = "tsp",
            quantityMetric = 5.0,
            unitMetric = "g",
        )
        assertEquals("1 tsp: baking soda", QuantityFormatter.ingredientLine(draft, UnitSystem.US))
        assertEquals("5 g: baking soda", QuantityFormatter.ingredientLine(draft, UnitSystem.METRIC))
    }

    @Test
    fun metricOnlyFallsBackWhenUsEmpty() {
        val butter = flour.copy(
            name = "unsalted butter",
            quantityUs = 0.0,
            unitUs = "",
            quantityMetric = 226.0,
            unitMetric = "g",
        )
        assertEquals("226 g: unsalted butter", QuantityFormatter.ingredientLine(butter, UnitSystem.METRIC, 1))
        assertEquals("226 g: unsalted butter", QuantityFormatter.ingredientLine(butter, UnitSystem.US, 1))
    }
}
