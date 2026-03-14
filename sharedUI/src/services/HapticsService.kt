package io.github.smithjustinn.blackjack.services

interface HapticsService {
    fun vibrate()
}

object NoOpHapticsService : HapticsService {
    override fun vibrate() = Unit
}
