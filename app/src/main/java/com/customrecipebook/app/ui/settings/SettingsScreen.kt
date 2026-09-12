package com.customrecipebook.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.customrecipebook.app.BuildConfig
import com.customrecipebook.app.RecipebookApplication
import com.customrecipebook.app.data.UnitSystem
import com.customrecipebook.app.ui.components.BrandMark
import com.customrecipebook.app.ui.components.CardShape
import com.customrecipebook.app.ui.components.SoftChip
import com.customrecipebook.app.ui.theme.Espresso
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.Sand
import com.customrecipebook.app.ui.theme.Taupe
import com.customrecipebook.app.ui.theme.Terracotta
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: RecipebookApplication, modifier: Modifier = Modifier) {
    val units by app.container.preferences.unitSystem.collectAsStateWithLifecycle(UnitSystem.US)
    val scope = rememberCoroutineScope()

    Column(
        modifier
            .fillMaxSize()
            .background(Sand)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text("Settings", color = Espresso, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("How Custom Recipebook should measure and remember.", color = Taupe, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(Ivory)
                .padding(20.dp),
        ) {
            Text("Default units", color = Espresso, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Used on every recipe. You can still switch on the detail screen.", color = Taupe, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Row {
                SoftChip("US", selected = units == UnitSystem.US) {
                    scope.launch { app.container.preferences.setUnitSystem(UnitSystem.US) }
                }
                Spacer(Modifier.padding(4.dp))
                SoftChip("Metric", selected = units == UnitSystem.METRIC) {
                    scope.launch { app.container.preferences.setUnitSystem(UnitSystem.METRIC) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(Ivory)
                .clickable { scope.launch { app.container.repository.clearChecklists() } }
                .padding(20.dp),
        ) {
            Text("Clear ingredient checks", color = Espresso, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Uncheck every row across your recipes.", color = Taupe, fontSize = 13.sp)
        }

        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(Ivory)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 36)
                Spacer(Modifier.padding(8.dp))
                Column {
                    Text("Custom Recipebook", color = Espresso, fontWeight = FontWeight.SemiBold)
                    Text("Version ${BuildConfig.VERSION_NAME}", color = Taupe, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "A warm pocket cookbook for recipes you already love — typed by hand, pulled from a PDF, or snapped from a page.",
                color = Taupe,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "PDF import copies the file and reads a text layer when one exists. Camera photos attach to an editable form.",
                color = Terracotta,
                fontSize = 13.sp,
            )
        }
    }
}
