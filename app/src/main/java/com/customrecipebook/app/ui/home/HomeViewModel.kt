package com.customrecipebook.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customrecipebook.app.data.Recipe
import com.customrecipebook.app.data.RecipeCategory
import com.customrecipebook.app.data.repo.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repository: RecipeRepository) : ViewModel() {
    private val category = MutableStateFlow(RecipeCategory.ALL)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.recipes,
        category,
    ) { recipes, selected ->
        HomeUiState(
            recipes = repository.filter(recipes, selected),
            selectedCategory = selected,
            totalCount = recipes.size,
            loaded = true,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(),
    )

    fun selectCategory(value: RecipeCategory) {
        category.value = value
    }
}

data class HomeUiState(
    val recipes: List<Recipe> = emptyList(),
    val selectedCategory: RecipeCategory = RecipeCategory.ALL,
    val totalCount: Int = 0,
    val loaded: Boolean = false,
)
