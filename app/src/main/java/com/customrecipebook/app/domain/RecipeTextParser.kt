package com.customrecipebook.app.domain

import com.customrecipebook.app.data.DraftIngredient

data class ParsedRecipeText(
    val title: String,
    val subtitle: String,
    val ingredients: List<DraftIngredient>,
    val directions: List<String>,
    val extracted: Boolean,
)

object RecipeTextParser {
    private val ingredientHeaders = setOf(
        "ingredients", "ingredient", "you will need", "shopping list", "what you need",
    )
    private val directionHeaders = setOf(
        "directions", "direction", "instructions", "instruction",
        "method", "steps", "preparation", "prepare", "to make",
    )
    private val units = listOf(
        "tablespoons", "tablespoon", "teaspoons", "teaspoon",
        "tbsp", "tsp", "cups", "cup", "ounces", "ounce", "oz",
        "pounds", "pound", "lbs", "lb", "grams", "gram", "kg", "g",
        "milliliters", "millilitre", "ml", "liters", "litre", "l",
        "cloves", "clove", "large", "medium", "small", "pinch", "dash",
    )
    private val unitPattern = units.joinToString("|") { Regex.escape(it) }
    private val qtyPattern = Regex(
        """^(?:(\d+\s+\d+/\d+|\d+/\d+|\d+(?:\.\d+)?|[¼½¾⅓⅔]))\s*(?:($unitPattern))?\b[:\s]*(.*)$""",
        RegexOption.IGNORE_CASE,
    )
    private val numbered = Regex("""^\d+[\.)]\s+(.*)$""")

    fun titleFromFileName(name: String): String {
        val base = name.substringAfterLast('/')
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (base.isBlank()) return "Imported recipe"
        return base.split(' ').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.titlecase() }
        }
    }

    fun parse(raw: String, fallbackTitle: String): ParsedRecipeText {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ParsedRecipeText(fallbackTitle, "", emptyList(), emptyList(), extracted = false)
        }

        var title = fallbackTitle
        var start = 0
        if (!looksLikeHeader(lines.first()) && lines.first().length in 3..80) {
            title = lines.first().trim()
            start = 1
        }

        val ingredients = mutableListOf<DraftIngredient>()
        val directions = mutableListOf<String>()
        var section = Section.UNKNOWN

        for (line in lines.drop(start)) {
            val header = headerKind(line)
            if (header != null) {
                section = header
                continue
            }
            when (section) {
                Section.INGREDIENTS -> ingredients += parseIngredientLine(line)
                Section.DIRECTIONS -> directions += stripNumber(line)
                Section.UNKNOWN -> {
                    when {
                        looksLikeIngredient(line) -> ingredients += parseIngredientLine(line)
                        numbered.matches(line) -> directions += stripNumber(line)
                    }
                }
            }
        }

        val extracted = ingredients.isNotEmpty() || directions.isNotEmpty()
        val subtitle = when {
            extracted && ingredients.isNotEmpty() ->
                ingredients.take(3).joinToString(" · ") { it.name.substringBefore(",") }
            !extracted -> "Imported file — add ingredients and steps"
            else -> ""
        }
        return ParsedRecipeText(
            title = title.ifBlank { fallbackTitle },
            subtitle = subtitle,
            ingredients = ingredients,
            directions = directions,
            extracted = extracted,
        )
    }

    fun parseIngredientLine(line: String): DraftIngredient {
        val cleaned = line.replace(Regex("^[-•*]\\s*"), "")
        val match = qtyPattern.find(cleaned)
        if (match != null) {
            val qty = parseAmount(match.groupValues[1])
            val unit = match.groupValues[2].ifBlank { "" }
            val name = match.groupValues[3].ifBlank { cleaned }
            return DraftIngredient(
                name = name.trim(),
                quantityUs = qty,
                unitUs = normalizeUnit(unit),
                quantityMetric = 0.0,
                unitMetric = "",
            )
        }
        return DraftIngredient(
            name = cleaned,
            quantityUs = 0.0,
            unitUs = "",
            quantityMetric = 0.0,
            unitMetric = "",
        )
    }

    private fun looksLikeIngredient(line: String): Boolean =
        qtyPattern.containsMatchIn(line.replace(Regex("^[-•*]\\s*"), ""))

    private fun looksLikeHeader(line: String): Boolean =
        headerKind(line) != null

    private fun headerKind(line: String): Section? {
        val key = line.lowercase().trimEnd(':').trim()
        return when {
            key in ingredientHeaders -> Section.INGREDIENTS
            key in directionHeaders -> Section.DIRECTIONS
            else -> null
        }
    }

    private fun stripNumber(line: String): String =
        numbered.matchEntire(line)?.groupValues?.get(1) ?: line

    private fun parseAmount(raw: String): Double {
        val value = raw.trim()
        return when (value) {
            "¼" -> 0.25
            "½" -> 0.5
            "¾" -> 0.75
            "⅓" -> 1.0 / 3.0
            "⅔" -> 2.0 / 3.0
            else -> {
                val parts = value.split(Regex("\\s+"))
                if (parts.size == 2 && parts[1].contains('/')) {
                    (parts[0].toDoubleOrNull() ?: 0.0) + fraction(parts[1])
                } else if (value.contains('/')) {
                    fraction(value)
                } else {
                    value.toDoubleOrNull() ?: 0.0
                }
            }
        }
    }

    private fun fraction(value: String): Double {
        val bits = value.split('/')
        if (bits.size != 2) return 0.0
        val num = bits[0].toDoubleOrNull() ?: return 0.0
        val den = bits[1].toDoubleOrNull() ?: return 0.0
        if (den == 0.0) return 0.0
        return num / den
    }

    private fun normalizeUnit(unit: String): String = when (unit.lowercase()) {
        "tablespoons", "tablespoon", "tbsp" -> "tbsp"
        "teaspoons", "teaspoon", "tsp" -> "tsp"
        "cups", "cup" -> if (unit.equals("cups", true)) "cups" else "cup"
        "ounces", "ounce", "oz" -> "oz"
        "pounds", "pound", "lbs", "lb" -> "lb"
        "grams", "gram" -> "g"
        "milliliters", "millilitre" -> "ml"
        else -> unit.lowercase()
    }

    private enum class Section { UNKNOWN, INGREDIENTS, DIRECTIONS }
}
