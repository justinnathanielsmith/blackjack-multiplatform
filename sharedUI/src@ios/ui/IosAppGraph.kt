package io.github.smithjustinn.blackjack.ui

import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.data.BalanceService
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.data.createBalanceService
import io.github.smithjustinn.blackjack.data.createSettingsRepository
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.services.IosAudioServiceImpl
import io.github.smithjustinn.blackjack.services.IosHapticsServiceImpl
import org.koin.dsl.module

val iosModule = module {
    single<Logger> { Logger.withTag("Blackjack") }
    single<AudioService> { IosAudioServiceImpl(get()) }
    single<HapticsService> { IosHapticsServiceImpl() }
    single<BalanceService> { createBalanceService() }
    single<SettingsRepository> { createSettingsRepository() }
}
