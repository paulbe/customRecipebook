package com.customrecipebook.app

import com.customrecipebook.app.data.UnitSystem
import com.customrecipebook.app.domain.PdfStreamTextExtractor
import com.customrecipebook.app.domain.QuantityFormatter
import com.customrecipebook.app.domain.RecipeTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTextParserTest {
    @Test
    fun titleFromFileNameCleansExtensionAndDashes() {
        assertEquals("Lemon Garlic Chicken", RecipeTextParser.titleFromFileName("lemon-garlic-chicken.pdf"))
        assertEquals("Imported recipe", RecipeTextParser.titleFromFileName(".pdf"))
    }

    @Test
    fun parseFillsSectionsFromHeaders() {
        val text = """
            Tomato Soup
            Ingredients
            2 cups vegetable broth
            1 tbsp olive oil
            Directions
            1. Warm the oil.
            2. Add broth and simmer.
        """.trimIndent()
        val parsed = RecipeTextParser.parse(text, "Fallback")
        assertEquals("Tomato Soup", parsed.title)
        assertEquals(2, parsed.ingredients.size)
        assertEquals("vegetable broth", parsed.ingredients[0].name)
        assertEquals(2.0, parsed.ingredients[0].quantityUs, 0.01)
        assertEquals("cups", parsed.ingredients[0].unitUs)
        assertEquals(listOf("Warm the oil.", "Add broth and simmer."), parsed.directions)
        assertTrue(parsed.extracted)
    }

    @Test
    fun gluedNumberedDirectionsBecomeSeparateSteps() {
        val parsed = RecipeTextParser.parse(
            """
            Soup
            Ingredients
            2 cups broth
            Directions
            1. Mix flour until combined. 2. Bake 12 min.
            """.trimIndent(),
            "Fallback",
        )
        assertEquals(listOf("Mix flour until combined.", "Bake 12 min."), parsed.directions)
        assertEquals("broth", parsed.ingredients.single().name)
    }

    @Test
    fun stepLabelsAndBareNumbersSplitAndStrip() {
        val parens = RecipeTextParser.parse(
            """
            Cake
            Ingredients
            1 cup sugar
            Directions
            1) Mix the batter 2) Bake until golden
            """.trimIndent(),
            "Fallback",
        )
        assertEquals(listOf("Mix the batter", "Bake until golden"), parens.directions)

        val named = RecipeTextParser.parse(
            """
            Cake
            Ingredients
            1 cup sugar
            Directions
            Step 1 Mix the batter
            Step 2 Bake until golden
            """.trimIndent(),
            "Fallback",
        )
        assertEquals(listOf("Mix the batter", "Bake until golden"), named.directions)

        val bare = RecipeTextParser.parse(
            """
            Cake
            Ingredients
            1 cup sugar
            Directions
            1 Mix the batter
            2 Bake until golden
            """.trimIndent(),
            "Fallback",
        )
        assertEquals(listOf("Mix the batter", "Bake until golden"), bare.directions)
    }

    @Test
    fun wrappedDirectionContinuesThePreviousStep() {
        val parsed = RecipeTextParser.parse(
            """
            Soup
            Ingredients
            2 cups broth
            Directions
            1. Mix flour and
            sugar together.
            2. Bake.
            """.trimIndent(),
            "Fallback",
        )
        assertEquals(listOf("Mix flour and sugar together.", "Bake."), parsed.directions)
    }

    @Test
    fun notesSectionIsNotFoldedIntoDirections() {
        val parsed = RecipeTextParser.parse(
            """
            Cookies
            Ingredients
            2 cups flour
            Directions
            1. Mix the dough.
            2. Bake 12 min.
            Notes
            Dough keeps 3 days in the fridge.
            Freeze baked cookies in a tin.
            """.trimIndent(),
            "Fallback",
        )
        assertEquals(listOf("Mix the dough.", "Bake 12 min."), parsed.directions)
        assertEquals(
            "Dough keeps 3 days in the fridge.\nFreeze baked cookies in a tin.",
            parsed.notes,
        )
        assertEquals("flour", parsed.ingredients.single().name)
        val draft = RecipeTextParser.toDraft(parsed, "cookies.pdf", null)
        assertEquals(parsed.notes, draft.notes)
        assertTrue(draft.directions.none { it.contains("keeps 3 days") })
    }

    @Test
    fun chefsNotesHeadingFillsNotesField() {
        val parsed = RecipeTextParser.parse(
            """
            Roast
            Ingredients
            1 lb beef
            Method
            1. Sear the meat.
            Chef's notes
            Rest 10 minutes before slicing.
            """.trimIndent(),
            "Fallback",
        )
        assertEquals(listOf("Sear the meat."), parsed.directions)
        assertEquals("Rest 10 minutes before slicing.", parsed.notes)
    }

    @Test
    fun gluedNotesAfterLastStepBecomesNotesField() {
        val parsed = RecipeTextParser.parse(
            "Cookies Ingredients 2 cups flour Directions 1. Mix the dough. 2. Bake 12 min. Notes Dough keeps 3 days in the fridge.",
            "Fallback",
        )
        assertEquals(listOf("Mix the dough.", "Bake 12 min."), parsed.directions)
        assertEquals("Dough keeps 3 days in the fridge.", parsed.notes)
    }

    @Test
    fun tipsHeadingIsNotesWhenItFollowsAFinishedStep() {
        val parsed = RecipeTextParser.parse(
            """
            Cookies
            Ingredients
            2 cups flour
            Directions
            1. Mix the dough.
            2. Bake 12 min.
            Tips
            Chill the dough overnight.
            """.trimIndent(),
            "Fallback",
        )
        assertEquals(listOf("Mix the dough.", "Bake 12 min."), parsed.directions)
        assertEquals("Chill the dough overnight.", parsed.notes)
    }

    @Test
    fun noteTheColorStaysADirection() {
        val parsed = RecipeTextParser.parse(
            """
            Cake
            Ingredients
            1 cup sugar
            Directions
            1. Note the color of the crust.
            2. Cool on a rack.
            """.trimIndent(),
            "Fallback",
        )
        assertEquals(listOf("Note the color of the crust.", "Cool on a rack."), parsed.directions)
        assertEquals("", parsed.notes)

        val colon = RecipeTextParser.parse(
            """
            Cake
            Ingredients
            1 cup sugar
            Directions
            1. Note: the color of the crust.
            2. Cool on a rack.
            """.trimIndent(),
            "Fallback",
        )
        assertEquals(listOf("Note: the color of the crust.", "Cool on a rack."), colon.directions)
        assertEquals("", colon.notes)
    }

    @Test
    fun parseIngredientLineDropsDualMetricWhenHouseholdUnitPresent() {
        val flour = RecipeTextParser.parseIngredientLine("2 cups (250g) flour")
        assertEquals("flour", flour.name)
        assertEquals(2.0, flour.quantityUs, 0.01)
        assertEquals("cups", flour.unitUs)
        assertEquals(0.0, flour.quantityMetric, 0.01)
        assertEquals("", flour.unitMetric)
        assertEquals(
            "2 cups: flour",
            QuantityFormatter.ingredientLine(flour, UnitSystem.US),
        )

        val oil = RecipeTextParser.parseIngredientLine("1 tbsp / 15 ml oil")
        assertEquals("oil", oil.name)
        assertEquals(1.0, oil.quantityUs, 0.01)
        assertEquals("tbsp", oil.unitUs)
        assertEquals(0.0, oil.quantityMetric, 0.01)
        assertEquals("", oil.unitMetric)
        assertEquals("1 tbsp: oil", QuantityFormatter.ingredientLine(oil, UnitSystem.US))

        val sugar = RecipeTextParser.parseIngredientLine("250g (1 cup) sugar")
        assertEquals("sugar", sugar.name)
        assertEquals(1.0, sugar.quantityUs, 0.01)
        assertEquals("cup", sugar.unitUs)
        assertEquals(0.0, sugar.quantityMetric, 0.01)
        assertEquals("", sugar.unitMetric)
    }

    @Test
    fun parseIngredientLineKeepsMetricOnly() {
        val flour = RecipeTextParser.parseIngredientLine("2 cups all-purpose flour")
        assertEquals("all-purpose flour", flour.name)
        assertEquals(2.0, flour.quantityUs, 0.01)
        assertEquals("cups", flour.unitUs)
        assertEquals(0.0, flour.quantityMetric, 0.01)
        assertEquals("", flour.unitMetric)

        val soda = RecipeTextParser.parseIngredientLine("1 tsp baking soda")
        assertEquals("baking soda", soda.name)
        assertEquals(1.0, soda.quantityUs, 0.01)
        assertEquals("tsp", soda.unitUs)

        val butter = RecipeTextParser.parseIngredientLine("226 g unsalted butter")
        assertEquals("unsalted butter", butter.name)
        assertEquals(226.0, butter.quantityMetric, 0.01)
        assertEquals("g", butter.unitMetric)
        assertEquals(0.0, butter.quantityUs, 0.01)
        assertEquals("", butter.unitUs)
    }

    @Test
    fun parseIngredientLineDoesNotInventUnitOrQty() {
        val eggs = RecipeTextParser.parseIngredientLine("2 eggs")
        assertEquals("eggs", eggs.name)
        assertEquals(2.0, eggs.quantityUs, 0.01)
        assertEquals("", eggs.unitUs)

        val large = RecipeTextParser.parseIngredientLine("2 large eggs")
        assertEquals("eggs", large.name)
        assertEquals(2.0, large.quantityUs, 0.01)
        assertEquals("large", large.unitUs)

        val pinch = RecipeTextParser.parseIngredientLine("a pinch of love")
        assertEquals("a pinch of love", pinch.name)
        assertEquals(0.0, pinch.quantityUs, 0.01)
        assertEquals("", pinch.unitUs)
    }

    @Test
    fun parseIngredientLineReadsFormattedShape() {
        val colon = RecipeTextParser.parseIngredientLine("2 cups: all-purpose flour")
        assertEquals("all-purpose flour", colon.name)
        assertEquals(2.0, colon.quantityUs, 0.01)
        assertEquals("cups", colon.unitUs)

        val flour = RecipeTextParser.parseIngredientLine("2: cups all-purpose flour")
        assertEquals("all-purpose flour", flour.name)
        assertEquals(2.0, flour.quantityUs, 0.01)
        assertEquals("cups", flour.unitUs)

        val eggs = RecipeTextParser.parseIngredientLine("2: eggs")
        assertEquals("eggs", eggs.name)
        assertEquals(2.0, eggs.quantityUs, 0.01)
        assertEquals("", eggs.unitUs)

        val legacy = RecipeTextParser.parseIngredientLine("2: (cups) (all-purpose flour)")
        assertEquals("all-purpose flour", legacy.name)
        assertEquals(2.0, legacy.quantityUs, 0.01)
        assertEquals("cups", legacy.unitUs)
    }

    @Test
    fun emptyTextUsesFallbackAndIsNotExtracted() {
        val parsed = RecipeTextParser.parse("   \n  ", "From Filename")
        assertEquals("From Filename", parsed.title)
        assertFalse(parsed.extracted)
        assertTrue(parsed.ingredients.isEmpty())
    }

    @Test
    fun extractorReadsUncompressedTjText() {
        val pdf = """
            %PDF-1.1
            1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
            2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
            3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] /Contents 4 0 R >> endobj
            4 0 obj << /Length 48 >> stream
            BT /F1 12 Tf 10 100 Td (Chocolate Cake) Tj ET
            endstream
            endobj
            trailer << /Root 1 0 R >>
            %%EOF
        """.trimIndent().toByteArray(Charsets.ISO_8859_1)
        val text = PdfStreamTextExtractor.extract(pdf)
        assertTrue(text.contains("Chocolate Cake"))
    }
}
