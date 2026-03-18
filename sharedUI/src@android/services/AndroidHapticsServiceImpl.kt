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

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun vibrate() {
        val deviceVibrator = vibrator ?: return
        if (!deviceVibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            deviceVibrator.vibrate(VibrationEffect.createOneShot(DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            deviceVibrator.vibrate(DURATION_MS)
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun heavyThud() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
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
    override fun pulse() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(PULSE_TIMINGS, PULSE_AMPLITUDES, VIBRATE_ONCE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(PULSE_TIMINGS, VIBRATE_ONCE)
        }
    }

    companion object {
        private const val DURATION_MS = 35L
        private const val HEAVY_THUD_DURATION_MS = 80L
        private const val VIBRATE_ONCE = -1
        private val PULSE_TIMINGS = longArrayOf(0, 60, 80, 60)
        private val PULSE_AMPLITUDES = intArrayOf(0, 200, 0, 200)
    }
}
