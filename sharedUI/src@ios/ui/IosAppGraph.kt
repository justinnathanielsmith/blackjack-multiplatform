package io.github.smithjustinn.blackjack.ui

import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.services.IosAudioServiceImpl
import io.github.smithjustinn.blackjack.services.IosHapticsServiceImpl
import org.koin.dsl.module

val iosModule =
    module {
        single<AudioService> { IosAudioServiceImpl(get()) }
        single<HapticsService> { IosHapticsServiceImpl() }
    }
