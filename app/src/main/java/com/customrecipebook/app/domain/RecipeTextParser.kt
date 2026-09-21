package com.customrecipebook.app.domain

import com.customrecipebook.app.data.DraftIngredient
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.RecipeDraft

data class ParsedRecipeText(
    val title: String,
    val subtitle: String,
    val ingredients: List<DraftIngredient>,
    val directions: List<String>,
    val notes: String = "",
    val prepMinutes: Int = 0,
    val cookMinutes: Int = 0,
    val totalMinutes: Int = 0,
    val servings: Int = 0,
    val extracted: Boolean,
    val sourceText: String = "",
)

object RecipeTextParser {
    private val ingredientHeaderWords = listOf(
        "ingredients", "ingredient list", "ingredient", "you will need",
        "shopping list", "what you need", "for the dough", "for the sauce",
        "for the filling",
    )
    private val notesHeaderPrefixes = listOf(
        "cook's notes", "cooks notes", "chef's notes", "chefs notes",
        "cooking notes", "baker's notes", "bakers notes", "recipe notes",
        "cook's tip", "cook's tips", "chef's tip", "chef's tips",
        "baker's tips", "bakers tips",
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
    private val householdUnitPattern = units
        .filter { it.lowercase() !in metricUnits }
        .joinToString("|") { Regex.escape(it) }
    private val metricUnitToken =
        """(?:kilograms?|milliliters?|millilitres?|liters?|litres?|grams?|kg|ml|g|l)"""
    private val qtyToken = """(?:\d+\s+\d+/\d+|\d+/\d+|\d+(?:\.\d+)?|[¼½¾⅓⅔])"""
    private val formattedParenLine = Regex(
        """^($qtyToken)?:\s*\(([^)]*)\)\s*\((.+)\)\s*$""",
        RegexOption.IGNORE_CASE,
    )
    private val qtyUnitColonLine = Regex(
        """^($qtyToken)\s+($unitPattern)\s*:\s*(.+)$""",
        RegexOption.IGNORE_CASE,
    )
    private val formattedLine = Regex(
        """^($qtyToken):\s+(?:($unitPattern)\b\s+)?(.+)$""",
        RegexOption.IGNORE_CASE,
    )
    private val qtyPattern = Regex(
        """^($qtyToken)\s*(?:($unitPattern))?\b[:\s,]*(.*)$""",
        RegexOption.IGNORE_CASE,
    )
    private val numbered = Regex("""^\d+[\.)]\s*(.*)$""")
    private val stepLabel = Regex(
        """^(?:(?i:step)\s+\d+\s*[:.\-)]*\s*|\d+[.)]\s*|\d{1,2}\s+(?=[A-Z][a-z]{2,}))""",
    )
    private val headerSplit = Regex(
        """(?i)(?<=\S)\s*(?=\b(?:ingredients?|directions?|instructions?|method|steps|preparation|cook'?s notes|chef'?s notes|baker'?s notes|cooking notes|recipe notes)\b|(?<!\d[.)]\s)(?<![sS] )notes\b)""",
    )
    private val tipsHeaderSplit = Regex(
        """(?i)(?<=[A-Za-z][.!?])\s+(?=tips?\b)""",
    )
    private val stepSplit = Regex(
        """(?<=\S)\s+(?=(?:(?i:step)\s+\d+\s*[:.\-)]*|\d+[.)]\s*))""",
    )
    private val qtySplit = Regex(
        """(?<!\n)\s+(?=[-•*]?\s*\d+(?:\s+\d+/\d+)?\s+(?:$unitPattern)\b)""",
        RegexOption.IGNORE_CASE,
    )
    private val volumeYieldUnitPattern = listOf(
        "tablespoons", "tablespoon", "teaspoons", "teaspoon",
        "tbsp", "tsp", "cups", "cup",
        "milliliters", "millilitre", "ml", "liters", "litre",
        "pints", "pint", "quarts", "quart", "gallons", "gallon",
    ).joinToString("|") { Regex.escape(it) }
    private val yieldLeadAtEnd = Regex(
        """(?i)(?:^|[\s;|/])(?:yield|serves|servings|makes(?:\s+about)?)\s*[:\-]?$""",
    )
    private val yieldMetaLine = Regex(
        """(?i)^\s*(?:yield|serves|servings|makes(?:\s+about)?)\s*[:\-]?\s*(?:about\s+)?(?:\d|$)""",
    )
    private val yieldLabelOnly = Regex(
        """(?i)^\s*(?:yield|serves|servings|makes(?:\s+about)?)\s*[:\-]?\s*(?:about\s*)?$""",
    )
    private val volumeOnlyLine = Regex(
        """(?i)^\s*(?:about\s+)?($qtyToken)\s+($volumeYieldUnitPattern)\s*\.?$""",
    )
    private val timeLabelChunk = Regex(
        """(?i)(prep(?:aration)?(?:\s*time)?|cook(?:ing)?(?:\s*time)?(?!')|bake(?:\s*time)?|oven\s*time|total(?:\s*time)?|ready\s*in)\s*[:\-]?\s*(.*?)(?=(?:prep(?:aration)?(?:\s*time)?|cook(?:ing)?(?:\s*time)?|bake(?:\s*time)?|oven\s*time|total(?:\s*time)?|ready\s*in|yield|serves|servings|makes|ingredients?|directions?|instructions?|method|steps)\b|$)""",
    )
    private val yieldChunk = Regex(
        """(?i)\b(?:yield|serves|servings|makes(?:\s+about)?)\s*[:\-]?\s*(?:about\s+)?(\d+)(?:\s+(?:$unitPattern)\b)?""",
    )
    private val hourDuration = Regex(
        """(?i)^\s*(\d+(?:\.\d+)?)\s*(hours?|hrs?|h)\b(?:\s*(?:and\s+)?(\d+)\s*(?:minutes?|mins?|min)\b)?""",
    )
    private val minuteDuration = Regex(
        """(?i)^\s*(\d+(?:\.\d+)?)\s*(minutes?|mins?|min)\b""",
    )
    private val bareDuration = Regex("""^\s*(\d+)\s*$""")

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
        text = text.replace(tipsHeaderSplit, "\n")
        text = text.replace(
            Regex(
                """(?i)(?<!\d[.)]\s)\b(ingredients?|directions?|instructions?|method|steps|preparation|cook'?s notes|chef'?s notes|baker'?s notes|cooking notes|recipe notes|notes?|tips)\s*:""",
            ),
            "\n$1\n",
        )
        text = stripDualMetric(text)
        text = replaceOutsideParens(text, stepSplit)
        text = splitGluedQuantities(text)
        return text
    }

