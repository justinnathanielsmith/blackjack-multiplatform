package io.github.smithjustinn.blackjack

import android.content.Context
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.data.BalanceService
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.data.createBalanceService
import io.github.smithjustinn.blackjack.data.createSettingsRepository
import io.github.smithjustinn.blackjack.services.AndroidAudioServiceImpl
import io.github.smithjustinn.blackjack.services.AndroidHapticsServiceImpl
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import org.koin.dsl.module

fun androidModule(context: Context) =
    module {
        single<Logger> { Logger.withTag("Blackjack") }
        single<AudioService> { AndroidAudioServiceImpl(context, get()) }
        single<HapticsService> { AndroidHapticsServiceImpl(context) }
        single<BalanceService> { createBalanceService() }
        single<SettingsRepository> { createSettingsRepository() }
    }
