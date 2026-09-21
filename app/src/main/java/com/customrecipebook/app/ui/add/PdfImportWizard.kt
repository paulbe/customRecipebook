package com.customrecipebook.app.ui.add

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.customrecipebook.app.data.DraftIngredient
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.RecipeDraft
import com.customrecipebook.app.data.repo.RecipeRepository
import com.customrecipebook.app.domain.IngredientNotesBoundary
import com.customrecipebook.app.ui.components.SoftChip
import com.customrecipebook.app.ui.theme.Clay
import com.customrecipebook.app.ui.theme.Espresso
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.Taupe
import com.customrecipebook.app.ui.theme.Terracotta
import kotlinx.coroutines.launch

private enum class PdfImportSlide(val title: String, val hint: String) {
    Details("Recipe", "Title, times, and servings"),
    Ingredients("Ingredients", "Check each line"),
    Boundary("Ingredients or notes", "Move the cut if the PDF mixed them"),
    Finish("Directions & notes", "Then save"),
}

@Composable
internal fun PdfImportWizard(
    draft: RecipeDraft,
    vm: AddRecipeViewModel,
    onSaved: (String) -> Unit,
) {
    val slides = PdfImportSlide.entries
    val pager = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == slides.lastIndex

    Column(Modifier.fillMaxSize()) {
        val slide = slides[pager.currentPage]
        Text(slide.title, color = Espresso, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(slide.hint, color = Taupe, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            slides.forEachIndexed { index, _ ->
                Box(
                    Modifier
                        .height(6.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (index <= pager.currentPage) Terracotta else Clay),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
        ) { page ->
            when (slides[page]) {
                PdfImportSlide.Details -> DetailsSlide(draft, vm)
                PdfImportSlide.Ingredients -> IngredientsSlide(draft, vm)
                PdfImportSlide.Boundary -> BoundarySlide(draft, vm)
                PdfImportSlide.Finish -> FinishSlide(draft, vm)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (pager.currentPage > 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Clay)
                        .clickable { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Back", color = Espresso, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Terracotta)
                    .clickable {
                        if (last) vm.save(onSaved)
                        else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (last) "Save recipe" else "Next",
                    color = Ivory,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun DetailsSlide(draft: RecipeDraft, vm: AddRecipeViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (draft.attachmentName != null || draft.parseMessage.isNotBlank()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Clay)
                    .padding(14.dp),
            ) {
                Text("Attached PDF", color = Taupe, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                if (draft.attachmentName != null) {
                    Text(draft.attachmentName, color = Espresso, fontWeight = FontWeight.SemiBold)
                }
                if (draft.parseMessage.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(draft.parseMessage, color = Taupe, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
        Field("Title", draft.title) { vm.updateDraft { d -> d.copy(title = it) } }
        Text("Category", color = Taupe, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeRepository.supportedCategories.forEach { category ->
                SoftChip(
                    text = category.label,
                    selected = draft.category == category,
                    onClick = { vm.updateDraft { it.copy(category = category) } },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                Field("Prep time (min)", draft.prepMinutes.toMinutesInput(), keyboard = KeyboardType.Number) { value ->
                    vm.updateDraft { it.copy(prepMinutes = value.filter(Char::isDigit).toIntOrNull() ?: 0) }
                }
            }
            Box(Modifier.weight(1f)) {
                Field("Cook time (min)", draft.cookMinutes.toMinutesInput(), keyboard = KeyboardType.Number) { value ->
                    vm.updateDraft { it.copy(cookMinutes = value.filter(Char::isDigit).toIntOrNull() ?: 0) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                Field("Total time (min)", draft.totalMinutes.toMinutesInput(), keyboard = KeyboardType.Number) { value ->
                    vm.updateDraft { it.copy(totalMinutes = value.filter(Char::isDigit).toIntOrNull() ?: 0) }
                }
            }
            Box(Modifier.weight(1f)) {
                Field("Servings", draft.baseServings.toString(), keyboard = KeyboardType.Number) { value ->
                    vm.updateDraft { it.copy(baseServings = value.filter(Char::isDigit).toIntOrNull() ?: 1) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun IngredientsSlide(draft: RecipeDraft, vm: AddRecipeViewModel) {
    Column(Modifier.fillMaxSize()) {
        SectionHeader("Ingredients", count = draft.ingredients.size) {
            vm.updateDraft { d ->
                d.copy(ingredients = d.ingredients + DraftIngredient("", 1.0, "cup", 120.0, "g"))
            }
        }
        if (draft.ingredients.isEmpty()) {
            Text("No ingredients detected yet. Tap Add, or pull lines back from Notes on the next slide.", color = Taupe, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            itemsIndexed(draft.ingredients, key = { index, item -> "$index-${item.name}" }) { index, item ->
                IngredientEditor(
                    item = item,
                    onChange = { updated ->
                        vm.updateDraft { d ->
                            d.copy(ingredients = d.ingredients.toMutableList().also { it[index] = updated })
                        }
                    },
                    onDelete = {
                        vm.updateDraft { d ->
                            d.copy(ingredients = d.ingredients.toMutableList().also { it.removeAt(index) })
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BoundarySlide(draft: RecipeDraft, vm: AddRecipeViewModel) {
    val noteLines = IngredientNotesBoundary.noteLines(draft.notes)
    val total = IngredientNotesBoundary.combinedLineCount(draft.ingredients, draft.notes)
    val cut = draft.ingredients.size.coerceIn(0, total)
    Column(Modifier.fillMaxSize()) {
        Text(
            "Lines above the cut stay ingredients. Lines below become Notes.",
            color = Taupe,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(10.dp))
        if (total == 0) {
            Text("Nothing to split yet. Add ingredients first.", color = Taupe, fontSize = 13.sp)
            return
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cut after ingredient $cut of $total", color = Espresso, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            CircleShift(
                enabled = cut > 0,
                label = "Move last ingredient into notes",
                icon = Icons.Outlined.KeyboardArrowUp,
            ) { vm.setIngredientNotesCut(cut - 1) }
            Spacer(Modifier.width(8.dp))
            CircleShift(
                enabled = cut < total,
                label = "Move first note into ingredients",
                icon = Icons.Outlined.KeyboardArrowDown,
            ) { vm.setIngredientNotesCut(cut + 1) }
        }
        if (total > 1) {
            Slider(
                value = cut.toFloat(),
                onValueChange = { vm.setIngredientNotesCut(it.roundToInt()) },
                valueRange = 0f..total.toFloat(),
                steps = (total - 1).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = Terracotta,
                    activeTrackColor = Terracotta,
                    inactiveTrackColor = Clay,
                ),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            itemsIndexed(draft.ingredients) { index, item ->
                BoundaryRow(
                    index = index + 1,
                    text = IngredientNotesBoundary.ingredientLineText(item),
                    kind = "Ingredient",
                    onClick = { vm.setIngredientNotesCut(index + 1) },
                )
            }
            item {
                CutBar(cut = cut, total = total)
            }
            if (noteLines.isEmpty()) {
                item {
                    Text("No notes yet. Raise the cut to send leftover ingredient lines here.", color = Taupe, fontSize = 13.sp)
                }
            } else {
                itemsIndexed(noteLines) { index, line ->
                    BoundaryRow(
                        index = cut + index + 1,
                        text = line,
                        kind = "Notes",
                        onClick = { vm.setIngredientNotesCut(cut + index + 1) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CutBar(cut: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Terracotta)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(2.dp).background(Ivory.copy(alpha = 0.5f)))
        Text(
            "  CUT  ",
            color = Ivory,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
        )
        Box(Modifier.weight(1f).height(2.dp).background(Ivory.copy(alpha = 0.5f)))
        Spacer(Modifier.width(8.dp))
        Text("$cut / $total", color = Ivory, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BoundaryRow(index: Int, text: String, kind: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ivory)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text("$kind $index", color = Taupe, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
        Text(text, color = Espresso, fontSize = 15.sp, lineHeight = 20.sp)
        Text("Tap to cut after this line", color = Taupe, fontSize = 11.sp)
    }
}

@Composable
private fun CircleShift(
    enabled: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) Clay else Clay.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) Terracotta else Taupe)
    }
}

@Composable
private fun FinishSlide(draft: RecipeDraft, vm: AddRecipeViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader("Directions", count = draft.directions.size) {
            vm.updateDraft { d -> d.copy(directions = d.directions + "") }
        }
        if (draft.directions.isEmpty()) {
            Text("No steps detected yet. Tap Add to write them.", color = Taupe, fontSize = 13.sp)
        }
        draft.directions.forEachIndexed { index, step ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    Field("Step ${index + 1}", step, singleLine = false) { value ->
                        vm.updateDraft { d ->
                            d.copy(directions = d.directions.toMutableList().also { it[index] = value })
                        }
                    }
                }
                DeleteControl("Delete step ${index + 1}") {
                    vm.updateDraft { d ->
                        d.copy(directions = d.directions.toMutableList().also { it.removeAt(index) })
                    }
                }
            }
        }
        Field("Notes", draft.notes, singleLine = false) { value ->
            vm.updateDraft { it.copy(notes = value) }
        }
        Spacer(Modifier.height(8.dp))
    }
}
