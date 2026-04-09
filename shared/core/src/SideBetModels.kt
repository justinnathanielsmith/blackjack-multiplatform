package io.github.smithjustinn.blackjack

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
 */
@Serializable
enum class SideBetOutcome {
    // Perfect Pairs outcomes
    PERFECT_PAIR,
    COLORED_PAIR,
    MIXED_PAIR,

    // 21+3 outcomes
    SUITED_TRIPLE,
    STRAIGHT_FLUSH,
    THREE_OF_A_KIND,
    STRAIGHT,
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
