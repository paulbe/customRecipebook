package com.customrecipebook.app.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.customrecipebook.app.RecipebookApplication
import com.customrecipebook.app.data.UnitSystem
import com.customrecipebook.app.domain.QuantityFormatter
import com.customrecipebook.app.ui.components.CardShape
import com.customrecipebook.app.ui.theme.Clay
import com.customrecipebook.app.ui.theme.Espresso
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.Sand
import com.customrecipebook.app.ui.theme.Taupe
import com.customrecipebook.app.ui.theme.Terracotta

@Composable
fun ShoppingScreen(app: RecipebookApplication, modifier: Modifier = Modifier) {
    val checked by app.container.repository.checkedIngredients.collectAsStateWithLifecycle(emptyList())
    val units by app.container.preferences.unitSystem.collectAsStateWithLifecycle(UnitSystem.US)

    Column(
        modifier
            .fillMaxSize()
            .background(Sand)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text("Shopping", color = Espresso, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("A list from the ingredients you check off.", color = Taupe, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        if (checked.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Ivory)
                    .padding(24.dp),
            ) {
                Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = Terracotta)
                Spacer(Modifier.height(12.dp))
                Text("Nothing on the list yet", color = Espresso, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Open a recipe and tap ingredient rows to check them off. Those items will gather here in a later shopping pass — for now this is a preview of what you have marked.",
                    color = Taupe,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        } else {
            checked.forEach { recipe ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(CardShape)
                        .background(Ivory)
                        .padding(18.dp),
                ) {
                    Text(recipe.title, color = Espresso, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    recipe.ingredients.forEach { ingredient ->
                        Text(
                            QuantityFormatter.ingredientLine(ingredient, units, scale = 1),
                            color = Espresso,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                    }
                }
            }
            Text(
                "Full shopping lists, aisle grouping, and sharing land in a later version.",
                color = Taupe,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(CardShape)
                    .background(Clay)
                    .padding(16.dp),
            )
        }
    }
}
