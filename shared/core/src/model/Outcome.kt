package io.github.smithjustinn.blackjack.model

import kotlinx.serialization.Serializable

/**
 * Represents the isolated win/loss result of a player hand against the dealer's score,
 * independent of payout ratios or wagers.
 */
@Serializable
enum class HandOutcome {
    /** A 2-card 21 (Natural Blackjack). Out-ranks a normal 21 and typically pays 3:2. */
    NATURAL_WIN,

    /** Player beat the dealer by score or the dealer busted. Typically pays 1:1. */
    WIN,

    /** Tie score with the dealer. Bet is returned to the player. */
    PUSH,

    /** Player busted or dealer achieved a higher score. Bet is forfeited. */
    LOSS
}

/**
 * Aggregated result of all player hands at the end of a round.
 * @param totalPayout sum of individual hand payouts (0 means all were lost)
 * @param anyWin true if at least one hand won or had a natural BJ
 * @param allPush true if every hand pushed (used to determine [GameStatus.PUSH])
 */
data class HandResults(
    val totalPayout: Int,
    val anyWin: Boolean,
    val allPush: Boolean
)
