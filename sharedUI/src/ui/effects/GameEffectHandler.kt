package io.github.smithjustinn.blackjack.ui.effects

import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService

fun handleGameEffect(
    effect: GameEffect,
    hapticsService: HapticsService,
    audioService: AudioService,
    isSoundMuted: Boolean,
) {
    when (effect) {
        GameEffect.Vibrate -> hapticsService.vibrate()
        GameEffect.PlayCardSound -> if (!isSoundMuted) audioService.playEffect(AudioService.SoundEffect.FLIP)
        GameEffect.PlayWinSound -> if (!isSoundMuted) audioService.playEffect(AudioService.SoundEffect.WIN)
        GameEffect.PlayLoseSound -> if (!isSoundMuted) audioService.playEffect(AudioService.SoundEffect.LOSE)
        GameEffect.DealerCriticalDraw -> if (!isSoundMuted) audioService.playEffect(AudioService.SoundEffect.TENSION)
        is GameEffect.NearMissHighlight -> { /* visual only — handled in BlackjackScreen */ }
        GameEffect.HeavyCardThud -> hapticsService.heavyThud()
        GameEffect.Pulse21 -> hapticsService.pulse()
        is GameEffect.ChipEruption -> { /* visual only — handled in BlackjackScreen */ }
        is GameEffect.ChipLoss -> { /* visual only — handled in BlackjackScreen */ }
    }
}
