package io.github.smithjustinn.blackjack.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DataStoreSettingsRepositoryTest {
    @Test
    fun testDecodeErrorReturnsDefaultFlow() =
        runTest {
            val fakeDataStore = FakeDataStore()
            val repository = DataStoreSettingsRepository(fakeDataStore)

            // Ensure default settings on empty store
            assertEquals(AppSettings(), repository.settingsFlow.first())

            // Create preferences with malformed JSON
            val settingsKey = stringPreferencesKey("app_settings")
            val malformedPrefs = mutablePreferencesOf(settingsKey to "{ bad json ]")

            fakeDataStore.emitPreferences(malformedPrefs)

            // It should catch the decoding exception and return the default AppSettings()
            assertEquals(AppSettings(), repository.settingsFlow.first())
        }

    @Test
    fun testDecodeErrorReturnsDefaultOnUpdate() =
        runTest {
            val fakeDataStore = FakeDataStore()
            val repository = DataStoreSettingsRepository(fakeDataStore)

            // Set bad data
            val settingsKey = stringPreferencesKey("app_settings")
            val malformedPrefs = mutablePreferencesOf(settingsKey to "{ bad json ]")
            fakeDataStore.emitPreferences(malformedPrefs)

            // Try to update it - it should fallback to default before updating
            repository.update { it.copy(isSoundMuted = true) }

            // The stored value should now be a valid JSON string of the updated default
            val prefs = fakeDataStore.data.first()
            val storedString = prefs[settingsKey]

            // Let's decode it correctly just to prove it was properly saved
            val updatedSettings = repository.settingsFlow.first()
            assertEquals(true, updatedSettings.isSoundMuted)
        }

    @Test
    fun testValidJsonIsDecoded() =
        runTest {
            val fakeDataStore = FakeDataStore()
            val repository = DataStoreSettingsRepository(fakeDataStore)

            // Set valid JSON data
            val settingsKey = stringPreferencesKey("app_settings")
            val validJson =
                """{"isSoundMuted":true,"gameRules":{"dealerHitsSoft17":true,""" +
                    """"allowDoubleAfterSplit":true,"allowSurrender":false,""" +
                    """"blackjackPayout":"THREE_TO_TWO","deckCount":6},""" +
                    """"defaultHandCount":2,"isAutoDealEnabled":false}"""
            val validPrefs = mutablePreferencesOf(settingsKey to validJson)
            fakeDataStore.emitPreferences(validPrefs)

            val settings = repository.settingsFlow.first()
            assertEquals(true, settings.isSoundMuted)
            assertEquals(2, settings.defaultHandCount)
        }
}
