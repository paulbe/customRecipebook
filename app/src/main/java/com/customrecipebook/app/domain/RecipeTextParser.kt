package com.customrecipebook.app.domain

import com.customrecipebook.app.data.DraftIngredient
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.RecipeDraft

data class ParsedRecipeText(
    val title: String,
    val subtitle: String,
    val ingredients: List<DraftIngredient>,
    val directions: List<String>,
    val extracted: Boolean,
    val sourceText: String = "",
)

object RecipeTextParser {
    private val ingredientHeaderWords = listOf(
        "ingredients", "ingredient list", "ingredient", "you will need",
        "shopping list", "what you need", "for the dough", "for the sauce",
        "for the filling",
    )
    private val directionHeaderWords = listOf(
        "directions", "direction", "instructions", "instruction",
        "method", "steps", "preparation", "prepare", "to make",
        "how to make", "procedure",
    )
    private val units = listOf(
        "tablespoons", "tablespoon", "teaspoons", "teaspoon",
        "tbsp", "tsp", "cups", "cup", "ounces", "ounce", "oz",
        "pounds", "pound", "lbs", "lb", "grams", "gram", "kg", "g",
        "milliliters", "millilitre", "ml", "liters", "litre",
        "cloves", "clove", "large", "medium", "small", "pinch", "dash",
        "cans", "can", "sticks", "stick", "slices", "slice",
    )
    private val metricUnits = setOf(
        "g", "kg", "ml", "l", "grams", "gram", "milliliters", "millilitre", "liters", "litre",
    )
    private val unitPattern = units.joinToString("|") { Regex.escape(it) }
    private val qtyToken = """(?:\d+\s+\d+/\d+|\d+/\d+|\d+(?:\.\d+)?|[¼½¾⅓⅔])"""
    private val formattedLine = Regex(
        """^($qtyToken)?:\s*\(([^)]*)\)\s*\((.+)\)\s*$""",
        RegexOption.IGNORE_CASE,
    )
    private val qtyPattern = Regex(
        """^($qtyToken)\s*(?:($unitPattern))?\b[:\s,]*(.*)$""",
        RegexOption.IGNORE_CASE,
    )
    private val numbered = Regex("""^\d+[\.)]\s*(.*)$""")
    private val headerSplit = Regex(
        """(?i)(?<=\S)\s*(?=\b(?:ingredients?|directions?|instructions?|method|steps|preparation)\b)""",
    )
    private val stepSplit = Regex("""(?<!\n)\s+(?=\d+[.)]\s+)""")
    private val qtySplit = Regex(
        """(?<!\n)\s+(?=[-•*]?\s*\d+(?:\s+\d+/\d+)?\s+(?:$unitPattern)\b)""",
        RegexOption.IGNORE_CASE,
    )

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

    fun normalize(raw: String): String {
        var text = raw.replace("\r\n", "\n").replace('\r', '\n')
        text = text.replace(headerSplit, "\n")
        text = text.replace(Regex("""(?i)\b(ingredients?|directions?|instructions?|method|steps|preparation)\s*:"""), "\n$1\n")
        text = text.replace(stepSplit, "\n")
        text = text.replace(qtySplit, "\n")
        return text
    }

    fun parse(raw: String, fallbackTitle: String): ParsedRecipeText {
        val normalized = normalize(raw)
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ParsedRecipeText(fallbackTitle, "", emptyList(), emptyList(), extracted = false, sourceText = raw)
        }

