package io.github.smithjustinn.blackjack.services

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission

class AndroidHapticsServiceImpl(
    context: Context,
) : HapticsService {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private inline fun withVibrator(block: (Vibrator) -> Unit) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        block(v)
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun vibrate() =
        withVibrator { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(DURATION_MS)
            }
        }

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun heavyThud() =
        withVibrator { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(HEAVY_THUD_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(HEAVY_THUD_DURATION_MS)
            }
        }

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun pulse() =
        withVibrator { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(PULSE_TIMINGS, PULSE_AMPLITUDES, WAVEFORM_NO_REPEAT))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(PULSE_TIMINGS, WAVEFORM_NO_REPEAT)
            }
        }

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun lightTick() =
        withVibrator { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(TICK_DURATION_MS, TICK_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(TICK_DURATION_MS)
            }
        }

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun winPulse() =
        withVibrator { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(WIN_PULSE_TIMINGS, WIN_PULSE_AMPLITUDES, WAVEFORM_NO_REPEAT))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(WIN_PULSE_TIMINGS, WAVEFORM_NO_REPEAT)
            }
        }

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun bustThud() =
        withVibrator { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(BUST_THUD_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(BUST_THUD_DURATION_MS)
            }
        }

    companion object {
        private const val DURATION_MS = 35L
        private const val HEAVY_THUD_DURATION_MS = 80L
        private const val WAVEFORM_NO_REPEAT = -1
        private val PULSE_TIMINGS = longArrayOf(0, 60, 80, 60)
        private val PULSE_AMPLITUDES = intArrayOf(0, 200, 0, 200)
        private const val TICK_DURATION_MS = 10L
        private const val TICK_AMPLITUDE = 100
        private val WIN_PULSE_TIMINGS = longArrayOf(0, 50, 60, 50)
        private val WIN_PULSE_AMPLITUDES = intArrayOf(0, 180, 0, 220)
        private const val BUST_THUD_DURATION_MS = 150L
    }
}
