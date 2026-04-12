package io.github.smithjustinn.blackjack.domain

import io.github.smithjustinn.blackjack.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}
