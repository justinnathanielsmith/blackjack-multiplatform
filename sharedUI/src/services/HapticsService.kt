package io.github.smithjustinn.blackjack.services

interface HapticsService {
    fun vibrate()

    fun heavyThud()

    fun pulse()
}

object NoOpHapticsService : HapticsService {
    override fun vibrate() = Unit

    override fun heavyThud() = Unit

    override fun pulse() = Unit
}
