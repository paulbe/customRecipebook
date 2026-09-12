package com.customrecipebook.app.data.repo

import com.customrecipebook.app.data.Direction
import com.customrecipebook.app.data.DraftIngredient
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.Ingredient
import com.customrecipebook.app.data.Recipe
import com.customrecipebook.app.data.RecipeCategory
import com.customrecipebook.app.data.RecipeDraft

object SeedData {
    private const val COOKIES = "recipe-cookies"
    private const val CHICKEN = "recipe-lemon-chicken"
    private const val PASTA = "recipe-garlic-pasta"
    private const val BREAD = "recipe-seeded-bread"
    private const val MUFFINS = "recipe-banana-muffins"

    fun recipes(): List<Recipe> = listOf(
        Recipe(
            id = COOKIES,
            title = "Chocolate Chip Cookies",
            subtitle = "Chewy centers · crisp edges",
            category = RecipeCategory.BAKING,
            minutes = 60,
            baseServings = 12,
            servingsUnit = "cookies",
            difficulty = "Easy",
            imageKey = "cookies",
            imageUri = null,
            isSaved = true,
            source = ImportSource.PDF,
            createdAt = 1_700_000_000_000,
            ingredients = listOf(
                ing(COOKIES, 0, "all-purpose flour", 2.0, "cups", 240.0, "g"),
                ing(COOKIES, 1, "baking soda", 1.0, "tsp", 6.0, "g"),
                ing(COOKIES, 2, "salt", 1.0, "tsp", 6.0, "g"),
                ing(COOKIES, 3, "unsalted butter", 1.0, "cup", 226.0, "g"),
                ing(COOKIES, 4, "granulated sugar", 1.0, "cup", 200.0, "g"),
                ing(COOKIES, 5, "packed brown sugar", 1.0, "cup", 200.0, "g"),
                ing(COOKIES, 6, "eggs", 2.0, "large", 100.0, "g", approx = true),
                ing(COOKIES, 7, "vanilla", 2.0, "tsp", 10.0, "ml"),
                ing(COOKIES, 8, "chocolate chips", 2.0, "cups", 340.0, "g"),
            ),
            directions = dirs(
                COOKIES,
                "Preheat oven to 350°F (175°C). Line sheets with parchment.",
                "Whisk flour, baking soda, and salt in a medium bowl.",
                "Beat butter and both sugars until light and fluffy.",
                "Add eggs one at a time, then vanilla. Fold in dry mix and chips.",
                "Scoop onto sheets. Bake 10–12 minutes until golden at the edges.",
            ),
        ),
        Recipe(
            id = CHICKEN,
            title = "One-Pan Lemon Chicken",
            subtitle = "Bright, garlicky, weeknight-easy",
            category = RecipeCategory.DINNER,
            minutes = 35,
            baseServings = 4,
            servingsUnit = "servings",
            difficulty = "Easy",
            imageKey = "chicken",
            imageUri = null,
            isSaved = true,
            source = ImportSource.SEEDED,
            createdAt = 1_700_000_100_000,
            ingredients = listOf(
                ing(CHICKEN, 0, "boneless chicken thighs", 1.5, "lb", 680.0, "g"),
                ing(CHICKEN, 1, "olive oil", 2.0, "tbsp", 30.0, "ml"),
                ing(CHICKEN, 2, "garlic", 3.0, "cloves", 9.0, "g"),
                ing(CHICKEN, 3, "lemon juice", 0.25, "cup", 60.0, "ml"),
                ing(CHICKEN, 4, "dried oregano", 1.0, "tsp", 2.0, "g"),
                ing(CHICKEN, 5, "cherry tomatoes", 1.0, "cup", 150.0, "g"),
                ing(CHICKEN, 6, "chicken broth", 0.5, "cup", 120.0, "ml"),
                ing(CHICKEN, 7, "kosher salt", 1.0, "tsp", 6.0, "g"),
            ),
            directions = dirs(
                CHICKEN,
                "Pat chicken dry and season with salt, pepper, and oregano.",
                "Sear in olive oil until golden, about 4 minutes per side.",
                "Add garlic, lemon juice, tomatoes, and broth. Simmer 12 minutes.",
                "Spoon pan juices over the chicken and serve with bread or rice.",
            ),
        ),
        Recipe(
            id = PASTA,
            title = "Garlic & Herb Pasta",
            subtitle = "Butter, parsley, and a little heat",
            category = RecipeCategory.DINNER,
            minutes = 25,
            baseServings = 2,
            servingsUnit = "servings",
            difficulty = "Easy",
            imageKey = "pasta",
            imageUri = null,
            isSaved = false,
            source = ImportSource.SEEDED,
            createdAt = 1_700_000_200_000,
            ingredients = listOf(
                ing(PASTA, 0, "spaghetti", 8.0, "oz", 225.0, "g"),
                ing(PASTA, 1, "unsalted butter", 3.0, "tbsp", 42.0, "g"),
                ing(PASTA, 2, "garlic", 4.0, "cloves", 12.0, "g"),
                ing(PASTA, 3, "chopped parsley", 0.25, "cup", 10.0, "g"),
                ing(PASTA, 4, "grated Parmesan", 0.5, "cup", 45.0, "g"),
                ing(PASTA, 5, "olive oil", 2.0, "tbsp", 30.0, "ml"),
                ing(PASTA, 6, "red pepper flakes", 0.25, "tsp", 0.5, "g"),
            ),
            directions = dirs(
                PASTA,
                "Boil pasta in salted water until just shy of al dente.",
                "Warm butter and oil; cook garlic and pepper flakes 1 minute.",
                "Toss pasta with a splash of pasta water, herbs, and Parmesan.",
                "Taste for salt and serve immediately.",
            ),
        ),
        Recipe(
            id = BREAD,
            title = "Simple Seeded Bread",
            subtitle = "A no-knead loaf with a crackly crust",
            category = RecipeCategory.BAKING,
            minutes = 180,
            baseServings = 1,
            servingsUnit = "loaf",
            difficulty = "Medium",
            imageKey = "bread",
            imageUri = null,
            isSaved = false,
            source = ImportSource.SEEDED,
            createdAt = 1_700_000_300_000,
            ingredients = listOf(
                ing(BREAD, 0, "bread flour", 3.0, "cups", 360.0, "g"),
                ing(BREAD, 1, "kosher salt", 1.5, "tsp", 9.0, "g"),
                ing(BREAD, 2, "instant yeast", 1.0, "tsp", 3.0, "g"),
                ing(BREAD, 3, "warm water", 1.25, "cups", 300.0, "ml"),
                ing(BREAD, 4, "mixed seeds", 2.0, "tbsp", 18.0, "g"),
                ing(BREAD, 5, "honey", 1.0, "tbsp", 21.0, "g"),
            ),
            directions = dirs(
                BREAD,
                "Stir flour, salt, yeast, honey, and water until a shaggy dough forms.",
                "Cover and rest 90 minutes, until puffy.",
                "Shape into a round, roll the top in seeds, and proof 30 minutes.",
                "Bake at 450°F (230°C) for 30–35 minutes until deeply browned.",
            ),
        ),
        Recipe(
            id = MUFFINS,
            title = "Morning Banana Muffins",
            subtitle = "Ripe bananas, one bowl, breakfast ready",
            category = RecipeCategory.BAKING,
            minutes = 40,
            baseServings = 12,
            servingsUnit = "muffins",
            difficulty = "Easy",
            imageKey = "muffins",
            imageUri = null,
            isSaved = false,
            source = ImportSource.SEEDED,
            createdAt = 1_700_000_400_000,
            ingredients = listOf(
                ing(MUFFINS, 0, "ripe bananas, mashed", 3.0, "large", 350.0, "g", approx = true),
                ing(MUFFINS, 1, "all-purpose flour", 1.5, "cups", 180.0, "g"),
                ing(MUFFINS, 2, "sugar", 0.5, "cup", 100.0, "g"),
                ing(MUFFINS, 3, "melted butter", 0.33, "cup", 80.0, "ml"),
                ing(MUFFINS, 4, "egg", 1.0, "large", 50.0, "g", approx = true),
                ing(MUFFINS, 5, "baking soda", 1.0, "tsp", 5.0, "g"),
                ing(MUFFINS, 6, "vanilla", 1.0, "tsp", 5.0, "ml"),
                ing(MUFFINS, 7, "salt", 0.25, "tsp", 1.5, "g"),
            ),
            directions = dirs(
                MUFFINS,
                "Heat oven to 350°F (175°C). Line a 12-cup muffin tin.",
                "Stir bananas, melted butter, sugar, egg, and vanilla.",
                "Fold in flour, baking soda, and salt until just combined.",
                "Divide and bake 18–22 minutes, until a toothpick comes out clean.",
            ),
        ),
    )

