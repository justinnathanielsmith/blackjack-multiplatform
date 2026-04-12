package io.github.smithjustinn.blackjack.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DataStoreSettingsRepositoryTest {
    @Test
    fun testDecodeErrorReturnsDefaultFlow() =
        runTest {
            val fakeDataStore = FakeDataStore()
            val repository = DataStoreSettingsRepository(fakeDataStore)

            repository.settingsFlow.test {
                // Ensure default settings on empty store
                assertEquals(AppSettings(), awaitItem())

                // Create preferences with malformed JSON
                val settingsKey = stringPreferencesKey("app_settings")
                val malformedPrefs = mutablePreferencesOf(settingsKey to "{ bad json ]")

                fakeDataStore.emitPreferences(malformedPrefs)

                // It should catch the decoding exception and return the default AppSettings()
                assertEquals(AppSettings(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun testDecodeErrorReturnsDefaultOnUpdate() =
        runTest {
            val fakeDataStore = FakeDataStore()
            val repository = DataStoreSettingsRepository(fakeDataStore)

            repository.settingsFlow.test {
                assertEquals(AppSettings(), awaitItem())

                // Set bad data
                val settingsKey = stringPreferencesKey("app_settings")
                val malformedPrefs = mutablePreferencesOf(settingsKey to "{ bad json ]")
                fakeDataStore.emitPreferences(malformedPrefs)

                // The emission of bad preferences should trigger the fallback to default in the flow
                assertEquals(AppSettings(), awaitItem())

                // Try to update it - it should fallback to default before updating
                repository.update { it.copy(isSoundMuted = true) }

                // The stored value should now be a valid JSON string of the updated default
                val updatedSettings = awaitItem()
                assertEquals(true, updatedSettings.isSoundMuted)

                // Verify the underlying storage as well
                fakeDataStore.data.test {
                    val p = awaitItem()
                    val storedString = p[settingsKey]
                    // Prove it's no longer the bad json
                    assertEquals(true, storedString?.contains("isSoundMuted\":true"))
                    cancelAndIgnoreRemainingEvents()
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun testValidJsonIsDecoded() =
        runTest {
            val fakeDataStore = FakeDataStore()
            val repository = DataStoreSettingsRepository(fakeDataStore)

            repository.settingsFlow.test {
                assertEquals(AppSettings(), awaitItem())

                // Set valid JSON data
                val settingsKey = stringPreferencesKey("app_settings")
                val validJson =
                    """{"isSoundMuted":true,"gameRules":{"dealerHitsSoft17":true,""" +
                        """"allowDoubleAfterSplit":true,"allowSurrender":false,""" +
                        """"blackjackPayout":"THREE_TO_TWO","deckCount":6},""" +
                        """"defaultHandCount":2,"isAutoDealEnabled":false}"""
                val validPrefs = mutablePreferencesOf(settingsKey to validJson)
                fakeDataStore.emitPreferences(validPrefs)

                val settings = awaitItem()
                assertEquals(true, settings.isSoundMuted)
                assertEquals(2, settings.defaultHandCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun testPartialUpdatePreservesAllOtherFields() =
        runTest {
            val fakeDataStore = FakeDataStore()
            val repository = DataStoreSettingsRepository(fakeDataStore)

            repository.settingsFlow.test {
                assertEquals(AppSettings(), awaitItem())

                val settingsKey = stringPreferencesKey("app_settings")
                val initialJson =
                    """{"isSoundMuted":false,"gameRules":{"dealerHitsSoft17":true,""" +
                        """"allowDoubleAfterSplit":true,"allowSurrender":true,""" +
                        """"blackjackPayout":"THREE_TO_TWO","deckCount":6,"splitOnValueOnly":false},""" +
                        """"defaultHandCount":3,"isAutoDealEnabled":true}"""
                fakeDataStore.emitPreferences(mutablePreferencesOf(settingsKey to initialJson))

                val initialSettings = awaitItem()
                assertEquals(false, initialSettings.isSoundMuted)
                assertEquals(true, initialSettings.isAutoDealEnabled)
                assertEquals(3, initialSettings.defaultHandCount)
                assertEquals(true, initialSettings.gameRules.allowSurrender)

                repository.update { it.copy(isSoundMuted = true) }

                val updatedSettings = awaitItem()
                assertEquals(true, updatedSettings.isSoundMuted)
                assertEquals(true, updatedSettings.isAutoDealEnabled)
                assertEquals(3, updatedSettings.defaultHandCount)
                assertEquals(true, updatedSettings.gameRules.allowSurrender)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
