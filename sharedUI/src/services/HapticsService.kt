package io.github.smithjustinn.blackjack.services

/**
 * Provides hardware feedback (vibrations and haptics) to enhance the user experience.
 *
 * This service is responsible for delivering tactile feedback that correlates with
 * game events (e.g., card deals, busts, wins) to provide a "premium" feel.
 * Platform-specific implementations handle the nuances of each OS's haptic engine.
 *
 * @see NoOpHapticsService
 */
interface HapticsService {
    /** Triggers a standard device vibration. */
    fun vibrate()

    /** Triggers a heavy, physical-feeling thud, often used when high-value cards are dealt. */
    fun heavyThud()

    /** Triggers a quick, success-oriented haptic pulse. */
    fun pulse()

    /** Triggers a subtle, short haptic tick for general interactions or low-value card deals. */
    fun lightTick()

    /** Triggers a distinct haptic burst representing a game victory or high-payout event. */
    fun winPulse()

    /** Triggers a heavy, error-oriented thud when a player's hand exceeds 21. */
    fun bustThud()
}

/**
 * A "no-op" implementation of [HapticsService] for platforms without haptic support
 * or for use during testing and previews.
 */
object NoOpHapticsService : HapticsService {
    /** @see HapticsService.vibrate */
    override fun vibrate() = Unit

    /** @see HapticsService.heavyThud */
    override fun heavyThud() = Unit

    /** @see HapticsService.pulse */
    override fun pulse() = Unit

    /** @see HapticsService.lightTick */
    override fun lightTick() = Unit

    /** @see HapticsService.winPulse */
    override fun winPulse() = Unit

    /** @see HapticsService.bustThud */
    override fun bustThud() = Unit
}
