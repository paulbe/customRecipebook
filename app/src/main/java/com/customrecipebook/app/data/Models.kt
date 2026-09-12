package com.customrecipebook.app.data

enum class RecipeCategory(val label: String) {
    ALL("All"),
    DINNER("Dinner"),
    BAKING("Baking"),
    SAVED("Saved"),
}

enum class UnitSystem { US, METRIC }

enum class ImportSource { SEEDED, PDF, CAMERA, MANUAL }

data class Ingredient(
    val id: String,
    val recipeId: String,
    val name: String,
    val quantityUs: Double,
    val unitUs: String,
    val quantityMetric: Double,
    val unitMetric: String,
    val metricApprox: Boolean = false,
    val sortOrder: Int,
    val isChecked: Boolean = false,
)

data class Direction(
    val id: String,
    val recipeId: String,
    val stepNumber: Int,
    val text: String,
)

data class Recipe(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: RecipeCategory,
    val minutes: Int,
    val baseServings: Int,
    val servingsUnit: String,
    val difficulty: String,
    val imageKey: String?,
    val imageUri: String?,
    val isSaved: Boolean,
    val source: ImportSource,
    val createdAt: Long,
    val ingredients: List<Ingredient> = emptyList(),
    val directions: List<Direction> = emptyList(),
) {
    fun yieldLabel(scale: Int = 1): String {
        val amount = baseServings * scale
        return "$amount $servingsUnit"
    }
}

data class RecipeDraft(
    val title: String = "",
    val subtitle: String = "",
    val category: RecipeCategory = RecipeCategory.DINNER,
    val minutes: Int = 30,
    val baseServings: Int = 4,
    val servingsUnit: String = "servings",
    val difficulty: String = "Easy",
    val source: ImportSource = ImportSource.MANUAL,
    val imageUri: String? = null,
    val ingredients: List<DraftIngredient> = emptyList(),
    val directions: List<String> = emptyList(),
    val titleConfidence: Int = 0,
    val ingredientsConfidence: Int = 0,
    val instructionsConfidence: Int = 0,
)

data class DraftIngredient(
    val name: String,
    val quantityUs: Double,
    val unitUs: String,
    val quantityMetric: Double,
    val unitMetric: String,
    val metricApprox: Boolean = false,
)
