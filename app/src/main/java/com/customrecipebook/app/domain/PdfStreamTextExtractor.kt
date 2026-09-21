package com.customrecipebook.app.domain

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import kotlin.math.abs

/**
 * Pulls a text layer out of a PDF by inflating content streams and reading
 * Tj / TJ / ' string operators. Inserts line breaks on Td/TD/T* so recipe
 * headers stay on their own lines for the parser.
 */
object PdfStreamTextExtractor {
    fun extract(bytes: ByteArray): String {
        if (bytes.size < 8) return ""
        val header = bytes.copyOfRange(0, minOf(8, bytes.size)).toString(Charsets.ISO_8859_1)
        if (!header.startsWith("%PDF")) return ""

        val latin = bytes.toString(Charsets.ISO_8859_1)
        val pieces = mutableListOf<String>()
        var index = 0
        while (true) {
            val streamAt = indexOfKeyword(latin, "stream", index) ?: break
            val afterKeyword = streamAt + 6
            var dataStart = afterKeyword
            if (dataStart < latin.length && latin[dataStart] == '\r') dataStart++
            if (dataStart < latin.length && latin[dataStart] == '\n') dataStart++
            val endAt = latin.indexOf("endstream", dataStart)
            if (endAt < 0) break
            val raw = bytes.copyOfRange(dataStart, endAt)
            val dictStart = latin.lastIndexOf("<<", streamAt)
            val dict = if (dictStart >= 0) latin.substring(dictStart, streamAt) else ""
            if (!looksLikeContentStream(dict)) {
                index = endAt + 9
                continue
            }
            val payload = decodeStream(raw, dict)
            val text = readTextOperators(insertLayoutBreaks(payload.toString(Charsets.ISO_8859_1)))
            if (text.isNotBlank()) pieces += text
            index = endAt + 9
        }
        return pieces.joinToString("\n")
            .replace(Regex("[ \\t]{2,}"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun looksLikeContentStream(dict: String): Boolean {
        if (dict.contains("/Subtype") && dict.contains("/Image")) return false
        if (dict.contains("/Length")) return true
        val type = Regex("""/Type\s*/(\w+)""").find(dict)?.groupValues?.get(1)
        return type == null || type == "XObject"
    }

    private fun indexOfKeyword(src: String, word: String, from: Int): Int? {
        var i = from
        while (true) {
            val at = src.indexOf(word, i)
            if (at < 0) return null
            val before = if (at == 0) ' ' else src[at - 1]
            if (before != 'd' && !before.isLetterOrDigit()) return at
            i = at + word.length
        }
    }

    private fun decodeStream(raw: ByteArray, dict: String): ByteArray {
        val flate = dict.contains("FlateDecode", ignoreCase = true)
        if (!flate) return raw
        return inflate(raw) ?: raw
    }

    private fun inflate(raw: ByteArray): ByteArray? {
        val trimmed = raw.dropLastWhile { it == '\n'.code.toByte() || it == '\r'.code.toByte() }.toByteArray()
        return tryInflate(trimmed, nowrap = false) ?: tryInflate(trimmed, nowrap = true)
    }

    private fun tryInflate(raw: ByteArray, nowrap: Boolean): ByteArray? = try {
        val inflater = Inflater(nowrap)
        inflater.setInput(raw)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            if (n == 0) break
            out.write(buf, 0, n)
        }
        inflater.end()
        out.toByteArray().takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }

    internal fun insertLayoutBreaks(src: String): String {
        return src
            .replace(Regex("""\bT\*"""), "\n")
            .replace(Regex("""(-?\d+(?:\.\d+)?)\s+(-?\d+(?:\.\d+)?)\s+T[dD]\b""")) { match ->
                val ty = match.groupValues[2].toDoubleOrNull() ?: 0.0
                if (abs(ty) > 1.0) "\n" else " "
            }
            .replace(Regex("""(?:-?\d+(?:\.\d+)?\s+){5}(-?\d+(?:\.\d+)?)\s+Tm\b""")) { "\n" }
    }

    internal fun readTextOperators(src: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < src.length) {
            when (src[i]) {
                '\n' -> {
                    out.append('\n')
                    i++
                }
                '(' -> {
                    val (text, next) = readLiteral(src, i)
                    i = next
                    val rest = src.substring(i).trimStart()
                    if (rest.startsWith("Tj") || rest.startsWith("'") || rest.startsWith("\"")) {
                        if (rest.startsWith("'") || rest.startsWith("\"")) out.append('\n')
                        appendToken(out, text)
                    }
                }
                '<' -> {
                    val end = src.indexOf('>', i)
                    if (end > i) {
                        val hex = decodeHex(src.substring(i + 1, end))
                        val rest = src.substring(end + 1).trimStart()
                        if (hex.isNotBlank() && (rest.startsWith("Tj") || rest.startsWith("'") || rest.startsWith("\""))) {
                            appendToken(out, hex)
                        }
                        i = end + 1
                    } else {
                        i++
                    }
                }
                '[' -> {
                    val close = findArrayEnd(src, i)
                    if (close > i) {
                        val after = src.substring(close + 1).trimStart()
                        if (after.startsWith("TJ")) {
                            appendToken(out, readTjArray(src.substring(i + 1, close)))
                        }
                        i = close + 1
                    } else {
                        i++
                    }
                }
                else -> i++
            }
        }
        return out.toString().trim()
    }

    private fun appendToken(out: StringBuilder, text: String) {
        if (text.isEmpty()) return
        if (out.isNotEmpty() && out.last() != '\n' && !out.last().isWhitespace() && !text.first().isWhitespace()) {
            out.append(' ')
        }
        out.append(text)
    }

    private fun readTjArray(body: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < body.length) {
            when (body[i]) {
                '(' -> {
                    val (text, next) = readLiteral(body, i)
                    out.append(text)
                    i = next
                }
                '<' -> {
                    val end = body.indexOf('>', i)
                    if (end > i) {
                        out.append(decodeHex(body.substring(i + 1, end)))
                        i = end + 1
                    } else {
                        i++
                    }
                }
                else -> i++
            }
        }
        return out.toString()
    }

