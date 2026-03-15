package io.github.smithjustinn.blackjack.services

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

class IosHapticsServiceImpl : HapticsService {
    private val mediumGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val heavyGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val notifGenerator = UINotificationFeedbackGenerator()

    override fun vibrate() {
        mediumGenerator.prepare()
        mediumGenerator.impactOccurred()
    }

    override fun heavyThud() {
        heavyGenerator.prepare()
        heavyGenerator.impactOccurred()
    }

    override fun pulse() {
        notifGenerator.prepare()
        notifGenerator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }
}
