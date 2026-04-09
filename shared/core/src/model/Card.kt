package io.github.smithjustinn.blackjack.model
import androidx.compose.runtime.Immutable
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import kotlinx.serialization.Serializable

/**
 * Represents the four standard suits in a deck of playing cards.
 */
@Serializable
enum class Suit {
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
}

/**
 * Represents the rank of a playing card and its associated scoring value in Blackjack.
 *
 * Face cards (Jack, Queen, King) are worth 10. Aces are initially worth 11, subject
 * to value reduction to 1 during [Hand] score calculation if the total would otherwise bust.
 *
 * @property value The base Blackjack scoring value for this rank.
 */
@Serializable
enum class Rank(
    /** The base Blackjack scoring value for this rank. */
    val value: Int
) {
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(10),
    QUEEN(10),
    KING(10),
    ACE(11)
}

/**
 * Represents a single playing card with a specific [rank] and [suit].
 *
 * In this application, a card also carries a [isFaceDown] state, which determines
 * its visibility in the UI and whether its score is included in [Hand.visibleScore].
 *
 * @property rank The [Rank] of the card (e.g., ACE, KING, TWO).
 * @property suit The [Suit] of the card (e.g., HEARTS, SPADES).
 * @property isFaceDown If true, the card's rank and suit are hidden from the player.
 */
@Immutable
@Serializable
data class Card(
    val rank: Rank,
    val suit: Suit,
    val isFaceDown: Boolean = false,
) {
    /** True if the card is face-up (visible to players). */
    val isFaceUp: Boolean get() = !isFaceDown
}
