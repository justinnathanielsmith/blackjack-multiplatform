package io.github.smithjustinn.blackjack.ui.effects

import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService

/**
 * Dispatches [GameEffect]s to the appropriate service (audio, haptics, etc.).
 * Complexity is managed by decomposing into specialized handlers.
 */
fun handleGameEffect(
    effect: GameEffect,
    hapticsService: HapticsService,
    audioService: AudioService,
) {
    handleHapticEffect(effect, hapticsService)
    handleAudioEffect(effect, audioService)
}

private fun handleHapticEffect(
    effect: GameEffect,
    haptics: HapticsService
) {
    when (effect) {
        GameEffect.Vibrate -> haptics.vibrate()
        GameEffect.HeavyCardThud -> haptics.heavyThud()
        GameEffect.Pulse21 -> haptics.pulse()
        GameEffect.LightTick -> haptics.lightTick()
        GameEffect.WinPulse -> haptics.winPulse()
        GameEffect.BustThud -> haptics.bustThud()
        else -> {}
    }
}

private fun handleAudioEffect(
    effect: GameEffect,
    audio: AudioService
) {
    when (effect) {
        GameEffect.PlayCardSound -> audio.playEffect(AudioService.SoundEffect.FLIP)
        GameEffect.PlayWinSound -> audio.playEffect(AudioService.SoundEffect.WIN)
        GameEffect.PlayLoseSound -> audio.playEffect(AudioService.SoundEffect.LOSE)
        GameEffect.DealerCriticalDraw -> audio.playEffect(AudioService.SoundEffect.TENSION)
        GameEffect.PlayPlinkSound -> audio.playEffect(AudioService.SoundEffect.PLINK)
        GameEffect.PlayPushSound -> audio.playEffect(AudioService.SoundEffect.PUSH)
        is GameEffect.BigWin -> audio.playEffect(AudioService.SoundEffect.THE_NUTS)
        else -> {}
    }
}
