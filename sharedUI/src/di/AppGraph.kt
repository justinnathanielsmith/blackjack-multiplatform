package io.github.smithjustinn.blackjack.di

import androidx.compose.runtime.staticCompositionLocalOf
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.data.AppSettings
import io.github.smithjustinn.blackjack.data.BalanceService
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.services.NoOpHapticsService
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow

interface AppGraph {
    val logger: Logger
    val audioService: AudioService
    val hapticsService: HapticsService
    val balanceService: BalanceService
    val settingsRepository: SettingsRepository
    val coroutineDispatchers: CoroutineDispatchers
    val applicationScope: CoroutineScope
}

/**
 * A no-op implementation of [AppGraph] for Compose Previews.
 */
object PreviewAppGraph : AppGraph {
    override val logger = Logger.withTag("Preview")
    override val hapticsService = NoOpHapticsService
    override val coroutineDispatchers = CoroutineDispatchers()
    override val applicationScope = MainScope()

    override val audioService =
        object : AudioService {
            override var isMuted = true

            override fun playEffect(effect: AudioService.SoundEffect) = Unit

            override fun release() = Unit
        }

    override val balanceService: BalanceService
        get() = error("BalanceService not available in Preview")

    override val settingsRepository =
        object : SettingsRepository {
            override val settingsFlow = MutableStateFlow(AppSettings())

            override suspend fun update(transform: (AppSettings) -> AppSettings) = Unit
        }
}

val LocalAppGraph =
    staticCompositionLocalOf<AppGraph> {
        PreviewAppGraph
    }
