package com.freegymtrainer.app.di

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** App-wide preferences that don't belong in Room: theme mode and unit system. */
class SettingsDataStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")       // "system" | "light" | "dark"
    private val unitsKey = stringPreferencesKey("units")            // "metric" | "imperial"
    private val disclaimerSeenKey = booleanPreferencesKey("disclaimer_seen")

    val themeMode: Flow<String> = context.dataStore.data.map { it[themeKey] ?: "system" }
    val units: Flow<String> = context.dataStore.data.map { it[unitsKey] ?: "metric" }
    val disclaimerSeen: Flow<Boolean> = context.dataStore.data.map { it[disclaimerSeenKey] ?: false }

    suspend fun setThemeMode(mode: String) { context.dataStore.edit { it[themeKey] = mode } }
    suspend fun setUnits(units: String) { context.dataStore.edit { it[unitsKey] = units } }
    suspend fun setDisclaimerSeen(seen: Boolean) { context.dataStore.edit { it[disclaimerSeenKey] = seen } }
}