        var title = fallbackTitle
        var start = 0
        if (headerKind(lines.first()) == null && lines.first().length in 3..80) {
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
                Section.UNKNOWN -> classifyUnknown(line, ingredients, directions)
            }
        }

        if (ingredients.isEmpty() && directions.isEmpty() && lines.size > start) {
            for (line in lines.drop(start)) {
                classifyUnknown(line, ingredients, directions)
            }
        }
        if (ingredients.isEmpty() && directions.isEmpty() && lines.size > start) {
            directions += lines.drop(start)
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
            sourceText = raw.trim(),
        )
    }

    fun toDraft(
        parsed: ParsedRecipeText,
        fileName: String,
        attachmentPath: String?,
    ): RecipeDraft {
        val message = if (parsed.extracted) {
            "Filled ${parsed.ingredients.size} ingredients and ${parsed.directions.size} steps from $fileName. Edit anything that looks off, then save."
        } else {
            "Couldn't read text from this PDF. The file is attached — add ingredients and steps below."
        }
        return RecipeDraft(
            title = parsed.title,
            subtitle = parsed.subtitle,
            source = ImportSource.PDF,
            imageUri = null,
            attachmentUri = attachmentPath,
            attachmentName = fileName,
            ingredients = parsed.ingredients,
            directions = parsed.directions,
            parseMessage = message,
            titleConfidence = if (parsed.extracted) 80 else 40,
            ingredientsConfidence = if (parsed.ingredients.isNotEmpty()) 80 else 0,
            instructionsConfidence = if (parsed.directions.isNotEmpty()) 80 else 0,
        )
    }

    fun parseIngredientLine(line: String): DraftIngredient {
        val cleaned = line.replace(Regex("^[-•*–]\\s*"), "").trim()
        val formatted = formattedLine.find(cleaned)
        if (formatted != null) {
            val qtyRaw = formatted.groupValues[1]
            val qty = if (qtyRaw.isBlank()) 0.0 else parseAmount(qtyRaw)
            val unit = formatted.groupValues[2].trim()
            val name = formatted.groupValues[3].trim().ifBlank { cleaned }
            return draftFromParts(name, qty, unit)
        }
        val match = qtyPattern.find(cleaned)
        if (match != null) {
            val qty = parseAmount(match.groupValues[1])
            val unit = match.groupValues[2].ifBlank { "" }
            val name = match.groupValues[3].ifBlank { cleaned }.trim()
            return draftFromParts(name, qty, unit)
        }
        return DraftIngredient(
            name = cleaned,
            quantityUs = 0.0,
            unitUs = "",
            quantityMetric = 0.0,
            unitMetric = "",
        )
    }

    private fun draftFromParts(name: String, qty: Double, rawUnit: String): DraftIngredient {
        val unit = normalizeUnit(rawUnit)
        return if (isMetricUnit(rawUnit, unit)) {
            DraftIngredient(
                name = name,
                quantityUs = 0.0,
                unitUs = "",
                quantityMetric = qty,
                unitMetric = unit,
            )
        } else {
            DraftIngredient(
                name = name,
                quantityUs = qty,
                unitUs = unit,
                quantityMetric = 0.0,
                unitMetric = "",
            )
        }
    }

    private fun isMetricUnit(rawUnit: String, normalized: String): Boolean =
        rawUnit.lowercase() in metricUnits || normalized in metricUnits

    private fun classifyUnknown(
        line: String,
        ingredients: MutableList<DraftIngredient>,
        directions: MutableList<String>,
    ) {
        when {
            looksLikeIngredient(line) -> ingredients += parseIngredientLine(line)
            numbered.matches(line) -> directions += stripNumber(line)
            line.startsWith("-") || line.startsWith("•") || line.startsWith("*") ->
                ingredients += parseIngredientLine(line)
        }
    }

    private fun looksLikeIngredient(line: String): Boolean {
        val cleaned = line.replace(Regex("^[-•*–]\\s*"), "")
        return formattedLine.containsMatchIn(cleaned) || qtyPattern.containsMatchIn(cleaned)
    }

    private fun headerKind(line: String): Section? {
        val key = line.lowercase().trim().trimEnd(':').trim()
        if (ingredientHeaderWords.any { key == it || key.startsWith("$it ") || key.startsWith("$it(") }) {
            return Section.INGREDIENTS
        }
        if (directionHeaderWords.any { key == it || key.startsWith("$it ") || key.startsWith("$it(") }) {
            return Section.DIRECTIONS
        }
        return null
    }

    private fun stripNumber(line: String): String =
        numbered.matchEntire(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: line

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
