package com.customrecipebook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.customrecipebook.app.ui.navigation.RecipebookNav
import com.customrecipebook.app.ui.theme.CustomRecipebookTheme
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.Sand

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Sand.toArgb(), Sand.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Ivory.toArgb(), Ivory.toArgb()),
        )
        setContent {
            CustomRecipebookTheme {
                RecipebookNav()
            }
        }
    }
}
