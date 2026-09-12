package com.customrecipebook.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.customrecipebook.app.data.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "custom_recipebook")

class UserPreferences(private val context: Context) {
    private val unitKey = stringPreferencesKey("unit_system")

    val unitSystem: Flow<UnitSystem> = context.dataStore.data.map { prefs ->
        runCatching { UnitSystem.valueOf(prefs[unitKey] ?: UnitSystem.US.name) }
            .getOrDefault(UnitSystem.US)
    }

    suspend fun setUnitSystem(system: UnitSystem) {
        context.dataStore.edit { it[unitKey] = system.name }
    }
}
