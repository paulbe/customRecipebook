package com.customrecipebook.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.customrecipebook.app.RecipebookApplication
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.Recipe
import com.customrecipebook.app.data.RecipeCategory
import com.customrecipebook.app.domain.QuantityFormatter
import com.customrecipebook.app.ui.components.BrandMark
import com.customrecipebook.app.ui.components.CardShape
import com.customrecipebook.app.ui.components.CircleIconButton
import com.customrecipebook.app.ui.components.RecipeThumb
import com.customrecipebook.app.ui.components.SoftChip
import com.customrecipebook.app.ui.theme.Clay
import com.customrecipebook.app.ui.theme.Espresso
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.Sand
import com.customrecipebook.app.ui.theme.Taupe
import com.customrecipebook.app.ui.theme.Terracotta

@Composable
fun HomeScreen(
    onOpenRecipe: (String) -> Unit,
    onAdd: (ImportSource?) -> Unit,
    app: RecipebookApplication,
    modifier: Modifier = Modifier,
) {
    val vm: HomeViewModel = viewModel(factory = app.container.factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    var addOpen by remember { mutableStateOf(false) }

    fun choose(source: ImportSource) {
        addOpen = false
        onAdd(source)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Sand),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(size = 42)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Custom",
                    color = Espresso,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                )
                Text(
                    "Recipebook",
                    color = Espresso,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                )
            }
            CircleIconButton(onClick = { addOpen = !addOpen }) {
                Icon(
                    if (addOpen) Icons.Outlined.Close else Icons.Outlined.Add,
                    contentDescription = if (addOpen) "Close add menu" else "Add recipe",
                    tint = Espresso,
                )
            }
        }
        AnimatedVisibility(visible = addOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ImportChip(
                    label = "PDF",
                    icon = Icons.Outlined.PictureAsPdf,
                    modifier = Modifier.weight(1f),
                    onClick = { choose(ImportSource.PDF) },
                )
                ImportChip(
                    label = "Camera",
                    icon = Icons.Outlined.CameraAlt,
                    modifier = Modifier.weight(1f),
                    onClick = { choose(ImportSource.CAMERA) },
                )
                ImportChip(
                    label = "Manual",
                    icon = Icons.Outlined.Edit,
                    modifier = Modifier.weight(1f),
                    onClick = { choose(ImportSource.MANUAL) },
                )
            }
        }
        Box(Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(RecipeCategory.entries) { category ->
                            SoftChip(
                                text = category.label,
                                selected = state.selectedCategory == category,
                                onClick = { vm.selectCategory(category) },
                            )
                        }
                    }
                }
                if (!state.loaded) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Terracotta, strokeWidth = 3.dp)
                        }
                    }
                } else if (state.totalCount == 0) {
                    item { EmptyLibrary() }
                } else if (state.recipes.isEmpty()) {
                    item { EmptyFilter(state.selectedCategory) }
                } else {
                    items(state.recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { onOpenRecipe(recipe.id) },
                        )
                    }
                }
            }
            if (addOpen) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Espresso.copy(alpha = 0.18f))
                        .clickable { addOpen = false },
                )
            }
        }
    }
}

@Composable
private fun ImportChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(20.dp), ambientColor = Terracotta.copy(0.08f))
            .clip(RoundedCornerShape(20.dp))
            .background(Ivory)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = Terracotta, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = Espresso, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(CardShape)
            .background(Ivory)
            .clickable(onClick = onClick)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(5.dp)
                .height(88.dp)
                .background(Terracotta),
        )
        RecipeThumb(recipe, modifier = Modifier.padding(start = 14.dp, top = 16.dp, bottom = 16.dp))
        Column(
            modifier = Modifier
                .padding(start = 14.dp, top = 14.dp, bottom = 14.dp)
                .weight(1f),
        ) {
            Text(
                recipe.title,
                color = Espresso,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${QuantityFormatter.formatMinutes(recipe.minutes)}  ·  ${recipe.yieldLabel()}",
                color = Taupe,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(8.dp))
            SoftChip(text = recipe.category.label)
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .clip(CardShape)
            .background(Ivory)
            .padding(22.dp),
    ) {
        Text("Your book is empty", color = Espresso, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Nothing ships preloaded. Tap + to add a PDF, a photo, or a recipe you type yourself.",
            color = Taupe,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun EmptyFilter(category: RecipeCategory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp)
            .clip(CardShape)
            .background(Clay)
            .padding(24.dp),
    ) {
        Text(
            when (category) {
                RecipeCategory.SAVED -> "Nothing saved yet"
                RecipeCategory.DINNER -> "No dinner recipes"
                RecipeCategory.BAKING -> "No baking recipes"
                RecipeCategory.ALL -> "Your book is empty"
            },
            color = Espresso,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when (category) {
                RecipeCategory.SAVED -> "Open a recipe and tap the bookmark to keep it on this list."
                else -> "Tap + to add a PDF, snap a page, or type a recipe for this shelf."
            },
            color = Taupe,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}