    fun creamyWhiteBeanSoup(): RecipeDraft = RecipeDraft(
        title = "Creamy White Bean Soup",
        subtitle = "Olive oil · onion · carrots · beans",
        category = RecipeCategory.DINNER,
        minutes = 40,
        baseServings = 4,
        servingsUnit = "servings",
        difficulty = "Easy",
        source = ImportSource.PDF,
        ingredients = listOf(
            DraftIngredient("olive oil", 2.0, "tbsp", 30.0, "ml"),
            DraftIngredient("yellow onion, chopped", 1.0, "large", 220.0, "g", metricApprox = true),
            DraftIngredient("carrots, diced", 2.0, "medium", 160.0, "g", metricApprox = true),
            DraftIngredient("garlic", 3.0, "cloves", 9.0, "g"),
            DraftIngredient("cannellini beans, drained", 2.0, "cans", 800.0, "g"),
            DraftIngredient("vegetable broth", 3.0, "cups", 720.0, "ml"),
            DraftIngredient("fresh rosemary", 1.0, "tsp", 1.0, "g"),
        ),
        directions = listOf(
            "Sweat onion and carrots in olive oil until soft.",
            "Add garlic and rosemary; cook 1 minute.",
            "Stir in beans and broth. Simmer 15 minutes.",
            "Blend half the soup, return to the pot, and season.",
        ),
        titleConfidence = 92,
        ingredientsConfidence = 88,
        instructionsConfidence = 75,
    )

    private fun ing(
        recipeId: String,
        order: Int,
        name: String,
        qtyUs: Double,
        unitUs: String,
        qtyMetric: Double,
        unitMetric: String,
        approx: Boolean = false,
    ) = Ingredient(
        id = "$recipeId-ing-$order",
        recipeId = recipeId,
        name = name,
        quantityUs = qtyUs,
        unitUs = unitUs,
        quantityMetric = qtyMetric,
        unitMetric = unitMetric,
        metricApprox = approx,
        sortOrder = order,
        isChecked = false,
    )

    private fun dirs(recipeId: String, vararg steps: String): List<Direction> =
        steps.mapIndexed { index, text ->
            Direction(
                id = "$recipeId-dir-$index",
                recipeId = recipeId,
                stepNumber = index + 1,
                text = text,
            )
        }
}
