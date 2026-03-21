package io.github.smithjustinn.blackjack

import android.content.Context
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.data.BalanceService
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.data.createBalanceService
import io.github.smithjustinn.blackjack.data.createSettingsRepository
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.services.AndroidAudioServiceImpl
import io.github.smithjustinn.blackjack.services.AndroidHapticsServiceImpl
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class AndroidAppGraph(
    private val context: Context
) : AppGraph {
    override val logger: Logger by lazy { Logger.withTag("Blackjack") }
    override val audioService: AudioService by lazy { AndroidAudioServiceImpl(context, logger) }
    override val hapticsService: HapticsService by lazy { AndroidHapticsServiceImpl(context) }
    override val balanceService: BalanceService by lazy { createBalanceService() }
    override val settingsRepository: SettingsRepository by lazy { createSettingsRepository() }
    override val coroutineDispatchers: CoroutineDispatchers by lazy { CoroutineDispatchers() }
    override val applicationScope: CoroutineScope by lazy { MainScope() }
}
