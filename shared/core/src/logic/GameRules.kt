package io.github.smithjustinn.blackjack.logic

import kotlinx.serialization.Serializable

/**
 * Represents the payout ratio for a natural Blackjack (an Ace and a 10-value card on the initial deal).
 *
 * Standard casino rules offer 3:2, though some rule variations offer 6:5 which increases the house edge.
 *
 * @property numerator The multiple of the bet returned as profit.
 * @property denominator The base unit of the bet used to calculate the multiplied profit.
 */
@Serializable
enum class BlackjackPayout(
    val numerator: Int,
    val denominator: Int
) {
    /** Standard payout: a $2 bet returns $3 profit. */
    THREE_TO_TWO(3, 2),

    /** Reduced payout: a $5 bet returns $6 profit. */
    SIX_TO_FIVE(6, 5),
}

/**
 * Configuration options governing the house rules and payouts for the Blackjack game.
 *
 * Modifying these rules affects player odds, strategy charts, and whether certain actions
 * (like surrendering or doubling after a split) are permitted.
 *
 * @property dealerHitsSoft17 If true, the dealer must draw another card on a soft 17 (an Ace and a 6). If false, dealer stands.
 * @property allowDoubleAfterSplit If true, the player can double down on hands resulting from a split.
 * @property allowSurrender If true, the player can forfeit their hand at the start of their turn for half their bet.
 * @property blackjackPayout The payout ratio for a natural Blackjack (e.g. 3:2).
 * @property deckCount The number of 52-card decks used in the shoe.
 * @property splitOnValueOnly If true, any two cards with the same point value may be split.
 */
@Serializable
data class GameRules(
    val dealerHitsSoft17: Boolean = true,
    val allowDoubleAfterSplit: Boolean = true,
    val allowSurrender: Boolean = false,
    val blackjackPayout: BlackjackPayout = BlackjackPayout.THREE_TO_TWO,
    val deckCount: Int = 6,
    /**
     * When true, any two cards with the same point value may be split (e.g. King + Jack).
     * When false (default), only cards of the exact same [Rank] may be split.
     */
    val splitOnValueOnly: Boolean = false,
)
