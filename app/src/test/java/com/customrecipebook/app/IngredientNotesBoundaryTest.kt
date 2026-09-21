package com.customrecipebook.app

import com.customrecipebook.app.data.DraftIngredient
import com.customrecipebook.app.data.UnitSystem
import com.customrecipebook.app.domain.IngredientNotesBoundary
import com.customrecipebook.app.domain.QuantityFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientNotesBoundaryTest {
    private val flour = DraftIngredient("flour", 2.0, "cups", 0.0, "")
    private val salt = DraftIngredient("salt", 1.0, "tsp", 0.0, "")
    private val onion = DraftIngredient("onion (diced)", 1.0, "", 0.0, "")

    @Test
    fun defaultCutLeavesListsUnchanged() {
        val (ings, notes) = IngredientNotesBoundary.applyCut(
            listOf(flour, salt),
            "Dough keeps 3 days.",
            cutAfter = 2,
        )
        assertEquals(listOf("flour", "salt"), ings.map { it.name })
        assertEquals(2.0, ings[0].quantityUs, 0.01)
        assertEquals("Dough keeps 3 days.", notes)
    }

    @Test
    fun movingCutUpSendsLastIngredientsToNotes() {
        val (ings, notes) = IngredientNotesBoundary.applyCut(
            listOf(flour, salt, onion),
            "Keep chilled.",
            cutAfter = 2,
        )
        assertEquals(listOf("flour", "salt"), ings.map { it.name })
        assertEquals(
            listOf("1: onion (diced)", "Keep chilled."),
            notes.lines(),
        )
    }

    @Test
    fun movingCutDownPromotesNoteLinesToIngredients() {
        val (ings, notes) = IngredientNotesBoundary.applyCut(
            listOf(flour),
            "1 tsp salt\nDough keeps 3 days.",
            cutAfter = 2,
        )
        assertEquals(listOf("flour", "salt"), ings.map { it.name })
        assertEquals("tsp", ings[1].unitUs)
        assertEquals(1.0, ings[1].quantityUs, 0.01)
        assertEquals("Dough keeps 3 days.", notes)
    }

    @Test
    fun cutAtZeroSendsEverythingToNotes() {
        val (ings, notes) = IngredientNotesBoundary.applyCut(
            listOf(flour, salt),
            "",
            cutAfter = 0,
        )
        assertEquals(emptyList<DraftIngredient>(), ings)
        assertEquals(
            listOf(
                QuantityFormatter.ingredientLine(flour, UnitSystem.US),
                QuantityFormatter.ingredientLine(salt, UnitSystem.US),
            ).joinToString("\n"),
            notes,
        )
    }
}
