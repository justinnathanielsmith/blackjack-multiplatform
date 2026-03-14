package io.github.smithjustinn.blackjack

import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.data.createSettingsRepository
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.BalanceService
import io.github.smithjustinn.blackjack.services.JvmAudioServiceImpl
import io.github.smithjustinn.blackjack.services.NoOpHapticsService
import io.github.smithjustinn.blackjack.services.createBalanceService
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class DesktopAppGraph : AppGraph {
    override val logger: Logger = Logger.withTag("Blackjack")
    override val audioService: AudioService = JvmAudioServiceImpl(logger)
    override val hapticsService = NoOpHapticsService
    override val balanceService: BalanceService = createBalanceService()
    override val settingsRepository: SettingsRepository = createSettingsRepository()
    override val coroutineDispatchers: CoroutineDispatchers = CoroutineDispatchers()
    override val applicationScope: CoroutineScope = MainScope()
}
