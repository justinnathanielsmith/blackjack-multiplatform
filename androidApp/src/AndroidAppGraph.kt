package io.github.smithjustinn.blackjack

import android.content.Context
import io.github.smithjustinn.blackjack.services.AndroidAudioServiceImpl
import io.github.smithjustinn.blackjack.services.AndroidHapticsServiceImpl
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import org.koin.dsl.module

fun androidModule(context: Context) =
    module {
        single<AudioService> { AndroidAudioServiceImpl(context, get()) }
        single<HapticsService> { AndroidHapticsServiceImpl(context) }
    }
