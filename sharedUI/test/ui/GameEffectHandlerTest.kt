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

    private fun trackingHaptics(
        onVibrate: () -> Unit = {},
        onHeavyThud: () -> Unit = {},
        onPulse: () -> Unit = {},
        onLightTick: () -> Unit = {},
        onWinPulse: () -> Unit = {},
        onBustThud: () -> Unit = {},
    ): HapticsService =
        object : HapticsService {
            override fun vibrate() = onVibrate()

            override fun heavyThud() = onHeavyThud()

            override fun pulse() = onPulse()

            override fun lightTick() = onLightTick()

            override fun winPulse() = onWinPulse()

            override fun bustThud() = onBustThud()
        }

    @Test
    fun vibrateEffectTriggersHaptics() {
        var calls = 0
        val hapticsService = trackingHaptics(onVibrate = { calls += 1 })

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
        val hapticsService = trackingHaptics(onVibrate = { calls += 1 })

        handleGameEffect(
            effect = GameEffect.PlayWinSound,
            hapticsService = hapticsService,
            audioService = noopAudio,
        )

        assertEquals(0, calls)
    }

    @Test
    fun lightTickEffectRoutesToLightTick() {
        var calls = 0
        val hapticsService = trackingHaptics(onLightTick = { calls += 1 })

        handleGameEffect(
            effect = GameEffect.LightTick,
            hapticsService = hapticsService,
            audioService = noopAudio,
        )

        assertEquals(1, calls)
    }

    @Test
    fun winPulseEffectRoutesToWinPulse() {
        var calls = 0
        val hapticsService = trackingHaptics(onWinPulse = { calls += 1 })

        handleGameEffect(
            effect = GameEffect.WinPulse,
            hapticsService = hapticsService,
            audioService = noopAudio,
        )

        assertEquals(1, calls)
    }

    @Test
    fun bustThdEffectRoutesToBustThud() {
        var calls = 0
        val hapticsService = trackingHaptics(onBustThud = { calls += 1 })

        handleGameEffect(
            effect = GameEffect.BustThud,
            hapticsService = hapticsService,
            audioService = noopAudio,
        )

        assertEquals(1, calls)
    }
}
