package io.github.smithjustinn.blackjack.ui.effects

import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import kotlin.test.Test
import kotlin.test.assertEquals

class GameEffectHandlerTest {
    private val noopAudio =
        object : AudioService {
            override var isMuted: Boolean = false
            override fun playEffect(effect: AudioService.SoundEffect) {}

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

                override fun heavyThud() = Unit

                override fun pulse() = Unit
            }

        handleGameEffect(
            effect = GameEffect.Vibrate,
            hapticsService = hapticsService,
            audioService = noopAudio,
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

                override fun heavyThud() = Unit

                override fun pulse() = Unit
            }

        handleGameEffect(
            effect = GameEffect.PlayWinSound,
            hapticsService = hapticsService,
            audioService = noopAudio,
        )

        assertEquals(0, calls)
    }
}
