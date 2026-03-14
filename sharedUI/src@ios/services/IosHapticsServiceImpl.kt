package io.github.smithjustinn.blackjack.services

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

class IosHapticsServiceImpl : HapticsService {
    private val generator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)

    override fun vibrate() {
        generator.prepare()
        generator.impactOccurred()
    }
}
