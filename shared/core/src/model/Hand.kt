package io.github.smithjustinn.blackjack.model
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

/**
 * A collection of cards held by a participant (Player or Dealer).
 *
 * This class encapsulates the **Scoring Truth** for a hand. It determines point
 * totals, bust status, and natural Blackjacks.
 *
 * **Functional Intent:**
 * - **Score vs. Visibility**: [score] is the true engine value; [visibleScore] is the
 *   player-facing value (gating face-down cards).
 * - **Tension Metric**: [tension] is a UI-only heuristic used to drive "juice"
 *   animations (shakes, glows) when a hand is in a high-risk standing zone.
 * - **Split Rules**: [isFromSplitAce] enforces house constraints (e.g., only one
 *   card dealt to split Aces).
 *
 * @property cards Persistent shoe slice held by this hand.
 * @property bet Active wager currently tied to this hand's outcome.
 * @property isStanding Input lock: true if the player has finalized this hand's turn.
 * @property wasSplit Lineage: true if this hand originated from a [GameAction.Split].
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
    val tension: Float by lazy {
        when {
            score >= BlackjackConfig.TENSION_SCORE_MAX -> BlackjackConfig.TENSION_VALUE_MAX
            score == BlackjackConfig.TENSION_SCORE_HIGH -> BlackjackConfig.TENSION_VALUE_HIGH
            score == BlackjackConfig.TENSION_SCORE_MED -> BlackjackConfig.TENSION_VALUE_MED
            score == BlackjackConfig.TENSION_SCORE_LOW -> BlackjackConfig.TENSION_VALUE_LOW
            else -> BlackjackConfig.TENSION_VALUE_NONE
        }
    }

    /**
     * True if the hand's [score] is exactly 21, regardless of whether achieved as a
     * natural blackjack (2 cards) or by drawing to 21 (3+ cards).
     * Use this when the distinction between [isBlackjack] and [isTwentyOne] does not matter —
     * single source of truth so callers never reconstruct this predicate inline.
     */
    val isScore21: Boolean get() = score == 21

    /**
     * True if at least one Ace is being counted as 11 (i.e. the hand is "soft").
     * Derived efficiently from the score calculation result.
     */
    val isSoft: Boolean get() = metrics.isSoft
}
