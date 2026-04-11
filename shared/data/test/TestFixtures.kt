package io.github.smithjustinn.blackjack.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/**
 * In-memory [DataStore] fake backed by a [MutableStateFlow].
 *
 * Suitable for use in all `shared/data` tests that need to drive [DataStore] emissions
 * without touching the filesystem. Thread-safe — [updateData] uses [MutableStateFlow.update].
 *
 * Usage:
 * ```
 * val fakeDataStore = FakeDataStore()
 * val repository = DataStoreSettingsRepository(fakeDataStore)
 * fakeDataStore.emitPreferences(somePrefs)
 * ```
 */
class FakeDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        state.update { transform(it) }
        return state.value
    }

    /** Directly pushes [prefs] into the flow, simulating an external write to disk. */
    fun emitPreferences(prefs: Preferences) {
        state.value = prefs
    }
}

/**
 * A [DataStore] fake whose [data] flow throws immediately, simulating a corrupted or
 * inaccessible DataStore file.
 *
 * Use this to verify that implementations handle read errors gracefully (e.g. by
 * falling back to a default value).
 */
class ExceptionFakeDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> =
        flow {
            throw RuntimeException("DataStore read error")
        }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        emptyPreferences()
}
