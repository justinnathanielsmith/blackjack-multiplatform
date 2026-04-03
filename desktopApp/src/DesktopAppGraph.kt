package io.github.smithjustinn.blackjack

import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.data.BalanceService
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.data.createBalanceService
import io.github.smithjustinn.blackjack.data.createSettingsRepository
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.services.JvmAudioServiceImpl
import io.github.smithjustinn.blackjack.services.NoOpHapticsService
import org.koin.dsl.module

val desktopModule = module {
    single<Logger> { Logger.withTag("Blackjack") }
    single<AudioService> { JvmAudioServiceImpl(get()) }
    single<HapticsService> { NoOpHapticsService }
    single<BalanceService> { createBalanceService() }
    single<SettingsRepository> { createSettingsRepository() }
}
