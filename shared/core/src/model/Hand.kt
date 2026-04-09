package io.github.smithjustinn.blackjack.model
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

/**
 * Represents a set of cards held by either the player or the dealer.
 *
 * Each hand tracks its own cards, associated bet, and lifecycle status (e.g., whether it
 * was split or stands). Blackjack scoring logic, including natural blackjacks and
 * ace-reduction, is encapsulated here.
 *
 * @property cards The [PersistentList] of cards held in this hand.
 * @property bet The current amount wagered on this hand (main bet).
 * @property lastBet The previous bet amount, used for "Repeat Bet" functionality.
 * @property isStanding True if the player has chosen to take no further cards for this hand.
 * @property wasSplit True if this hand was created by splitting a identical pair.
 * @property isFromSplitAce True if this hand was one of two created by splitting a pair of Aces.
 *           Special rules often apply to split Aces (e.g., only one card deal).
 * @property isSurrendered True if the player has surrendered the hand (losing half the bet).
 */
@Immutable
@Serializable
data class Hand(
    val cards: PersistentList<Card> = persistentListOf(),
    val bet: Int = 0,
    val lastBet: Int = 0,
    val isStanding: Boolean = false,
    val wasSplit: Boolean = false,
    val isFromSplitAce: Boolean = false,
    val isSurrendered: Boolean = false,
    /** True when this hand was doubled down — the 3rd card should render rotated 90°. */
    val isDoubleDown: Boolean = false,
) {
    private data class HandMetrics(
        val score: Int,
        val isSoft: Boolean
    )

    // Bolt Performance Optimization: Score and softness are computed in a single pass.
    // Memoization using `by lazy` reduces O(N) card iterations to O(1) during gameplay.
    private val metrics: HandMetrics by lazy {
        var s = 0
        var aces = 0
        for (i in 0 until cards.size) {
            val card = cards[i]
            s += card.rank.value
            if (card.rank == Rank.ACE) aces++
        }
        while (s > 21 && aces > 0) {
            s -= 10
            aces--
        }
        HandMetrics(score = s, isSoft = aces > 0)
    }

    /**
     * The total point value of all cards in the hand, with Aces counted as 11
     * where possible without exceeding 21.
     */
    val score: Int get() = metrics.score

    /**
     * The point value of only face-up cards in the hand.
     * This is primarily used for the dealer up-card display before the hole card is revealed.
     */
    val visibleScore: Int by lazy {
        var vs = 0
        var vAces = 0
        for (i in 0 until cards.size) {
            val card = cards[i]
            if (card.isFaceDown) continue
            vs += card.rank.value
            if (card.rank == Rank.ACE) vAces++
        }
        while (vs > 21 && vAces > 0) {
            vs -= 10
            vAces--
        }
        vs
    }

    /** True if the hand's [score] exceeds 21. */
    val isBust: Boolean by lazy { score > 21 }

    /**
     * True if this hand is a "natural" blackjack — exactly 2 cards totalling 21.
     * Note: A hand totaling 21 after splitting or hitting is NOT a natural blackjack.
     */
    val isBlackjack: Boolean by lazy { cards.size == 2 && score == 21 }

    /**
     * True if the hand totals 21 via three or more cards (not a natural blackjack).
     * Distinct from [isBlackjack], which requires exactly 2 cards.
     */
    val isTwentyOne: Boolean by lazy { score == 21 && !isBlackjack }

    /**
     * A [0.0, 1.0] weight representing how dangerous this hand is to hit.
     * Scores 17–20 are the standing-risk zone; scores below 17 carry no tension.
     */
    @Suppress("MagicNumber") // Blackjack standing-risk thresholds are self-documenting domain values
    val tension: Float by lazy {
        when {
            score >= 20 -> 1.0f
            score == 19 -> 0.7f
            score == 18 -> 0.4f
            score == 17 -> 0.2f
            else -> 0.0f
        }
    }

    /**
     * True if at least one Ace is being counted as 11 (i.e. the hand is "soft").
     * Derived efficiently from the score calculation result.
     */
    val isSoft: Boolean get() = metrics.isSoft
}