    internal fun stripDualMetric(text: String): String {
        if (text.isBlank()) return text
        var t = text
        val qty = qtyToken
        val metric = metricUnitToken
        val household = householdUnitPattern
        t = t.replace(
            Regex("""(?i)$qty\s*$metric\s*\(\s*($qty)\s+($household)\s*\)"""),
            "$1 $2",
        )
        t = t.replace(
            Regex("""(?i)$qty\s*$metric\s*(?:/|or)\s*($qty)\s+($household)\b"""),
            "$1 $2",
        )
        t = t.replace(
            Regex("""(?i)($qty\s+$household)\s+$qty\s*$metric\b"""),
            "$1",
        )
        t = t.replace(Regex("""(?i)\(\s*$qty\s*$metric\s*\)"""), "")
        t = t.replace(Regex("""(?i)\s*(?:/|or)\s*$qty\s*$metric\b"""), "")
        t = t.replace(Regex("""[ \t]{2,}"""), " ")
        return t
    }

    private fun splitGluedQuantities(text: String): String =
        replaceOutsideParens(text, qtySplit, skip = ::isQtySplitAfterYieldLead)

    private fun isQtySplitAfterYieldLead(text: String, splitAt: Int): Boolean {
        val lineStart = text.lastIndexOf('\n', splitAt - 1).let { if (it < 0) 0 else it + 1 }
        val prefix = text.substring(lineStart, splitAt).trim()
        return prefix.isNotEmpty() && yieldLeadAtEnd.containsMatchIn(prefix)
    }