    private fun readLiteral(src: String, start: Int): Pair<String, Int> {
        val out = StringBuilder()
        var i = start + 1
        var depth = 0
        while (i < src.length) {
            val c = src[i]
            when (c) {
                '\\' -> {
                    if (i + 1 >= src.length) break
                    val n = src[i + 1]
                    when (n) {
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000c')
                        '(', ')', '\\' -> out.append(n)
                        in '0'..'7' -> {
                            var oct = ""
                            var k = i + 1
                            while (k < src.length && oct.length < 3 && src[k] in '0'..'7') {
                                oct += src[k]
                                k++
                            }
                            out.append(oct.toInt(8).toChar())
                            i = k
                            continue
                        }
                        else -> out.append(n)
                    }
                    i += 2
                }
                ')' -> {
                    if (depth == 0) return out.toString() to (i + 1)
                    depth--
                    out.append(')')
                    i++
                }
                '(' -> {
                    depth++
                    out.append('(')
                    i++
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString() to i
    }

    private fun findArrayEnd(src: String, start: Int): Int {
        var depth = 0
        var i = start
        var inLiteral = false
        while (i < src.length) {
            val c = src[i]
            when {
                inLiteral && c == '\\' -> i += 2
                inLiteral && c == ')' -> {
                    inLiteral = false
                    i++
                }
                !inLiteral && c == '(' -> {
                    inLiteral = true
                    i++
                }
                !inLiteral && c == '[' -> {
                    depth++
                    i++
                }
                !inLiteral && c == ']' -> {
                    depth--
                    if (depth == 0) return i
                    i++
                }
                else -> i++
            }
        }
        return -1
    }

    private fun decodeHex(hex: String): String {
        val clean = hex.filter { !it.isWhitespace() }
        if (clean.length < 2) return ""
        val bytes = ByteArray(clean.length / 2)
        for (i in bytes.indices) {
            bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        } else {
            String(bytes, Charsets.ISO_8859_1)
        }
    }
}
