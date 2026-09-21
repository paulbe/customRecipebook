package com.customrecipebook.app.ui.add

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.customrecipebook.app.RecipebookApplication
import com.customrecipebook.app.data.DraftIngredient
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.data.repo.RecipeRepository
import com.customrecipebook.app.ui.components.BackCircle
import com.customrecipebook.app.ui.components.SoftChip
import com.customrecipebook.app.ui.theme.Clay
import com.customrecipebook.app.ui.theme.Espresso
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.Sand
import com.customrecipebook.app.ui.theme.Taupe
import com.customrecipebook.app.ui.theme.Terracotta

@Composable
fun ManualRecipeScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    app: RecipebookApplication,
) {
    val activity = LocalContext.current as ComponentActivity
    val vm: AddRecipeViewModel = viewModel(activity, factory = app.container.factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val draft = state.draft
    val pdfImport = draft.source == ImportSource.PDF && state.editingRecipeId == null

    Column(
        Modifier
            .fillMaxSize()
            .background(Sand)
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackCircle(onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                when {
                    state.editingRecipeId != null -> "Edit recipe"
                    pdfImport -> "Review PDF"
                    else -> "Recipe details"
                },
                color = Espresso,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (pdfImport) {
            Box(Modifier.padding(horizontal = 20.dp).weight(1f)) {
                PdfImportWizard(draft = draft, vm = vm, onSaved = onSaved)
            }
        } else {
            SinglePageEditor(state = state, vm = vm, onSaved = onSaved)
        }
    }
}

@Composable
private fun SinglePageEditor(
    state: AddRecipeUiState,
    vm: AddRecipeViewModel,
    onSaved: (String) -> Unit,
) {
    val draft = state.draft
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
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
                    Text(
                        when (draft.source) {
                            ImportSource.PDF -> "Attached PDF"
                            ImportSource.CAMERA -> "Attached photo"
                            else -> "Attachment"
                        },
                        color = Taupe,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
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
                    Field(
                        "Prep time (min)",
                        draft.prepMinutes.toMinutesInput(),
                        keyboard = KeyboardType.Number,
                    ) { value ->
                        vm.updateDraft { it.copy(prepMinutes = value.filter(Char::isDigit).toIntOrNull() ?: 0) }
                    }
                }
                Box(Modifier.weight(1f)) {
                    Field(
                        "Cook time (min)",
                        draft.cookMinutes.toMinutesInput(),
                        keyboard = KeyboardType.Number,
                    ) { value ->
                        vm.updateDraft { it.copy(cookMinutes = value.filter(Char::isDigit).toIntOrNull() ?: 0) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    Field(
                        "Total time (min)",
                        draft.totalMinutes.toMinutesInput(),
                        keyboard = KeyboardType.Number,
                    ) { value ->
                        vm.updateDraft { it.copy(totalMinutes = value.filter(Char::isDigit).toIntOrNull() ?: 0) }
                    }
                }
                Box(Modifier.weight(1f)) {
                    Field(
                        "Servings",
                        draft.baseServings.toString(),
                        keyboard = KeyboardType.Number,
                    ) { value ->
                        vm.updateDraft { it.copy(baseServings = value.filter(Char::isDigit).toIntOrNull() ?: 1) }
                    }
                }
            }

            SectionHeader("Ingredients", count = draft.ingredients.size) {
                vm.updateDraft { d ->
                    d.copy(ingredients = d.ingredients + DraftIngredient("", 1.0, "cup", 120.0, "g"))
                }
            }
            if (draft.ingredients.isEmpty()) {
                Text("No ingredients detected yet. Tap Add to enter them.", color = Taupe, fontSize = 13.sp)
            }
            draft.ingredients.forEachIndexed { index, item ->
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
            Field(
                "Notes",
                draft.notes,
                singleLine = false,
            ) { value ->
                vm.updateDraft { it.copy(notes = value) }
            }
            Spacer(Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Terracotta)
                .clickable { vm.save(onSaved) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (state.editingRecipeId != null) "Save changes" else "Save recipe",
                color = Ivory,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}
