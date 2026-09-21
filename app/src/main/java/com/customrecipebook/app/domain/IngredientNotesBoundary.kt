package com.customrecipebook.app.domain

import com.customrecipebook.app.data.DraftIngredient
import com.customrecipebook.app.data.UnitSystem

object IngredientNotesBoundary {
    fun noteLines(notes: String): List<String> =
        notes.lines().map { it.trim() }.filter { it.isNotBlank() }

    fun ingredientLineText(item: DraftIngredient): String {
        val formatted = QuantityFormatter.ingredientLine(item, UnitSystem.US).trim()
        return formatted.ifBlank { item.name.trim() }
    }

    fun combinedLineCount(ingredients: List<DraftIngredient>, notes: String): Int =
        ingredients.size + noteLines(notes).size

    fun applyCut(
        ingredients: List<DraftIngredient>,
        notes: String,
        cutAfter: Int,
    ): Pair<List<DraftIngredient>, String> {
        val noteParts = noteLines(notes)
        val total = ingredients.size + noteParts.size
        val cut = cutAfter.coerceIn(0, total)
        val keptIngredients = ingredients.take(cut.coerceAtMost(ingredients.size))
        val promoted = if (cut > ingredients.size) {
            noteParts.take(cut - ingredients.size).map { RecipeTextParser.parseIngredientLine(it) }
        } else {
            emptyList()
        }
        val demoted = if (cut < ingredients.size) {
            ingredients.drop(cut).map(::ingredientLineText)
        } else {
            emptyList()
        }
        val remainingNotes = noteParts.drop((cut - ingredients.size).coerceAtLeast(0))
        val newNotes = (demoted + remainingNotes).joinToString("\n")
        return (keptIngredients + promoted) to newNotes
    }
}
