package com.customrecipebook.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.customrecipebook.app.data.Recipe
import com.customrecipebook.app.data.UnitSystem
import com.customrecipebook.app.data.prefs.UserPreferences
import com.customrecipebook.app.data.repo.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    private val preferences: UserPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val recipeId: String = checkNotNull(savedStateHandle["recipeId"])
    private val scale = MutableStateFlow(1)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeRecipe(recipeId),
        preferences.unitSystem,
        scale,
    ) { recipe, units, batch ->
        DetailUiState(
            recipe = recipe,
            unitSystem = units,
            scale = batch.coerceIn(1, 8),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DetailUiState(),
    )

    fun setUnitSystem(system: UnitSystem) {
        viewModelScope.launch { preferences.setUnitSystem(system) }
    }

    fun bumpScale(delta: Int) {
        scale.update { (it + delta).coerceIn(1, 8) }
    }

    fun toggleChecked(ingredientId: String, currentlyChecked: Boolean) {
        viewModelScope.launch {
            repository.setIngredientChecked(ingredientId, !currentlyChecked)
        }
    }

    fun toggleSaved() {
        val recipe = uiState.value.recipe ?: return
        viewModelScope.launch { repository.setSaved(recipe.id, !recipe.isSaved) }
    }

    companion object {
        fun factory(
            repository: RecipeRepository,
            preferences: UserPreferences,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return RecipeDetailViewModel(
                    repository,
                    preferences,
                    extras.createSavedStateHandle(),
                ) as T
            }
        }
    }
}

private fun MutableStateFlow<Int>.update(block: (Int) -> Int) {
    value = block(value)
}

data class DetailUiState(
    val recipe: Recipe? = null,
    val unitSystem: UnitSystem = UnitSystem.US,
    val scale: Int = 1,
    val missing: Boolean = false,
)
