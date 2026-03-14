package io.github.smithjustinn.blackjack.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}
