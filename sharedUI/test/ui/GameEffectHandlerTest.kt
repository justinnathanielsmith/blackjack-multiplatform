package io.github.smithjustinn.blackjack.ui.effects

import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.services.HapticsService
import kotlin.test.Test
import kotlin.test.assertEquals

class GameEffectHandlerTest {
    private val audioService =
        object : io.github.smithjustinn.blackjack.services.AudioService {
            override fun playEffect(effect: io.github.smithjustinn.blackjack.services.AudioService.SoundEffect) {}

            override fun release() {}
        }

    @Test
    fun vibrateEffectTriggersHaptics() {
        var calls = 0
        val hapticsService =
            object : HapticsService {
                override fun vibrate() {
                    calls += 1
                }
            }

        handleGameEffect(
            effect = GameEffect.Vibrate,
            hapticsService = hapticsService,
            audioService = audioService,
            isSoundMuted = false
        )

        assertEquals(1, calls)
    }

    @Test
    fun nonVibrateEffectDoesNotTriggerHaptics() {
        var calls = 0
        val hapticsService =
            object : HapticsService {
                override fun vibrate() {
                    calls += 1
                }
            }

        handleGameEffect(
            effect = GameEffect.PlayWinSound,
            hapticsService = hapticsService,
            audioService = audioService,
            isSoundMuted = false
        )

        assertEquals(0, calls)
    }
}
