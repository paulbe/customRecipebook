package com.customrecipebook.app

import com.customrecipebook.app.domain.PdfStreamTextExtractor
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
    fun parseIngredientLineSplitsQtyUnitName() {
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
