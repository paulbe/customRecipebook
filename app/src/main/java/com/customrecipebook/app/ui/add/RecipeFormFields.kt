package com.customrecipebook.app.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customrecipebook.app.data.DraftIngredient
import com.customrecipebook.app.data.UnitSystem
import com.customrecipebook.app.domain.QuantityFormatter
import com.customrecipebook.app.ui.theme.Clay
import com.customrecipebook.app.ui.theme.Espresso
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.Taupe
import com.customrecipebook.app.ui.theme.Terracotta

@Composable
internal fun SectionHeader(title: String, count: Int = 0, onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            if (count > 0) "$title ($count)" else title,
            color = Espresso,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
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
internal fun IngredientEditor(
    item: DraftIngredient,
    onChange: (DraftIngredient) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Ivory)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    QuantityFormatter.ingredientLine(item, UnitSystem.US),
                    color = Espresso,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                val usPresent = item.quantityUs > 0.0 || item.unitUs.isNotBlank()
                val metricPresent = item.quantityMetric > 0.0 || item.unitMetric.isNotBlank()
                if (usPresent && metricPresent) {
                    Text(
                        QuantityFormatter.ingredientLine(item, UnitSystem.METRIC),
                        color = Taupe,
                        fontSize = 13.sp,
                    )
                }
            }
            DeleteControl("Delete ingredient") { onDelete() }
        }
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
internal fun DeleteControl(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(Clay)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = label, tint = Terracotta)
    }
}

@Composable
internal fun Field(
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

internal fun Double.toPlain(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

internal fun Int.toMinutesInput(): String = if (this > 0) toString() else ""
