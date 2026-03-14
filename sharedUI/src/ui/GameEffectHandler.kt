package io.github.smithjustinn.blackjack.ui

import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.services.HapticsService

fun handleGameEffect(
    effect: GameEffect,
    hapticsService: HapticsService,
) {
    if (effect is GameEffect.Vibrate) {
        hapticsService.vibrate()
    }
}
