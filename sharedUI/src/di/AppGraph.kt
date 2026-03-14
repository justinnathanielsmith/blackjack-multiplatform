package io.github.smithjustinn.blackjack.di

import androidx.compose.runtime.staticCompositionLocalOf
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.BalanceService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope

interface AppGraph {
    val logger: Logger
    val audioService: AudioService
    val hapticsService: HapticsService
    val balanceService: BalanceService
    val settingsRepository: SettingsRepository
    val coroutineDispatchers: CoroutineDispatchers
    val applicationScope: CoroutineScope
}

val LocalAppGraph =
    staticCompositionLocalOf<AppGraph> {
        error("No AppGraph provided")
    }
