package com.customrecipebook.app

import com.customrecipebook.app.data.Direction
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.Ingredient
import com.customrecipebook.app.data.Recipe
import com.customrecipebook.app.data.RecipeCategory
import com.customrecipebook.app.data.toDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeDraftMappingTest {
    @Test
    fun recipeToDraftKeepsEditableFields() {
        val recipe = Recipe(
            id = "recipe-1",
            title = "Tomato Soup",
            subtitle = "",
            category = RecipeCategory.DINNER,
            minutes = 40,
            baseServings = 6,
            servingsUnit = "servings",
            difficulty = "Easy",
            imageKey = null,
            imageUri = null,
            isSaved = true,
            source = ImportSource.PDF,
            createdAt = 1L,
            ingredients = listOf(
                Ingredient(
                    id = "i1",
                    recipeId = "recipe-1",
                    name = "tomatoes",
                    quantityUs = 2.0,
                    unitUs = "cups",
                    quantityMetric = 0.0,
                    unitMetric = "",
                    sortOrder = 0,
                ),
            ),
            directions = listOf(
                Direction(id = "d1", recipeId = "recipe-1", stepNumber = 1, text = "Simmer."),
            ),
        )
        val draft = recipe.toDraft()
        assertEquals("Tomato Soup", draft.title)
        assertEquals(RecipeCategory.DINNER, draft.category)
        assertEquals(40, draft.minutes)
        assertEquals(6, draft.baseServings)
        assertEquals("tomatoes", draft.ingredients.single().name)
        assertEquals(2.0, draft.ingredients.single().quantityUs, 0.01)
        assertEquals("cups", draft.ingredients.single().unitUs)
        assertEquals(listOf("Simmer."), draft.directions)
        assertEquals(ImportSource.PDF, draft.source)
    }
}
