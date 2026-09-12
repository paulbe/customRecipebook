package com.customrecipebook.app.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.RecipeDraft
import com.customrecipebook.app.data.repo.RecipeRepository
import com.customrecipebook.app.data.repo.SeedData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddRecipeViewModel(private val repository: RecipeRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddRecipeUiState())
    val state: StateFlow<AddRecipeUiState> = _state

    fun applyParsedImport(source: ImportSource, imageUri: String?) {
        val stub = SeedData.creamyWhiteBeanSoup().copy(
            source = source,
            imageUri = imageUri,
        )
        _state.update {
            it.copy(
                draft = stub,
                hasPreview = true,
                parseLabel = when (source) {
                    ImportSource.PDF -> "Parsed from your PDF"
                    ImportSource.CAMERA -> "Read from your photo"
                    else -> "Draft ready"
                },
            )
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
    val parseLabel: String = "",
    val error: String? = null,
)
