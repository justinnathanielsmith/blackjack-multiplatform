package io.github.smithjustinn.blackjack.ui.ui

import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.ui.handleGameEffect
import kotlin.test.Test
import kotlin.test.assertEquals

class GameEffectHandlerTest {
    @Test
    fun vibrateEffectTriggersHaptics() {
        var calls = 0
        val hapticsService =
            object : HapticsService {
                override fun vibrate() {
                    calls += 1
                }
            }

        handleGameEffect(GameEffect.Vibrate, hapticsService)

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

        handleGameEffect(GameEffect.PlayWinSound, hapticsService)

        assertEquals(0, calls)
    }
}
