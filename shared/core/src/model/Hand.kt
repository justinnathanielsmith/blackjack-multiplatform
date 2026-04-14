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
    val isSettled: Boolean = false,
    val isSurrendered: Boolean = false,
    /** True when this hand was doubled down — the 3rd card should render rotated 90°. */
    val isDoubleDown: Boolean = false,
) {
    /** The total point value of all cards in the hand, with Aces counted as 11 where possible. */
    val score: Int

    /** True if at least one Ace is being counted as 11 (i.e. the hand is "soft"). */
    val isSoft: Boolean

    /** The point value of only face-up cards, used for the dealer up-card display. */
    val visibleScore: Int

    init {
        // Bolt Performance Optimization: Compute score, softness, and visibility in a single pass
        // during instantiation. This eliminates 6 Lazy delegate allocations and 1 HandMetrics
        // object allocation per Hand copy, significantly reducing GC churn in the hot loop.
        var s = 0
        var aces = 0
        var vs = 0
        var vAces = 0

        for (i in 0 until cards.size) {
            val card = cards[i]
            val value = card.rank.value
            val isAce = card.rank == Rank.ACE

            s += value
            if (isAce) aces++

            if (!card.isFaceDown) {
                vs += value
                if (isAce) vAces++
            }
        }

        while (s > 21 && aces > 0) {
            s -= 10
            aces--
        }

        while (vs > 21 && vAces > 0) {
            vs -= 10
            vAces--
        }

        score = s
        isSoft = aces > 0
        visibleScore = vs
    }

    /** True if the hand's [score] exceeds 21. */
    val isBust: Boolean get() = score > 21

    /**
     * True if this hand is a "natural" blackjack — exactly 2 cards totalling 21.
     * Note: A hand totaling 21 after splitting or hitting is NOT a natural blackjack.
     */
    val isBlackjack: Boolean get() = cards.size == 2 && score == 21

    /**
     * True if the hand totals 21 via three or more cards (not a natural blackjack).
     * Distinct from [isBlackjack], which requires exactly 2 cards.
     */
    val isTwentyOne: Boolean get() = score == 21 && !isBlackjack

    /**
     * A [0.0, 1.0] weight representing how dangerous this hand is to hit.
     * Scores 17–20 are the standing-risk zone; scores below 17 carry no tension.
     */
    val tension: Float
        get() =
            when {
                score >= BlackjackConfig.TENSION_SCORE_MAX -> BlackjackConfig.TENSION_VALUE_MAX
                score == BlackjackConfig.TENSION_SCORE_HIGH -> BlackjackConfig.TENSION_VALUE_HIGH
                score == BlackjackConfig.TENSION_SCORE_MED -> BlackjackConfig.TENSION_VALUE_MED
                score == BlackjackConfig.TENSION_SCORE_LOW -> BlackjackConfig.TENSION_VALUE_LOW
                else -> BlackjackConfig.TENSION_VALUE_NONE
            }

    /**
     * True if the hand's [score] is exactly 21, regardless of whether achieved as a
     * natural blackjack (2 cards) or by drawing to 21 (3+ cards).
     * Use this when the distinction between [isBlackjack] and [isTwentyOne] does not matter —
     * single source of truth so callers never reconstruct this predicate inline.
     */
    val isScore21: Boolean get() = score == 21
}
