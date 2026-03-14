package io.github.smithjustinn.blackjack.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val SETTINGS_KEY = stringPreferencesKey("app_settings")

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                prefs[SETTINGS_KEY]?.let {
                    runCatching { Json.decodeFromString<AppSettings>(it) }.getOrNull()
                } ?: AppSettings()
            }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val current = prefs[SETTINGS_KEY]
                ?.let { runCatching { Json.decodeFromString<AppSettings>(it) }.getOrNull() }
                ?: AppSettings()
            prefs[SETTINGS_KEY] = Json.encodeToString(transform(current))
        }
    }
}

fun createSettingsRepository(): SettingsRepository =
    DataStoreSettingsRepository(createDataStore())
