package com.customrecipebook.app.ui.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customrecipebook.app.data.RecipeDraft
import com.customrecipebook.app.data.importing.RecipeImporter
import com.customrecipebook.app.data.repo.RecipeRepository
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddRecipeViewModel(
    private val repository: RecipeRepository,
    private val importer: RecipeImporter,
) : ViewModel() {
    private val _state = MutableStateFlow(AddRecipeUiState())
    val state: StateFlow<AddRecipeUiState> = _state

    fun importPdf(uri: Uri, onReady: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isParsing = true, error = null) }
            runCatching { importer.importPdf(uri) }
                .onSuccess { draft ->
                    Log.i(
                        "RecipeImport",
                        "ViewModel draft title=${draft.title} ings=${draft.ingredients.size} steps=${draft.directions.size}",
                    )
                    _state.update {
                        it.copy(
                            draft = draft,
                            hasPreview = true,
                            isParsing = false,
                            parseLabel = draft.attachmentName ?: "PDF",
                        )
                    }
                    onReady()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isParsing = false,
                            error = error.message ?: "Couldn't read that PDF.",
                        )
                    }
                }
        }
    }

    fun importPhoto(uri: Uri, onReady: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isParsing = true, error = null) }
            runCatching { importer.importPhoto(uri) }
                .onSuccess { draft ->
                    _state.update {
                        it.copy(
                            draft = draft,
                            hasPreview = true,
                            isParsing = false,
                            parseLabel = draft.attachmentName ?: "Photo",
                        )
                    }
                    onReady()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isParsing = false,
                            error = error.message ?: "Couldn't use that photo.",
                        )
                    }
                }
        }
    }

    fun updateDraft(transform: (RecipeDraft) -> RecipeDraft) {
        _state.update { it.copy(draft = transform(it.draft)) }
    }

    fun save(onSaved: (String) -> Unit) {
        val draft = _state.value.draft
        if (draft.title.isBlank()) {
            _state.update { it.copy(error = "Add a title before saving.") }
            return
        }
        viewModelScope.launch {
            val id = repository.saveDraft(draft)
            onSaved(id)
        }
    }

    fun startBlankManual() {
        _state.value = AddRecipeUiState(draft = RecipeDraft())
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class AddRecipeUiState(
    val draft: RecipeDraft = RecipeDraft(),
    val hasPreview: Boolean = false,
    val isParsing: Boolean = false,
    val parseLabel: String = "",
    val error: String? = null,
)
