package com.customrecipebook.app.data.importing

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.RecipeDraft
import com.customrecipebook.app.domain.PdfStreamTextExtractor
import com.customrecipebook.app.domain.RecipeTextParser
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
        val extracted = runCatching { PdfStreamTextExtractor.extract(bytes) }.getOrDefault("")
        val fallbackTitle = RecipeTextParser.titleFromFileName(displayName)
        val parsed = RecipeTextParser.parse(extracted, fallbackTitle)
        val message = if (parsed.extracted) {
            "Read the text layer from $displayName. Check quantities, then save."
        } else {
            "Couldn't read text from this PDF. The file is attached — add ingredients and steps below."
        }
        RecipeDraft(
            title = parsed.title,
            subtitle = parsed.subtitle,
            source = ImportSource.PDF,
            imageUri = null,
            attachmentUri = copied.absolutePath,
            attachmentName = displayName,
            ingredients = parsed.ingredients,
            directions = parsed.directions,
            parseMessage = message,
            titleConfidence = if (parsed.extracted) 80 else 40,
            ingredientsConfidence = if (parsed.ingredients.isNotEmpty()) 70 else 0,
            instructionsConfidence = if (parsed.directions.isNotEmpty()) 70 else 0,
        )
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
            subtitle = "Photo attached — add ingredients and steps",
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
}
