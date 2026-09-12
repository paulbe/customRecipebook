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
