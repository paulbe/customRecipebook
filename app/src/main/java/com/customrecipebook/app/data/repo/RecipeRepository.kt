package com.customrecipebook.app.data.repo

import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.Recipe
import com.customrecipebook.app.data.RecipeCategory
import com.customrecipebook.app.data.RecipeDraft
import com.customrecipebook.app.data.db.AppDatabase
import com.customrecipebook.app.data.db.DirectionEntity
import com.customrecipebook.app.data.db.IngredientEntity
import com.customrecipebook.app.data.db.RecipeEntity
import com.customrecipebook.app.data.db.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class RecipeRepository(private val db: AppDatabase) {
    private val dao = db.recipeDao()

    val recipes: Flow<List<Recipe>> = combine(
        dao.observeRecipes(),
        dao.observeAllIngredients(),
    ) { recipes, ingredients ->
        val byRecipe = ingredients.groupBy { it.recipeId }
        recipes.map { entity ->
            entity.toModel(ingredients = byRecipe[entity.id].orEmpty().map { it.toModel() })
        }
    }

    fun observeRecipe(id: String): Flow<Recipe?> = combine(
        dao.observeRecipe(id),
        dao.observeIngredients(id),
        dao.observeDirections(id),
    ) { recipe, ingredients, directions ->
        recipe?.toModel(
            ingredients = ingredients.map { it.toModel() },
            directions = directions.map { it.toModel() },
        )
    }

    val checkedIngredients: Flow<List<Recipe>> = recipes.map { list ->
        list.map { recipe ->
            recipe.copy(ingredients = recipe.ingredients.filter { it.isChecked })
        }.filter { it.ingredients.isNotEmpty() }
    }

    suspend fun setIngredientChecked(id: String, checked: Boolean) {
        dao.setIngredientChecked(id, checked)
    }

    suspend fun setSaved(id: String, saved: Boolean) {
        dao.setSaved(id, saved)
    }

    suspend fun clearChecklists() {
        dao.clearAllChecks()
    }

    suspend fun saveDraft(draft: RecipeDraft, existingId: String? = null): String {
        val existing = existingId?.let { dao.getRecipe(it) }
        val id = existing?.id ?: "recipe-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val recipe = RecipeEntity(
            id = id,
            title = draft.title.ifBlank { "Untitled recipe" },
            subtitle = draft.subtitle,
            category = draft.category.name,
            minutes = draft.minutes.coerceAtLeast(1),
            baseServings = draft.baseServings.coerceAtLeast(1),
            servingsUnit = draft.servingsUnit.ifBlank { "servings" },
            difficulty = draft.difficulty.ifBlank { "Easy" },
            imageKey = existing?.imageKey,
            imageUri = draft.imageUri ?: existing?.imageUri,
            attachmentUri = draft.attachmentUri ?: existing?.attachmentUri,
            attachmentName = draft.attachmentName ?: existing?.attachmentName,
            notes = draft.notes,
            isSaved = existing?.isSaved ?: false,
            source = draft.source.name,
            createdAt = existing?.createdAt ?: now,
        )
        val ingredients = draft.ingredients.mapIndexed { index, item ->
            IngredientEntity(
                id = "$id-ing-$index",
                recipeId = id,
                name = item.name,
                quantityUs = item.quantityUs,
                unitUs = item.unitUs,
                quantityMetric = item.quantityMetric,
                unitMetric = item.unitMetric,
                metricApprox = item.metricApprox,
                sortOrder = index,
                isChecked = false,
            )
        }
        val directions = draft.directions.filter { it.isNotBlank() }.mapIndexed { index, text ->
            DirectionEntity(
                id = "$id-dir-$index",
                recipeId = id,
                stepNumber = index + 1,
                text = text,
            )
        }
        if (existing != null) {
            dao.replaceFullRecipe(recipe, ingredients, directions)
        } else {
            dao.insertFullRecipe(recipe, ingredients, directions)
        }
        return id
    }

    fun filter(recipes: List<Recipe>, category: RecipeCategory): List<Recipe> = when (category) {
        RecipeCategory.ALL -> recipes
        RecipeCategory.SAVED -> recipes.filter { it.isSaved }
        RecipeCategory.DINNER, RecipeCategory.BAKING -> recipes.filter { it.category == category }
    }

    companion object {
        val supportedCategories = listOf(RecipeCategory.DINNER, RecipeCategory.BAKING)
    }
}

fun ImportSource.label(): String = when (this) {
    ImportSource.PDF -> "From PDF"
    ImportSource.CAMERA -> "From camera"
    ImportSource.MANUAL -> "Handwritten"
    ImportSource.SEEDED -> "Cookbook"
}
