package com.customrecipebook.app.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.customrecipebook.app.data.Direction
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.Ingredient
import com.customrecipebook.app.data.Recipe
import com.customrecipebook.app.data.RecipeCategory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val minutes: Int,
    val baseServings: Int,
    val servingsUnit: String,
    val difficulty: String,
    val imageKey: String?,
    val imageUri: String?,
    val attachmentUri: String?,
    val attachmentName: String?,
    val notes: String = "",
    val isSaved: Boolean,
    val source: String,
    val createdAt: Long,
)

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val name: String,
    val quantityUs: Double,
    val unitUs: String,
    val quantityMetric: Double,
    val unitMetric: String,
    val metricApprox: Boolean,
    val sortOrder: Int,
    val isChecked: Boolean,
)

@Entity(tableName = "directions")
data class DirectionEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val stepNumber: Int,
    val text: String,
)

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipe(id: String): Flow<RecipeEntity?>

    @Query("SELECT * FROM ingredients ORDER BY sortOrder")
    fun observeAllIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE recipeId = :recipeId ORDER BY sortOrder")
    fun observeIngredients(recipeId: String): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM directions WHERE recipeId = :recipeId ORDER BY stepNumber")
    fun observeDirections(recipeId: String): Flow<List<DirectionEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipe(id: String): RecipeEntity?

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun recipeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirections(directions: List<DirectionEntity>)

    @Query("UPDATE ingredients SET isChecked = :checked WHERE id = :id")
    suspend fun setIngredientChecked(id: String, checked: Boolean)

    @Query("UPDATE recipes SET isSaved = :saved WHERE id = :id")
    suspend fun setSaved(id: String, saved: Boolean)

    @Query("UPDATE ingredients SET isChecked = 0")
    suspend fun clearAllChecks()

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: String)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: String)

    @Query("DELETE FROM directions WHERE recipeId = :recipeId")
    suspend fun deleteDirections(recipeId: String)

    @Transaction
    suspend fun replaceFullRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        directions: List<DirectionEntity>,
    ) {
        insertRecipes(listOf(recipe))
        deleteIngredients(recipe.id)
        deleteDirections(recipe.id)
        insertIngredients(ingredients)
        insertDirections(directions)
    }

    @Transaction
    suspend fun insertFullRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        directions: List<DirectionEntity>,
    ) {
        insertRecipes(listOf(recipe))
        insertIngredients(ingredients)
        insertDirections(directions)
    }
}

@Database(
    entities = [RecipeEntity::class, IngredientEntity::class, DirectionEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
}

fun RecipeEntity.toModel(
    ingredients: List<Ingredient> = emptyList(),
    directions: List<Direction> = emptyList(),
): Recipe = Recipe(
    id = id,
    title = title,
    subtitle = subtitle,
    category = runCatching { RecipeCategory.valueOf(category) }.getOrDefault(RecipeCategory.DINNER),
    minutes = minutes,
    baseServings = baseServings,
    servingsUnit = servingsUnit,
    difficulty = difficulty,
    imageKey = imageKey,
    imageUri = imageUri,
    attachmentUri = attachmentUri,
    attachmentName = attachmentName,
    notes = notes,
    isSaved = isSaved,
    source = runCatching { ImportSource.valueOf(source) }.getOrDefault(ImportSource.MANUAL),
    createdAt = createdAt,
    ingredients = ingredients,
    directions = directions,
)

fun Recipe.toEntity(): RecipeEntity = RecipeEntity(
    id = id,
    title = title,
    subtitle = subtitle,
    category = category.name,
    minutes = minutes,
    baseServings = baseServings,
    servingsUnit = servingsUnit,
    difficulty = difficulty,
    imageKey = imageKey,
    imageUri = imageUri,
    attachmentUri = attachmentUri,
    attachmentName = attachmentName,
    notes = notes,
    isSaved = isSaved,
    source = source.name,
    createdAt = createdAt,
)

fun IngredientEntity.toModel(): Ingredient = Ingredient(
    id = id,
    recipeId = recipeId,
    name = name,
    quantityUs = quantityUs,
    unitUs = unitUs,
    quantityMetric = quantityMetric,
    unitMetric = unitMetric,
    metricApprox = metricApprox,
    sortOrder = sortOrder,
    isChecked = isChecked,
)

fun DirectionEntity.toModel(): Direction = Direction(
    id = id,
    recipeId = recipeId,
    stepNumber = stepNumber,
    text = text,
)
