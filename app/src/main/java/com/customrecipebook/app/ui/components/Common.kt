package com.customrecipebook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customrecipebook.app.R
import com.customrecipebook.app.data.Recipe
import com.customrecipebook.app.data.RecipeCategory
import com.customrecipebook.app.ui.theme.Clay
import com.customrecipebook.app.ui.theme.Espresso
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.SoftLine
import com.customrecipebook.app.ui.theme.Taupe
import com.customrecipebook.app.ui.theme.Terracotta

val CardShape = RoundedCornerShape(22.dp)
val PillShape = RoundedCornerShape(50)

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Terracotta),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "C",
            color = Ivory,
            fontSize = (size * 0.48).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Ivory,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun BackCircle(onClick: () -> Unit, modifier: Modifier = Modifier, light: Boolean = false) {
    CircleIconButton(
        onClick = onClick,
        modifier = modifier,
        background = if (light) Ivory.copy(alpha = 0.94f) else Ivory,
    ) {
        Icon(
            imageVector = Icons.Rounded.ChevronLeft,
            contentDescription = "Back",
            tint = Espresso,
        )
    }
}

@Composable
fun SoftChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val bg = if (selected) Terracotta else Ivory
    val fg = if (selected) Color.White else Taupe
    Text(
        text = text,
        color = fg,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(PillShape)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun RecipeThumb(recipe: Recipe, modifier: Modifier = Modifier, size: Int = 56) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Clay),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = recipeThumbIcon(recipe),
            contentDescription = null,
            tint = Terracotta.copy(alpha = 0.85f),
            modifier = Modifier.size((size * 0.42).dp),
        )
    }
}

fun recipeThumbIcon(recipe: Recipe): ImageVector = when (recipe.imageKey) {
    "cookies" -> Icons.Outlined.BakeryDining
    "chicken" -> Icons.Outlined.DinnerDining
    "pasta" -> Icons.Outlined.RamenDining
    "bread", "muffins" -> Icons.Outlined.BakeryDining
    else -> if (recipe.category == RecipeCategory.BAKING) {
        Icons.Outlined.BakeryDining
    } else {
        Icons.Outlined.LocalDining
    }
}

fun recipeImageRes(imageKey: String?): Int? = when (imageKey) {
    "cookies" -> R.drawable.img_cookies
    else -> null
}

@Composable
fun MetaDotRow(items: List<String>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Text("  ·  ", color = Taupe, fontSize = 13.sp)
            }
            Text(item, color = Taupe, fontSize = 13.sp)
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(SoftLine)
    )
}
