package io.github.smithjustinn.blackjack

import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.services.JvmAudioServiceImpl
import io.github.smithjustinn.blackjack.services.NoOpHapticsService
import org.koin.dsl.module

val desktopModule =
    module {
        single<AudioService> { JvmAudioServiceImpl(get()) }
        single<HapticsService> { NoOpHapticsService }
    }
