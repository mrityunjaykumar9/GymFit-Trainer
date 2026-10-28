package com.freegymtrainer.app.di

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.freegymtrainer.app.domain.nutrition.DietGoal
import com.freegymtrainer.app.domain.nutrition.DietPrefs
import com.freegymtrainer.app.domain.nutrition.DietStyle
import com.freegymtrainer.app.domain.nutrition.FastingProtocol
import com.freegymtrainer.app.domain.nutrition.MacroStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** App-wide preferences that don't belong in Room: theme mode and unit system. */
class SettingsDataStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")       // "system" | "light" | "dark"
    private val unitsKey = stringPreferencesKey("units")            // "metric" | "imperial"
    private val disclaimerSeenKey = booleanPreferencesKey("disclaimer_seen")
    private val dietPreferenceKey = stringPreferencesKey("diet_preference") // legacy "Veg" | "Non-Veg"
    private val dietGoalKey = stringPreferencesKey("diet_goal")
    private val dietPaceKey = floatPreferencesKey("diet_pace")
    private val dietStyleKey = stringPreferencesKey("diet_style")
    private val dietMacroKey = stringPreferencesKey("diet_macro")
    private val dietMealsKey = intPreferencesKey("diet_meals")
    private val dietTargetWeightKey = floatPreferencesKey("diet_target_weight")
    private val fastingProtocolKey = stringPreferencesKey("fasting_protocol")
    private val fastingStartKey = intPreferencesKey("fasting_start_minute")
    private val shoppingCheckedKey = stringSetPreferencesKey("shopping_checked")

    // Defaults to dark rather than following the system theme, so a fresh install looks the
    // same for every user regardless of their phone's setting - System/Light remain available
    // as explicit opt-ins via the picker in Settings.
    val themeMode: Flow<String> = context.dataStore.data.map { it[themeKey] ?: "dark" }
    val units: Flow<String> = context.dataStore.data.map { it[unitsKey] ?: "metric" }
    val disclaimerSeen: Flow<Boolean> = context.dataStore.data.map { it[disclaimerSeenKey] ?: false }
    val dietPreference: Flow<String> = context.dataStore.data.map { it[dietPreferenceKey] ?: "Veg" }

    /** The diet plan the person set up, or null until they do (callers then derive defaults from their profile). */
    val savedDietPrefs: Flow<DietPrefs?> = context.dataStore.data.map { p ->
        val goal = p[dietGoalKey]?.let { n -> DietGoal.values().firstOrNull { it.name == n } } ?: return@map null
        DietPrefs(
            goal = goal,
            paceKgPerWeek = p[dietPaceKey] ?: 0f,
            style = p[dietStyleKey]?.let { n -> DietStyle.values().firstOrNull { it.name == n } } ?: DietStyle.fromLegacy(p[dietPreferenceKey] ?: "Veg"),
            macroStyle = p[dietMacroKey]?.let { n -> MacroStyle.values().firstOrNull { it.name == n } } ?: MacroStyle.BALANCED,
            mealsPerDay = (p[dietMealsKey] ?: 4).coerceIn(3, 5),
            targetWeightKg = p[dietTargetWeightKey]?.takeIf { it > 0f }
        )
    }

    suspend fun saveDietPrefs(prefs: DietPrefs) {
        context.dataStore.edit {
            it[dietGoalKey] = prefs.goal.name
            it[dietPaceKey] = prefs.paceKgPerWeek
            it[dietStyleKey] = prefs.style.name
            it[dietMacroKey] = prefs.macroStyle.name
            it[dietMealsKey] = prefs.mealsPerDay
            it[dietTargetWeightKey] = prefs.targetWeightKg ?: 0f
            it[dietPreferenceKey] = if (prefs.style == DietStyle.NON_VEG) "Non-Veg" else "Veg" // keep the legacy flag in step
        }
    }

    val fastingProtocol: Flow<FastingProtocol> = context.dataStore.data.map { FastingProtocol.fromName(it[fastingProtocolKey]) }
    /** Minutes after midnight when the eating window opens (default 12:00). */
    val fastingStartMinute: Flow<Int> = context.dataStore.data.map { it[fastingStartKey] ?: 12 * 60 }
    suspend fun setFastingProtocol(p: FastingProtocol) { context.dataStore.edit { it[fastingProtocolKey] = p.name } }
    suspend fun setFastingStartMinute(minute: Int) { context.dataStore.edit { it[fastingStartKey] = ((minute % 1440) + 1440) % 1440 } }

    val shoppingChecked: Flow<Set<String>> = context.dataStore.data.map { it[shoppingCheckedKey] ?: emptySet() }
    suspend fun setShoppingChecked(items: Set<String>) { context.dataStore.edit { it[shoppingCheckedKey] = items } }

    suspend fun setThemeMode(mode: String) { context.dataStore.edit { it[themeKey] = mode } }
    suspend fun setUnits(units: String) { context.dataStore.edit { it[unitsKey] = units } }
    suspend fun setDisclaimerSeen(seen: Boolean) { context.dataStore.edit { it[disclaimerSeenKey] = seen } }
    suspend fun setDietPreference(preference: String) { context.dataStore.edit { it[dietPreferenceKey] = preference } }
}
