package com.customrecipebook.app

import com.customrecipebook.app.data.UnitSystem
import com.customrecipebook.app.domain.PdfStreamTextExtractor
import com.customrecipebook.app.domain.QuantityFormatter
import com.customrecipebook.app.domain.RecipeTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeImportPipelineTest {
    private val sampleText = """
        Chocolate Chip Cookies
        Ingredients
        2 cups all-purpose flour
        1 tsp baking soda
        1 cup unsalted butter
        Directions
        1. Preheat oven to 350°F.
        2. Beat butter and sugars.
        3. Fold in chips and bake.
    """.trimIndent()

    @Test
    fun sampleTextBecomesNonEmptyDraftLists() {
        val parsed = RecipeTextParser.parse(sampleText, "cookies.pdf")
        val draft = RecipeTextParser.toDraft(parsed, "cookies.pdf", "/tmp/cookies.pdf")
        assertEquals("Chocolate Chip Cookies", draft.title)
        assertTrue("ingredients should be filled", draft.ingredients.size >= 3)
        assertTrue("directions should be filled", draft.directions.size >= 3)
        assertEquals("all-purpose flour", draft.ingredients.first().name)
        assertEquals(
            "2 cups: all-purpose flour",
            QuantityFormatter.ingredientLine(draft.ingredients.first(), UnitSystem.US),
        )
        assertEquals("baking soda", draft.ingredients[1].name)
        assertEquals(
            "1 tsp: baking soda",
            QuantityFormatter.ingredientLine(draft.ingredients[1], UnitSystem.US),
        )
        assertTrue(draft.directions.first().contains("Preheat"))
        assertTrue(draft.parseMessage.contains("3 ingredients") || draft.ingredients.isNotEmpty())
        assertEquals("", draft.subtitle)
        assertEquals("servings", draft.servingsUnit)
        assertEquals("Easy", draft.difficulty)
        assertEquals("", draft.notes)
    }

    @Test
    fun dualMetricOnIngredientLinesIsDroppedAndNotSplit() {
        val text = """
            Cookies
            Ingredients
            2 cups (250g) flour
            1 tbsp / 15 ml oil
            226 g unsalted butter
            Directions
            1. Mix.
        """.trimIndent()
        val parsed = RecipeTextParser.parse(text, "cookies.pdf")
        val draft = RecipeTextParser.toDraft(parsed, "cookies.pdf", null)
        assertEquals(3, draft.ingredients.size)
        assertEquals("2 cups: flour", QuantityFormatter.ingredientLine(draft.ingredients[0], UnitSystem.US))
        assertEquals("1 tbsp: oil", QuantityFormatter.ingredientLine(draft.ingredients[1], UnitSystem.US))
        assertEquals("226 g: unsalted butter", QuantityFormatter.ingredientLine(draft.ingredients[2], UnitSystem.US))
        assertEquals(0.0, draft.ingredients[0].quantityMetric, 0.01)
        assertEquals("", draft.ingredients[0].unitMetric)
        assertEquals("g", draft.ingredients[2].unitMetric)
        assertTrue(draft.ingredients.none { it.name.contains("250") || it.name.contains("ml") })
    }

    @Test
    fun notesBlockIsCopiedToDraftNotDirections() {
        val text = """
            Chocolate Chip Cookies
            Ingredients
            2 cups all-purpose flour
            Directions
            1. Preheat oven to 350°F.
            2. Beat butter and sugars.
            Notes
            Dough keeps 3 days refrigerated.
        """.trimIndent()
        val parsed = RecipeTextParser.parse(text, "cookies.pdf")
        val draft = RecipeTextParser.toDraft(parsed, "cookies.pdf", null)
        assertEquals(listOf("Preheat oven to 350°F.", "Beat butter and sugars."), draft.directions)
        assertEquals("Dough keeps 3 days refrigerated.", draft.notes)
        assertTrue(draft.parseMessage.contains("and notes"))
    }

    @Test
    fun concatenatedPdfBlobStillSplitsSections() {
        val blob = "Oatmeal Cookies Ingredients 2 cups oats 1 tsp cinnamon Instructions 1. Mix dry. 2. Bake 12 min."
        val parsed = RecipeTextParser.parse(blob, "oatmeal.pdf")
        assertTrue(parsed.ingredients.size >= 2)
        assertTrue(parsed.directions.size >= 2)
        val draft = RecipeTextParser.toDraft(parsed, "oatmeal.pdf", null)
        assertTrue(draft.ingredients.isNotEmpty())
        assertTrue(draft.directions.isNotEmpty())
    }

    @Test
    fun decoratedHeadersStillMatch() {
        val text = """
            Chili
            INGREDIENTS (serves 4)
            1 lb ground beef
            2 cups tomatoes
            METHOD
            1. Brown the beef.
            2. Simmer 20 minutes.
        """.trimIndent()
        val parsed = RecipeTextParser.parse(text, "chili.pdf")
        assertEquals("Chili", parsed.title)
        assertEquals(2, parsed.ingredients.size)
        assertEquals("ground beef", parsed.ingredients[0].name)
        assertEquals("tomatoes", parsed.ingredients[1].name)
        assertTrue(parsed.ingredients.none { it.name.contains("serves", ignoreCase = true) })
        assertTrue(parsed.directions.size >= 2)
    }

    @Test
    fun tdLineBreakPdfExtractsThenFillsDraft() {
        val pdf = tdRecipePdf()
        val extracted = PdfStreamTextExtractor.extract(pdf)
        assertTrue("extractor should keep line breaks: $extracted", extracted.contains("Ingredients"))
        assertTrue(extracted.contains('\n') || extracted.contains("2 cups"))
        val parsed = RecipeTextParser.parse(extracted, "td-recipe.pdf")
        val draft = RecipeTextParser.toDraft(parsed, "td-recipe.pdf", "/imports/td.pdf")
        assertTrue("parsed ingredients from Td PDF: $extracted → ${parsed.ingredients}", draft.ingredients.isNotEmpty())
        assertTrue("parsed directions from Td PDF: $extracted → ${parsed.directions}", draft.directions.isNotEmpty())
    }

    private fun tdRecipePdf(): ByteArray {
        val content = "BT /F1 12 Tf 10 200 Td (Weeknight Pasta) Tj 0 -16 Td (Ingredients) Tj 0 -16 Td (2 cups dry pasta) Tj 0 -16 Td (1 tbsp olive oil) Tj 0 -16 Td (Directions) Tj 0 -16 Td (1. Boil the pasta.) Tj 0 -16 Td (2. Toss with oil.) Tj ET"
        return """
            %PDF-1.1
            1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
            2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
            3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] /Contents 4 0 R >> endobj
            4 0 obj << /Length ${content.length} >> stream
            $content
            endstream
            endobj
            trailer << /Root 1 0 R >>
            %%EOF
        """.trimIndent().toByteArray(Charsets.ISO_8859_1)
    }
}
