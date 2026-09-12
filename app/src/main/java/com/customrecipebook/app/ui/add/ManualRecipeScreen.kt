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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
            Text("Recipe details", color = Espresso, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Field("Title", draft.title) { vm.updateDraft { d -> d.copy(title = it) } }
            Field("Short description", draft.subtitle) { vm.updateDraft { d -> d.copy(subtitle = it) } }
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
                        "Minutes",
                        draft.minutes.toString(),
                        keyboard = KeyboardType.Number,
                    ) { value ->
                        vm.updateDraft { it.copy(minutes = value.filter(Char::isDigit).toIntOrNull() ?: 0) }
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
            Field("Servings label", draft.servingsUnit) { value -> vm.updateDraft { it.copy(servingsUnit = value) } }
            Field("Difficulty", draft.difficulty) { value -> vm.updateDraft { it.copy(difficulty = value) } }

            SectionHeader("Ingredients") {
                vm.updateDraft { d ->
                    d.copy(ingredients = d.ingredients + DraftIngredient("", 1.0, "cup", 120.0, "g"))
                }
            }
            draft.ingredients.forEachIndexed { index, item ->
                IngredientEditor(item) { updated ->
                    vm.updateDraft { d ->
                        d.copy(ingredients = d.ingredients.toMutableList().also { it[index] = updated })
                    }
                }
            }

            SectionHeader("Directions") {
                vm.updateDraft { d -> d.copy(directions = d.directions + "") }
            }
            draft.directions.forEachIndexed { index, step ->
                Field("Step ${index + 1}", step, singleLine = false) { value ->
                    vm.updateDraft { d ->
                        d.copy(directions = d.directions.toMutableList().also { it[index] = value })
                    }
                }
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
            Text("Save recipe", color = Ivory, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = Espresso, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Clay)
                .clickable(onClick = onAdd)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = Terracotta)
            Text("Add", color = Terracotta, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun IngredientEditor(item: DraftIngredient, onChange: (DraftIngredient) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Ivory)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Field("Name", item.name, compact = true) { onChange(item.copy(name = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                Field("US qty", item.quantityUs.toPlain(), keyboard = KeyboardType.Decimal, compact = true) {
                    onChange(item.copy(quantityUs = it.toDoubleOrNull() ?: 0.0))
                }
            }
            Box(Modifier.weight(1f)) {
                Field("US unit", item.unitUs, compact = true) { onChange(item.copy(unitUs = it)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                Field("Metric qty", item.quantityMetric.toPlain(), keyboard = KeyboardType.Decimal, compact = true) {
                    onChange(item.copy(quantityMetric = it.toDoubleOrNull() ?: 0.0))
                }
            }
            Box(Modifier.weight(1f)) {
                Field("Metric unit", item.unitMetric, compact = true) { onChange(item.copy(unitMetric = it)) }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    compact: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Terracotta,
            unfocusedBorderColor = Clay,
            focusedLabelColor = Terracotta,
            unfocusedLabelColor = Taupe,
            focusedContainerColor = Ivory,
            unfocusedContainerColor = Ivory,
            focusedTextColor = Espresso,
            unfocusedTextColor = Espresso,
        ),
        minLines = if (singleLine) 1 else 2,
    )
}

private fun Double.toPlain(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()
