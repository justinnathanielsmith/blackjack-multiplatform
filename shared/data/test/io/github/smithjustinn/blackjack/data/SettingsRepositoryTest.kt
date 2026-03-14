package io.github.smithjustinn.blackjack.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    private fun createRepository(name: String) = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(
            produceFile = {
                java.io.File.createTempFile(name, ".preferences_pb")
            }
        )
    )

    @Test
    fun default_settings_on_first_read() = runTest {
        val repo = createRepository("test1")
        val settings = repo.settingsFlow.first()
        assertEquals(AppSettings(), settings)
    }

    @Test
    fun update_sound_muted() = runTest {
        val repo = createRepository("test2")
        repo.update { it.copy(isSoundMuted = true) }
        val settings = repo.settingsFlow.first()
        assertTrue(settings.isSoundMuted)
    }
}
