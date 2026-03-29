package io.github.smithjustinn.blackjack.ui

import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.data.BalanceService
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.data.createBalanceService
import io.github.smithjustinn.blackjack.data.createSettingsRepository
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.IosAudioServiceImpl
import io.github.smithjustinn.blackjack.services.IosHapticsServiceImpl
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

// Platform-specific AppGraph for iOS — mirrors AndroidAppGraph / DesktopAppGraph pattern
class IosAppGraph : AppGraph {
    override val logger: Logger by lazy { Logger.withTag("Blackjack") }
    override val audioService: AudioService by lazy { IosAudioServiceImpl(logger) }
    override val hapticsService by lazy { IosHapticsServiceImpl() }
    override val balanceService: BalanceService by lazy { createBalanceService() }
    override val settingsRepository: SettingsRepository by lazy { createSettingsRepository() }
    override val coroutineDispatchers: CoroutineDispatchers by lazy { CoroutineDispatchers() }
    override val applicationScope: CoroutineScope by lazy { MainScope() }
}