    private fun replaceOutsideParens(
        text: String,
        pattern: Regex,
        skip: (String, Int) -> Boolean = { _, _ -> false },
    ): String {
        val matches = pattern.findAll(text).toList()
        if (matches.isEmpty()) return text
        val insideParen = BooleanArray(text.length)
        var depth = 0
        for (i in text.indices) {
            when (text[i]) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
            }
            insideParen[i] = depth > 0
        }
        val out = StringBuilder()
        var last = 0
        for (match in matches) {
            if (insideParen.getOrElse(match.range.first) { false }) continue
            if (skip(text, match.range.first)) continue
            out.append(text, last, match.range.first)
            out.append('\n')
            last = match.range.last + 1
        }
        out.append(text, last, text.length)
        return out.toString()
    }

    fun parse(raw: String, fallbackTitle: String): ParsedRecipeText {
        val normalized = normalize(raw)
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ParsedRecipeText(fallbackTitle, "", emptyList(), emptyList(), notes = "", extracted = false, sourceText = raw)
        }

        var title = fallbackTitle
        var start = 0
        if (headerKind(lines.first()) == null &&
            !isMetadataOnlyLine(lines.first()) &&
            !isYieldMetaLine(lines.first()) &&
            lines.first().length in 3..80
        ) {
            title = lines.first().trim()
            start = 1
        }

        val ingredients = mutableListOf<DraftIngredient>()
        val directions = mutableListOf<String>()
        val notes = StringBuilder()
        var section = Section.UNKNOWN
        var prepMinutes = 0
        var cookMinutes = 0
        var totalMinutes = 0
        var servings = 0
        var expectYieldValue = false

        fun mergeTimes(prep: Int, cook: Int, total: Int, serve: Int) {
            if (prepMinutes == 0 && prep > 0) prepMinutes = prep
            if (cookMinutes == 0 && cook > 0) cookMinutes = cook
            if (totalMinutes == 0 && total > 0) totalMinutes = total
            if (servings == 0 && serve > 0) servings = serve
        }

        fun appendNote(text: String) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return
            if (notes.contains(trimmed)) return
            if (notes.isNotEmpty()) notes.append('\n')
            notes.append(trimmed)
        }

        fun applyYieldLine(line: String) {
            val meta = extractMetadata(line)
            val volumeServings = servingsFromVolumeOnly(line)
            mergeTimes(meta.prepMinutes, meta.cookMinutes, meta.totalMinutes, meta.servings)
            if (servings == 0 && volumeServings > 0) servings = volumeServings
            if (isVolumeYieldText(line)) appendNote(line.trim().trimEnd(':').trim())
        }

        fun ingestMetadata(line: String, force: Boolean = false): Boolean {
            val whole = isMetadataOnlyLine(line) || isYieldMetaLine(line)
            if (whole || force || section == Section.UNKNOWN) {
                applyYieldLine(line)
            }
            return whole
        }

        fun consume(target: Section, text: String) {
            val trimmed = text.trim()
            if (isYieldMetaLine(trimmed) || (expectYieldValue && isVolumeOnlyLine(trimmed))) {
                applyYieldLine(trimmed)
                expectYieldValue = isYieldLabelOnly(trimmed)
                return
            }
            when (target) {
                Section.INGREDIENTS -> {
                    if (ingredients.isNotEmpty() && isParentheticalLine(trimmed)) {
                        val prev = ingredients.last()
                        val note = trimmed.replace(Regex("^[-•*–]\\s*"), "").trim()
                        ingredients[ingredients.lastIndex] = prev.copy(
                            name = "${prev.name} $note".trim(),
                        )
                    } else {
                        ingredients += parseIngredientLine(text)
                    }
                }
                Section.DIRECTIONS -> addDirectionLine(text, directions)
                Section.NOTES -> appendNote(text)
                Section.UNKNOWN -> classifyUnknown(text, ingredients, directions)
            }
        }

        for (line in lines.drop(start)) {
            val yieldRemainder = expectYieldValue && isVolumeOnlyLine(line)
            if (yieldRemainder) {
                applyYieldLine(line)
                expectYieldValue = false
                continue
            }
            val wholeMeta = ingestMetadata(line)
            if (wholeMeta) {
                expectYieldValue = isYieldLabelOnly(line)
                continue
            }
            expectYieldValue = false
            val matched = matchHeader(line)
            if (matched != null) {
                section = matched.first
                val rest = matched.second
                ingestMetadata(rest, force = true)
                if (rest.isNotBlank() &&
                    !isParentheticalLine(rest) &&
                    !isMetadataOnlyLine(rest) &&
                    !isYieldMetaLine(rest)
                ) {
                    consume(section, rest)
                }
                continue
            }
            consume(section, line)
        }

        if (ingredients.isEmpty() && directions.isEmpty() && notes.isEmpty() && lines.size > start) {
            for (line in lines.drop(start)) {
                classifyUnknown(line, ingredients, directions)
            }
        }
        if (ingredients.isEmpty() && directions.isEmpty() && notes.isEmpty() && lines.size > start) {
            directions += lines.drop(start)
        }

        val extracted = ingredients.isNotEmpty() || directions.isNotEmpty() || notes.isNotEmpty()
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
            notes = notes.toString().trim(),
            prepMinutes = prepMinutes,
            cookMinutes = cookMinutes,
            totalMinutes = totalMinutes,
            servings = servings,
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
            val notesBit = if (parsed.notes.isNotBlank()) " and notes" else ""
            "Filled ${parsed.ingredients.size} ingredients and ${parsed.directions.size} steps$notesBit from $fileName. Edit anything that looks off, then save."
        } else {
            "Couldn't read text from this PDF. The file is attached — add ingredients and steps below."
        }
        return RecipeDraft(
            title = parsed.title,
            subtitle = "",
            source = ImportSource.PDF,
            imageUri = null,
            attachmentUri = attachmentPath,
            attachmentName = fileName,
            notes = parsed.notes,
            prepMinutes = parsed.prepMinutes,
            cookMinutes = parsed.cookMinutes,
            totalMinutes = parsed.totalMinutes,
            baseServings = parsed.servings.takeIf { it > 0 } ?: 4,
            ingredients = parsed.ingredients,
            directions = parsed.directions,
            parseMessage = message,
            titleConfidence = if (parsed.extracted) 80 else 40,
            ingredientsConfidence = if (parsed.ingredients.isNotEmpty()) 80 else 0,
            instructionsConfidence = if (parsed.directions.isNotEmpty()) 80 else 0,
        )
    }

    fun parseIngredientLine(line: String): DraftIngredient {
        val cleaned = stripDualMetric(line.replace(Regex("^[-•*–]\\s*"), "")).trim()
        val paren = formattedParenLine.find(cleaned)
        if (paren != null) {
            val qtyRaw = paren.groupValues[1]
            val qty = if (qtyRaw.isBlank()) 0.0 else parseAmount(qtyRaw)
            val unit = paren.groupValues[2].trim()
            val name = paren.groupValues[3].trim().ifBlank { cleaned }
            return draftFromParts(name, qty, unit)
        }
        val qtyUnitColon = qtyUnitColonLine.find(cleaned)
        if (qtyUnitColon != null) {
            val qty = parseAmount(qtyUnitColon.groupValues[1])
            val unit = qtyUnitColon.groupValues[2].trim()
            val name = qtyUnitColon.groupValues[3].trim().ifBlank { cleaned }
            return draftFromParts(name, qty, unit)
        }
        val formatted = formattedLine.find(cleaned)
        if (formatted != null) {
            val qty = parseAmount(formatted.groupValues[1])
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
        if (isYieldMetaLine(line) || isVolumeOnlyLine(line)) return
        when {
            startsWithStep(line) || splitDirectionSteps(line).size > 1 ->
                addDirectionLine(line, directions)
            looksLikeIngredient(line) -> ingredients += parseIngredientLine(line)
            numbered.matches(line) -> addDirectionLine(line, directions)
            line.startsWith("-") || line.startsWith("•") || line.startsWith("*") ->
                ingredients += parseIngredientLine(line)
        }
    }

    private fun looksLikeIngredient(line: String): Boolean {
        val cleaned = line.replace(Regex("^[-•*–]\\s*"), "")
        if (isYieldMetaLine(cleaned) || isVolumeOnlyLine(cleaned)) return false
        return formattedParenLine.containsMatchIn(cleaned) ||
            qtyUnitColonLine.containsMatchIn(cleaned) ||
            formattedLine.containsMatchIn(cleaned) ||
            qtyPattern.containsMatchIn(cleaned)
    }

    private fun isParentheticalLine(line: String): Boolean {
        val trimmed = line.trim().replace(Regex("^[-•*–]\\s*"), "").trim()
        if (!trimmed.startsWith("(")) return false
        var depth = 0
        for (c in trimmed) {
            when (c) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
            }
        }
        return depth == 0 && trimmed.endsWith(")")
    }

    private fun headerKind(line: String): Section? = matchHeader(line)?.first

    internal fun parseDurationToMinutes(raw: String): Int? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        hourDuration.find(text)?.let { match ->
            val hours = match.groupValues[1].toDoubleOrNull() ?: return@let
            val extra = match.groupValues[3].toIntOrNull() ?: 0
            return (hours * 60).toInt() + extra
        }
        minuteDuration.find(text)?.let { match ->
            return match.groupValues[1].toDoubleOrNull()?.toInt()
        }
        return bareDuration.find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    private data class LineMetadata(
        val prepMinutes: Int = 0,
        val cookMinutes: Int = 0,
        val totalMinutes: Int = 0,
        val servings: Int = 0,
    )

    private fun extractMetadata(line: String): LineMetadata {
        var prep = 0
        var cook = 0
        var total = 0
        timeLabelChunk.findAll(line).forEach { match ->
            val minutes = parseDurationToMinutes(match.groupValues[2]) ?: return@forEach
            if (minutes <= 0) return@forEach
            when (timeFieldForLabel(match.groupValues[1])) {
                TimeField.PREP -> if (prep == 0) prep = minutes
                TimeField.COOK -> if (cook == 0) cook = minutes
                TimeField.TOTAL -> if (total == 0) total = minutes
            }
        }
        val servings = yieldChunk.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return LineMetadata(prep, cook, total, servings)
    }

    private fun isMetadataOnlyLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        if (isYieldMetaLine(trimmed)) return true
        if (!timeLabelChunk.containsMatchIn(trimmed) && !yieldChunk.containsMatchIn(trimmed)) return false
        val leftover = leftoverAfterMetadata(trimmed)
        if (leftover.isEmpty()) return true
        val words = leftover.split(Regex("\\s+")).filter { it.isNotBlank() }
        return yieldChunk.containsMatchIn(trimmed) &&
            words.size <= 2 &&
            leftover.none { it.isDigit() }
    }

    private fun isYieldMetaLine(line: String): Boolean =
        yieldMetaLine.containsMatchIn(line.trim())

    private fun isYieldLabelOnly(line: String): Boolean =
        yieldLabelOnly.matches(line.trim())

    private fun isVolumeOnlyLine(line: String): Boolean =
        volumeOnlyLine.matches(line.replace(Regex("^[-•*–]\\s*"), "").trim())

    private fun servingsFromVolumeOnly(line: String): Int {
        val match = volumeOnlyLine.find(line.replace(Regex("^[-•*–]\\s*"), "").trim()) ?: return 0
        return parseAmount(match.groupValues[1]).toInt().coerceAtLeast(0)
    }

    private fun isVolumeYieldText(line: String): Boolean {
        val trimmed = line.trim()
        if (isVolumeOnlyLine(trimmed)) return true
        if (!isYieldMetaLine(trimmed)) return false
        return Regex("""(?i)\b(?:$volumeYieldUnitPattern)\b""").containsMatchIn(trimmed)
    }

    private fun leftoverAfterMetadata(line: String): String {
        var text = timeLabelChunk.replace(line, " ")
        text = yieldChunk.replace(text, " ")
        return text.replace(Regex("""[\s()|:;,\-–—/.]+"""), " ").trim()
    }

    private fun timeFieldForLabel(label: String): TimeField {
        val key = label.lowercase().replace(Regex("\\s+"), " ").trim()
        return when {
            key.startsWith("prep") -> TimeField.PREP
            key.startsWith("total") || key.startsWith("ready") -> TimeField.TOTAL
            else -> TimeField.COOK
        }
    }

    private fun matchHeader(line: String): Pair<Section, String>? {
        val raw = line.trim().replace('’', '\'').replace('`', '\'')
        if (raw.isEmpty()) return null
        val key = raw.lowercase().trimEnd(':').trim()

        fun remainder(prefix: String): String {
            val pattern = Regex("^${Regex.escape(prefix)}\\s*:?\\s*", RegexOption.IGNORE_CASE)
            return pattern.replaceFirst(raw, "").trim().trimStart(':', '-', '—', '–').trim()
        }

        fun startsWithHeading(heading: String): Boolean =
            key == heading || key.startsWith("$heading ") || key.startsWith("$heading(") || key.startsWith("$heading:")

        for (word in ingredientHeaderWords) {
            if (startsWithHeading(word)) return Section.INGREDIENTS to remainder(word)
        }
        for (prefix in notesHeaderPrefixes) {
            if (startsWithHeading(prefix)) return Section.NOTES to remainder(prefix)
        }
        when {
            startsWithHeading("notes") -> return Section.NOTES to remainder("notes")
            key == "note" || key.startsWith("note:") -> return Section.NOTES to remainder("note")
            startsWithHeading("tips") -> return Section.NOTES to remainder("tips")
            key == "tip" || key.startsWith("tip:") -> return Section.NOTES to remainder("tip")
        }
        for (word in directionHeaderWords) {
            if (startsWithHeading(word)) return Section.DIRECTIONS to remainder(word)
        }
        return null
    }

    private fun addDirectionLine(line: String, directions: MutableList<String>) {
        val pieces = splitDirectionSteps(line)
        if (pieces.isEmpty()) return
        val continuation = !startsWithStep(line) && directions.isNotEmpty() && pieces.size == 1
        if (continuation) {
            val extra = stripStepLabel(pieces[0])
            if (extra.isNotBlank()) {
                directions[directions.lastIndex] = "${directions.last()} $extra".trim()
            }
            return
        }
        for (piece in pieces) {
            val body = stripStepLabel(piece)
            if (body.isNotBlank()) directions += body
        }
    }

    private fun splitDirectionSteps(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()
        val markers = stepMarkerStarts(trimmed)
        val starts = when {
            markers.isEmpty() -> listOf(0)
            markers.first() == 0 -> markers
            else -> listOf(0) + markers
        }.distinct()
        return starts.mapIndexed { i, from ->
            val to = starts.getOrNull(i + 1) ?: trimmed.length
            trimmed.substring(from, to).trim()
        }.filter { it.isNotEmpty() }
    }

    private fun stepMarkerStarts(text: String): List<Int> {
        val namedRanges = Regex("""(?i:step)\s+\d+\s*[:.\-)]*""").findAll(text).map { it.range }.toList()
        val starts = mutableListOf<Int>()
        fun atBoundary(index: Int): Boolean =
            index == 0 || text.getOrNull(index - 1)?.isWhitespace() == true
        fun insideNamed(index: Int): Boolean = namedRanges.any { index in it }
        namedRanges.forEach { range ->
            if (atBoundary(range.first)) starts += range.first
        }
        Regex("""\d+[.)]\s*""").findAll(text).forEach { match ->
            val index = match.range.first
            if (atBoundary(index) && !insideNamed(index)) starts += index
        }
        Regex("""\d{1,2}\s+(?=[A-Z][a-z]{2,})""").findAll(text).forEach { match ->
            val index = match.range.first
            if (atBoundary(index) && !insideNamed(index)) starts += index
        }
        return starts.distinct().sorted()
    }

    private fun startsWithStep(line: String): Boolean =
        stepLabel.find(line.trim())?.range?.first == 0

    private fun stripStepLabel(line: String): String {
        val trimmed = line.trim()
        val match = stepLabel.find(trimmed)
        if (match != null && match.range.first == 0) {
            return trimmed.substring(match.range.last + 1).trim()
        }
        return numbered.matchEntire(trimmed)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: trimmed
    }

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

    private enum class Section { UNKNOWN, INGREDIENTS, DIRECTIONS, NOTES }
    private enum class TimeField { PREP, COOK, TOTAL }
}
