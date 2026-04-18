package com.luminapdf.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lumina_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_BLUE_LIGHT_FILTER = floatPreferencesKey("blue_light_filter")   // 0f – 1f
        val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")                // 2 or 3
        val KEY_SORT_ORDER = stringPreferencesKey("sort_order")                 // "recent" | "name"
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DARK_MODE] ?: false }

    val blueLightFilter: Flow<Float> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_BLUE_LIGHT_FILTER] ?: 0f }

    val gridColumns: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_GRID_COLUMNS] ?: 2 }

    val sortOrder: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_SORT_ORDER] ?: "recent" }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setBlueLightFilter(value: Float) {
        context.dataStore.edit { it[KEY_BLUE_LIGHT_FILTER] = value.coerceIn(0f, 1f) }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[KEY_GRID_COLUMNS] = columns }
    }

    suspend fun setSortOrder(order: String) {
        context.dataStore.edit { it[KEY_SORT_ORDER] = order }
    }
}
