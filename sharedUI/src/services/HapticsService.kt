package io.github.smithjustinn.blackjack.services

interface HapticsService {
    fun vibrate()

    fun heavyThud()

    fun pulse()

    fun lightTick()

    fun winPulse()

    fun bustThud()
}

object NoOpHapticsService : HapticsService {
    override fun vibrate() = Unit

    override fun heavyThud() = Unit

    override fun pulse() = Unit

    override fun lightTick() = Unit

    override fun winPulse() = Unit

    override fun bustThud() = Unit
}
