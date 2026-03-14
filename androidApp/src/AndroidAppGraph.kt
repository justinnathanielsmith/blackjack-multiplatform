package io.github.smithjustinn.blackjack

import android.content.Context
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.services.AndroidAudioServiceImpl
import io.github.smithjustinn.blackjack.services.AndroidHapticsServiceImpl
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.BalanceService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.services.createBalanceService
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class AndroidAppGraph(
    context: Context
) : AppGraph {
    override val logger: Logger = Logger.withTag("Blackjack")
    override val audioService: AudioService = AndroidAudioServiceImpl(context, logger)
    override val hapticsService: HapticsService = AndroidHapticsServiceImpl(context)
    override val balanceService: BalanceService = createBalanceService()
    override val coroutineDispatchers: CoroutineDispatchers = CoroutineDispatchers()
    override val applicationScope: CoroutineScope = MainScope()
}
