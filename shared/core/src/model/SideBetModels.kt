package io.github.smithjustinn.blackjack.model
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Identifies the specific type of side bet a player can wager on.
 *
 * Side bets are resolved independently of the main Blackjack hand outcome using the
 * initial dealt cards before any player actions are taken.
 */
@Serializable
enum class SideBetType {
    /** Poker-style hands formed using the player's two initial cards and the dealer's upcard. */
    TWENTY_ONE_PLUS_THREE,

    /** Wagers that the player's initial two cards form a pair based on rank, color, or suit. */
    PERFECT_PAIRS
}

/**
 * Typed discriminator for a winning side-bet hand — replaces raw English strings so the UI can
 * match exhaustively and the compiler enforces coverage when new outcomes are added.
 *
 * Each variant represents a specific poker-style or pair-based hand configuration as defined
 * by standard multi-deck Blackjack side-bet rules.
 */
@Serializable
enum class SideBetOutcome {
    /** A pair of cards with the exact same [Rank] and [Suit]. Possible in multi-deck shoes. (25:1) */
    PERFECT_PAIR,

    /** A pair of cards with the same [Rank] and color (e.g., Hearts and Diamonds), but different suits. (12:1) */
    COLORED_PAIR,

    /** A pair of cards with the same [Rank], but different colors (e.g., Clubs and Diamonds). (5:1) */
    MIXED_PAIR,

    /** Three cards of the exact same [Rank] and [Suit]. (100:1) */
    SUITED_TRIPLE,

    /** Three cards in sequential [Rank] (e.g., 5-6-7) and of the same [Suit]. (40:1) */
    STRAIGHT_FLUSH,

    /** Three cards of the same [Rank], but not all the same suit. (30:1) */
    THREE_OF_A_KIND,

    /** Three cards in sequential [Rank] (e.g., 8-9-10) regardless of suit. (10:1) */
    STRAIGHT,

    /** Three cards of the same [Suit] regardless of rank. (5:1) */
    FLUSH,
}

/**
 * Contains the settled resolution of a specific side bet after the initial cards are dealt.
 *
 * This immutable result is stored in the [GameState] and is used by the UI to present
 * specific bet winnings and their categorical names in the result banner.
 *
 * @property type The [SideBetType] that this result corresponds to.
 * @property payoutMultiplier The ratio at which the initial wager is multiplied to calculate profit.
 * @property payoutAmount The total chips (original bet + profit) returned to the player.
 * @property outcome A typed [SideBetOutcome] identifying the winning hand (e.g. [SideBetOutcome.FLUSH]).
 */
@Immutable
@Serializable
data class SideBetResult(
    val type: SideBetType,
    val payoutMultiplier: Int,
    val payoutAmount: Int,
    // Typed discriminator; exhaustiveness enforced at every when-site
    val outcome: SideBetOutcome,
)
