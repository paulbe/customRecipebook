package com.customrecipebook.app.data.importing

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.RecipeDraft
import com.customrecipebook.app.domain.PdfStreamTextExtractor
import com.customrecipebook.app.domain.RecipeTextParser
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class RecipeImporter(private val app: Application) {
    suspend fun importPdf(uri: Uri): RecipeDraft = withContext(Dispatchers.IO) {
        persistReadPermission(uri)
        val displayName = displayName(uri) ?: "recipe.pdf"
        val copied = copyToImports(uri, displayName)
        val bytes = copied.readBytes()
        val extracted = extractPdfText(bytes)
        val fallbackTitle = RecipeTextParser.titleFromFileName(displayName)
        val parsed = RecipeTextParser.parse(extracted, fallbackTitle)
        val draft = RecipeTextParser.toDraft(parsed, displayName, copied.absolutePath)
        Log.i(
            TAG,
            "PDF import file=$displayName bytes=${bytes.size} extractedChars=${extracted.length} " +
                "ingredients=${draft.ingredients.size} directions=${draft.directions.size} " +
                "extracted=${parsed.extracted}",
        )
        draft
    }

    suspend fun importPhoto(uri: Uri): RecipeDraft = withContext(Dispatchers.IO) {
        persistReadPermission(uri)
        val displayName = displayName(uri) ?: "photo.jpg"
        val copied = copyToImports(uri, displayName)
        val title = RecipeTextParser.titleFromFileName(displayName).let {
            if (it.equals("Photo", true) || it.equals("Image", true) || it.equals("Capture", true)) {
                "Photo recipe"
            } else {
                it
            }
        }
        RecipeDraft(
            title = title,
            source = ImportSource.CAMERA,
            imageUri = copied.absolutePath,
            attachmentUri = copied.absolutePath,
            attachmentName = displayName,
            ingredients = emptyList(),
            directions = emptyList(),
            parseMessage = "Saved your photo. Add the recipe details by hand — camera OCR is not enabled in v1.",
            titleConfidence = 35,
            ingredientsConfidence = 0,
            instructionsConfidence = 0,
        )
    }

    internal fun extractPdfText(bytes: ByteArray): String {
        val custom = runCatching { PdfStreamTextExtractor.extract(bytes) }.getOrDefault("")
        val pdfBox = extractWithPdfBox(bytes)
        val chosen = when {
            pdfBox.length > custom.length + 10 -> pdfBox
            custom.isNotBlank() -> custom
            else -> pdfBox
        }
        Log.i(TAG, "extract custom=${custom.length} pdfbox=${pdfBox.length} chosen=${chosen.length}")
        return chosen
    }

    private fun extractWithPdfBox(bytes: ByteArray): String = try {
        if (!pdfBoxReady) {
            PDFBoxResourceLoader.init(app)
            pdfBoxReady = true
        }
        PDDocument.load(bytes).use { document ->
            PDFTextStripper().apply { sortByPosition = true }.getText(document).trim()
        }
    } catch (error: Exception) {
        Log.w(TAG, "PDFBox extract failed: ${error.message}")
        ""
    }

    private fun persistReadPermission(uri: Uri) {
        try {
            app.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // GetContent / camera captures often are not persistable; we copy bytes instead.
        }
    }

    private fun displayName(uri: Uri): String? {
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return uri.lastPathSegment
    }

    private fun copyToImports(uri: Uri, displayName: String): File {
        val dir = File(app.filesDir, "imports").apply { mkdirs() }
        val ext = displayName.substringAfterLast('.', missingDelimiterValue = "bin")
            .lowercase()
            .filter { it.isLetterOrDigit() }
            .ifBlank { "bin" }
            .take(8)
        val dest = File(dir, "${UUID.randomUUID()}.$ext")
        val input = app.contentResolver.openInputStream(uri)
            ?: error("Couldn't open the selected file.")
        input.use { src ->
            dest.outputStream().use { out -> src.copyTo(out) }
        }
        if (dest.length() == 0L) error("The selected file was empty.")
        return dest
    }

    companion object {
        private const val TAG = "RecipeImport"
        @Volatile private var pdfBoxReady = false
    }
}
