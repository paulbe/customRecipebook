package com.customrecipebook.app

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.customrecipebook.app.data.db.AppDatabase
import com.customrecipebook.app.data.prefs.UserPreferences
import com.customrecipebook.app.data.repo.RecipeRepository
import com.customrecipebook.app.ui.add.AddRecipeViewModel
import com.customrecipebook.app.ui.home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RecipebookApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        container = AppContainer(this, scope)
        scope.launch { container.repository.seedIfEmpty() }
    }
}

class AppContainer(app: RecipebookApplication, val scope: CoroutineScope) {
    private val database: AppDatabase = Room.databaseBuilder(
        app,
        AppDatabase::class.java,
        "custom-recipebook.db",
    ).fallbackToDestructiveMigration().build()

    val repository = RecipeRepository(database)
    val preferences = UserPreferences(app)
    val factory = RecipebookViewModelFactory(repository, preferences)
}

class RecipebookViewModelFactory(
    private val repository: RecipeRepository,
    private val preferences: UserPreferences,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(repository) as T
            modelClass.isAssignableFrom(AddRecipeViewModel::class.java) ->
                AddRecipeViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
        }
    }
}
