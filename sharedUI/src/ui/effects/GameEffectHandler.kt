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
        GameEffect.PlayCardSound -> playAudio(audioService, isSoundMuted, AudioService.SoundEffect.FLIP)
        GameEffect.PlayWinSound -> playAudio(audioService, isSoundMuted, AudioService.SoundEffect.WIN)
        GameEffect.PlayLoseSound -> playAudio(audioService, isSoundMuted, AudioService.SoundEffect.LOSE)
        GameEffect.DealerCriticalDraw -> playAudio(audioService, isSoundMuted, AudioService.SoundEffect.TENSION)
        is GameEffect.NearMissHighlight -> { /* visual only — handled in BlackjackScreen */ }
        GameEffect.HeavyCardThud -> hapticsService.heavyThud()
        GameEffect.Pulse21 -> hapticsService.pulse()
        is GameEffect.ChipEruption -> { /* visual only — handled in BlackjackScreen */ }
        is GameEffect.ChipLoss -> { /* visual only — handled in BlackjackScreen */ }
    }
}

private fun playAudio(
    audioService: AudioService,
    isSoundMuted: Boolean,
    sound: AudioService.SoundEffect,
) {
    if (!isSoundMuted) audioService.playEffect(sound)
}
