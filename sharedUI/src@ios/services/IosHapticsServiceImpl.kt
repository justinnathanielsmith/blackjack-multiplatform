package io.github.smithjustinn.blackjack.services

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

/**
 * iOS-specific implementation of [HapticsService] using UIKit's feedback generators.
 *
 * Employs [UIImpactFeedbackGenerator] for physical-feeling impacts and
 * [UINotificationFeedbackGenerator] for success/error notifications.
 *
 * @see HapticsService
 */
class IosHapticsServiceImpl : HapticsService {
    private val mediumGenerator by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium) }
    private val heavyGenerator by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy) }
    private val lightGenerator by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight) }
    private val notifGenerator by lazy { UINotificationFeedbackGenerator() }

    /** @see HapticsService.vibrate */
    override fun vibrate() {
        mediumGenerator.prepare()
        mediumGenerator.impactOccurred()
    }

    /** @see HapticsService.heavyThud */
    override fun heavyThud() {
        heavyGenerator.prepare()
        heavyGenerator.impactOccurred()
    }

    /** @see HapticsService.pulse */
    override fun pulse() {
        notifGenerator.prepare()
        notifGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }

    /** @see HapticsService.lightTick */
    override fun lightTick() {
        lightGenerator.prepare()
        lightGenerator.impactOccurred()
    }

    /** @see HapticsService.winPulse */
    override fun winPulse() {
        notifGenerator.prepare()
        notifGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
        mediumGenerator.prepare()
        mediumGenerator.impactOccurred()
    }

    /** @see HapticsService.bustThud */
    override fun bustThud() {
        notifGenerator.prepare()
        notifGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
    }
}
