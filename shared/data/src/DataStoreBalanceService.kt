package io.github.smithjustinn.blackjack.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// DataStore-backed impl; inject BalanceService interface for testability (mirrors SettingsRepository pattern)
class DataStoreBalanceService internal constructor(
    private val dataStore: DataStore<Preferences>,
) : BalanceService {
    override val balanceFlow: Flow<Int> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs -> prefs[BALANCE_KEY] ?: BalanceService.DEFAULT_BALANCE }

    override suspend fun saveBalance(balance: Int) {
        dataStore.edit { prefs -> prefs[BALANCE_KEY] = balance }
    }

    override suspend fun resetBalance() {
        dataStore.edit { prefs -> prefs[BALANCE_KEY] = BalanceService.DEFAULT_BALANCE }
    }

    companion object {
        private val BALANCE_KEY = intPreferencesKey("balance")
    }
}

fun createBalanceService(): BalanceService = DataStoreBalanceService(createDataStore())
