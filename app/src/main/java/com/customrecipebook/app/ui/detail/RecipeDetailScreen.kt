package com.customrecipebook.app.ui.detail

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.customrecipebook.app.RecipebookApplication
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.Ingredient
import com.customrecipebook.app.data.Recipe
import com.customrecipebook.app.data.UnitSystem
import com.customrecipebook.app.data.repo.label
import com.customrecipebook.app.domain.QuantityFormatter
import com.customrecipebook.app.ui.components.BackCircle
import com.customrecipebook.app.ui.components.PillShape
import com.customrecipebook.app.ui.components.RecipeThumb
import com.customrecipebook.app.ui.components.SoftChip
import com.customrecipebook.app.ui.components.recipeImageRes
import com.customrecipebook.app.ui.theme.Clay
import com.customrecipebook.app.ui.theme.Espresso
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.Sand
import com.customrecipebook.app.ui.theme.SoftLine
import com.customrecipebook.app.ui.theme.Taupe
import com.customrecipebook.app.ui.theme.Terracotta

@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    app: RecipebookApplication,
) {
    val vm: RecipeDetailViewModel = viewModel(
        factory = RecipeDetailViewModel.factory(app.container.repository, app.container.preferences),
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val recipe = state.recipe

    if (recipe == null) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Sand)
                .statusBarsPadding()
                .padding(20.dp),
        ) {
            BackCircle(onClick = onBack)
            Spacer(Modifier.height(24.dp))
            Text("This recipe is no longer in your book.", color = Taupe)
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Sand)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Hero(recipe = recipe, onBack = onBack, onToggleSaved = vm::toggleSaved)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(recipe.title, color = Espresso, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp)
            if (recipe.subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(recipe.subtitle, color = Taupe, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftChip(QuantityFormatter.formatMinutes(recipe.minutes))
                SoftChip(recipe.difficulty)
                SoftChip(recipe.category.label)
            }
            Spacer(Modifier.height(18.dp))
            UnitToggle(state.unitSystem, vm::setUnitSystem)
            Spacer(Modifier.height(12.dp))
            ScaleRow(recipe = recipe, scale = state.scale, onBump = vm::bumpScale)
            Spacer(Modifier.height(22.dp))
            IngredientsHeader(scale = state.scale)
            Spacer(Modifier.height(10.dp))
        }
        recipe.ingredients.forEach { ingredient ->
            IngredientRow(
                ingredient = ingredient,
                system = state.unitSystem,
                scale = state.scale,
                onToggle = { vm.toggleChecked(ingredient.id, ingredient.isChecked) },
            )
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            Text("DIRECTIONS", color = Taupe, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(12.dp))
            recipe.directions.forEach { step ->
                Row(
                    modifier = Modifier.padding(bottom = 14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Terracotta),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            step.stepNumber.toString(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        step.text,
                        color = Espresso,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Hero(recipe: Recipe, onBack: () -> Unit, onToggleSaved: () -> Unit) {
    val imageRes = recipeImageRes(recipe.imageKey)
    Box(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Clay),
    ) {
        when {
            imageRes != null -> Image(
                painter = painterResource(imageRes),
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            recipe.imageUri != null -> AsyncImage(
                model = Uri.parse(recipe.imageUri),
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RecipeThumb(recipe, size = 96)
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Sand))),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackCircle(onClick = onBack, light = true)
            Spacer(Modifier.weight(1f))
            if (recipe.source == ImportSource.PDF) {
                Text(
                    recipe.source.label(),
                    color = Espresso,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(PillShape)
                        .background(Ivory.copy(alpha = 0.94f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Ivory.copy(alpha = 0.94f))
                    .clickable(onClick = onToggleSaved),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (recipe.isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (recipe.isSaved) "Remove from saved" else "Save recipe",
                    tint = Terracotta,
                )
            }
        }
    }
}

@Composable
private fun UnitToggle(system: UnitSystem, onChange: (UnitSystem) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(Clay),
    ) {
        UnitSystem.entries.forEach { option ->
            val selected = option == system
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(if (selected) Terracotta else Color.Transparent)
                    .clickable { onChange(option) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (option == UnitSystem.US) "US" else "Metric",
                    color = if (selected) Color.White else Taupe,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun ScaleRow(recipe: Recipe, scale: Int, onBump: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ivory)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("SERVINGS", color = Taupe, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            val from = recipe.yieldLabel(1)
            val to = recipe.yieldLabel(scale)
            Text(
                text = if (scale == 1) to else "${recipe.baseServings} → $to",
                color = Espresso,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (scale == 1) {
                Text(from, color = Color.Transparent, fontSize = 1.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("BATCH", color = Taupe, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleStep(enabled = scale > 1, onClick = { onBump(-1) }) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease batch", tint = Espresso, modifier = Modifier.size(18.dp))
                }
                Text(
                    "${scale}×",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = Espresso,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                CircleStep(enabled = scale < 8, onClick = { onBump(1) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase batch", tint = Espresso, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CircleStep(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Clay)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun IngredientsHeader(scale: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("INGREDIENTS", color = Taupe, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        if (scale > 1) {
            Spacer(Modifier.width(8.dp))
            Text(
                "${scale}×",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Terracotta)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("Tap to check off", color = Taupe, fontSize = 12.sp)
    }
}

@Composable
private fun IngredientRow(
    ingredient: Ingredient,
    system: UnitSystem,
    scale: Int,
    onToggle: () -> Unit,
) {
    val bg = if (ingredient.isChecked) Clay else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (ingredient.isChecked) Terracotta else SoftLine, CircleShape)
                .background(if (ingredient.isChecked) Terracotta else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (ingredient.isChecked) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = QuantityFormatter.ingredientLine(ingredient, system, scale),
            color = if (ingredient.isChecked) Taupe else Espresso,
            fontSize = 17.sp,
            lineHeight = 24.sp,
        )
    }
}
