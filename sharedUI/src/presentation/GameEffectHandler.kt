// Moved from ui.effects to presentation — this is pure effect routing logic with
// zero Compose dependencies, consumed exclusively by the presentation layer.
package io.github.smithjustinn.blackjack.presentation

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
    val action: (HapticsService) -> Unit =
        when (effect) {
            GameEffect.Vibrate -> HapticsService::vibrate
            GameEffect.HeavyCardThud -> HapticsService::heavyThud
            GameEffect.Pulse21 -> HapticsService::pulse
            GameEffect.LightTick -> HapticsService::lightTick
            GameEffect.WinPulse -> HapticsService::winPulse
            GameEffect.BustThud -> HapticsService::bustThud
            else -> return
        }
    action(haptics)
}

private fun handleAudioEffect(
    effect: GameEffect,
    audio: AudioService
) {
    val sound =
        when (effect) {
            GameEffect.PlayCardSound -> AudioService.SoundEffect.FLIP
            GameEffect.PlayWinSound -> AudioService.SoundEffect.WIN
            GameEffect.PlayLoseSound -> AudioService.SoundEffect.LOSE
            GameEffect.DealerCriticalDraw -> AudioService.SoundEffect.TENSION
            GameEffect.PlayPlinkSound -> AudioService.SoundEffect.PLINK
            GameEffect.PlayPushSound -> AudioService.SoundEffect.PUSH
            is GameEffect.BigWin -> AudioService.SoundEffect.THE_NUTS
            else -> null
        }
    if (sound != null) {
        audio.playEffect(sound)
    }
}
