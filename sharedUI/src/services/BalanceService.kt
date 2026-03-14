package io.github.smithjustinn.blackjack.services

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import io.github.smithjustinn.blackjack.data.createDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

fun createBalanceService(): BalanceService = BalanceService(createDataStore())

class BalanceService internal constructor(
    private val dataStore: DataStore<Preferences>
) {
    val balanceFlow: Flow<Int> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs -> prefs[BALANCE_KEY] ?: DEFAULT_BALANCE }

    suspend fun saveBalance(balance: Int) {
        dataStore.edit { prefs -> prefs[BALANCE_KEY] = balance }
    }

    suspend fun resetBalance() {
        dataStore.edit { prefs -> prefs[BALANCE_KEY] = DEFAULT_BALANCE }
    }

    companion object {
        private val BALANCE_KEY = intPreferencesKey("balance")
        const val DEFAULT_BALANCE = 1000
    }
}
