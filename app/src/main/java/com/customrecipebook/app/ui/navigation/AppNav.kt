package com.customrecipebook.app.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.customrecipebook.app.RecipebookApplication
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.ui.add.AddRecipeScreen
import com.customrecipebook.app.ui.add.AddRecipeViewModel
import com.customrecipebook.app.ui.add.ManualRecipeScreen
import com.customrecipebook.app.ui.detail.RecipeDetailScreen
import com.customrecipebook.app.ui.home.HomeScreen
import com.customrecipebook.app.ui.settings.SettingsScreen
import com.customrecipebook.app.ui.shopping.ShoppingScreen
import com.customrecipebook.app.ui.theme.Espresso
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.Sand
import com.customrecipebook.app.ui.theme.Taupe
import com.customrecipebook.app.ui.theme.Terracotta

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Recipes("recipes", "Recipes", Icons.AutoMirrored.Outlined.MenuBook),
    Shopping("shopping", "Shopping", Icons.Outlined.ShoppingBag),
    Settings("settings", "Settings", Icons.Outlined.Settings),
}

@Composable
fun RecipebookNav() {
    val app = LocalContext.current.applicationContext as RecipebookApplication
    val activity = LocalContext.current as ComponentActivity
    val nav = rememberNavController()
    val addVm: AddRecipeViewModel = viewModel(activity, factory = app.container.factory)
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val showBar = Tab.entries.any { route == it.route }

    Scaffold(
        containerColor = Sand,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Ivory, contentColor = Espresso) {
                    Tab.entries.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Tab.Recipes.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(tab.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Terracotta,
                                selectedTextColor = Terracotta,
                                unselectedIconColor = Taupe,
                                unselectedTextColor = Taupe,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Recipes.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Recipes.route) {
                HomeScreen(
                    onOpenRecipe = { nav.navigate("recipe/$it") },
                    onAdd = { source -> navigateToAdd(nav, addVm, source) },
                    app = app,
                    modifier = Modifier.statusBarsPadding(),
                )
            }
            composable(Tab.Shopping.route) {
                ShoppingScreen(app, Modifier.statusBarsPadding())
            }
            composable(Tab.Settings.route) {
                SettingsScreen(app, Modifier.statusBarsPadding())
            }
            composable(
                route = "add?source={source}",
                arguments = listOf(
                    navArgument("source") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val source = entry.arguments?.getString("source")?.let {
                    runCatching { ImportSource.valueOf(it) }.getOrNull()
                }
                AddRecipeScreen(
                    initialSource = source,
                    onBack = { nav.popBackStack() },
                    onManual = {
                        addVm.startBlankManual()
                        nav.navigate("manual")
                    },
                    onReview = { nav.navigate("manual") },
                    onSaved = { id ->
                        nav.navigate("recipe/$id") { popUpTo(Tab.Recipes.route) }
                    },
                    app = app,
                )
            }
            composable("manual") {
                ManualRecipeScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { id ->
                        val returnedToDetail = nav.popBackStack("recipe/$id", inclusive = false)
                        if (!returnedToDetail) {
                            nav.navigate("recipe/$id") { popUpTo(Tab.Recipes.route) }
                        }
                    },
                    app = app,
                )
            }
            composable(
                route = "recipe/{recipeId}",
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType }),
            ) {
                RecipeDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { recipe ->
                        addVm.startEdit(recipe)
                        nav.navigate("manual")
                    },
                    app = app,
                )
            }
        }
    }
}

private fun navigateToAdd(
    nav: NavHostController,
    addVm: AddRecipeViewModel,
    source: ImportSource?,
) {
    when (source) {
        ImportSource.MANUAL -> {
            addVm.startBlankManual()
            nav.navigate("manual")
        }
        null -> nav.navigate("add")
        else -> nav.navigate("add?source=${source.name}")
    }
}
