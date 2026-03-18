package io.github.smithjustinn.blackjack.ui.effects

import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService

fun handleGameEffect(
    effect: GameEffect,
    hapticsService: HapticsService,
    audioService: AudioService,
) {
    when (effect) {
        GameEffect.Vibrate -> hapticsService.vibrate()
        GameEffect.PlayCardSound -> audioService.playEffect(AudioService.SoundEffect.FLIP)
        GameEffect.PlayWinSound -> audioService.playEffect(AudioService.SoundEffect.WIN)
        GameEffect.PlayLoseSound -> audioService.playEffect(AudioService.SoundEffect.LOSE)
        GameEffect.DealerCriticalDraw -> audioService.playEffect(AudioService.SoundEffect.TENSION)
        is GameEffect.NearMissHighlight -> { /* visual only — handled in BlackjackScreen */ }
        GameEffect.HeavyCardThud -> hapticsService.heavyThud()
        GameEffect.Pulse21 -> hapticsService.pulse()
        GameEffect.LightTick -> hapticsService.lightTick()
        GameEffect.WinPulse -> hapticsService.winPulse()
        GameEffect.BustThud -> hapticsService.bustThud()
        is GameEffect.ChipEruption -> { /* visual only — handled in BlackjackScreen */ }
        is GameEffect.ChipLoss -> { /* visual only — handled in BlackjackScreen */ }
        GameEffect.PlayPlinkSound -> audioService.playEffect(AudioService.SoundEffect.PLINK)
    }
}
