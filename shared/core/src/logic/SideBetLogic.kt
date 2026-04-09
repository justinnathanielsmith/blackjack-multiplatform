package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.SideBetOutcome
import io.github.smithjustinn.blackjack.model.SideBetResult
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.model.Suit
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

/**
 * Result of resolving all active side bets for a round.
 *
 * This container aggregates the total winnings across all side bets and provides
 * granular results for each [SideBetType] to reflect in the UI.
 *
 * @property payoutTotal The total sum of chips to be returned to the player from all winning side bets.
 * @property results A map of [SideBetType] to its specific [SideBetResult], containing payout details and outcome names.
 */
data class SideBetResolution(
    val payoutTotal: Int,
    val results: PersistentMap<SideBetType, SideBetResult>
)

/**
 * Domain logic for settling Blackjack side bets.
 *
 * This object contains pure functions for evaluating player and dealer cards against
 * specialized side bet rules like "Perfect Pairs" and "21+3". Payouts are calculated
 * based on standard multi-deck casino conventions.
 */
object SideBetLogic {
    private const val PERFECT_PAIR_PAYOUT = 25
    private const val COLORED_PAIR_PAYOUT = 12
    private const val MIXED_PAIR_PAYOUT = 5

    private const val SUITED_TRIPLE_PAYOUT = 100
    private const val STRAIGHT_FLUSH_PAYOUT = 40
    private const val THREE_OF_A_KIND_PAYOUT = 30
    private const val STRAIGHT_PAYOUT = 10
    private const val FLUSH_PAYOUT = 5

    private val RED_SUITS = setOf(Suit.HEARTS, Suit.DIAMONDS)
    private val BLACK_SUITS = setOf(Suit.CLUBS, Suit.SPADES)

    /**
     * Orchestrates the resolution of all active side bets on the table.
     *
     * Iterates through the player's active [sideBets], evaluates each against the
     * provided player and dealer cards, and computes the total payout.
     *
     * @param sideBets The active side bets placed during the [GameStatus.BETTING] phase.
     * @param playerHand The player's first [Hand], which must contain exactly two cards.
     * @param dealerUpcard The dealer's first card (face-up).
     * @return A [SideBetResolution] containing the total chip payout and detailed results for each bet.
     */
    fun resolveSideBets(
        sideBets: PersistentMap<SideBetType, Int>,
        playerHand: Hand,
        dealerUpcard: Card
    ): SideBetResolution {
        var totalPayout = 0
        val results = mutableMapOf<SideBetType, SideBetResult>()

        sideBets.forEach { (type, amount) ->
            val result =
                when (type) {
                    SideBetType.PERFECT_PAIRS -> evaluatePerfectPairs(playerHand)
                    SideBetType.TWENTY_ONE_PLUS_THREE ->
                        evaluateTwentyOnePlusThree(
                            playerHand,
                            dealerUpcard
                        )
                }

            if (result != null) {
                val payout = amount * result.payoutMultiplier + amount
                totalPayout += payout
                results[type] = result.copy(payoutAmount = payout)
            }
        }

        return SideBetResolution(totalPayout, results.toPersistentMap())
    }

    /**
     * Evaluates a player's starting hand for "Perfect Pairs" combinations.
     *
     * A pair is defined as two cards of the exact same [Rank].
     * Payouts follow standard rules:
     * - Perfect Pair (same suit): 25:1
     * - Colored Pair (different suit, same color): 12:1
     * - Mixed Pair (different suit, different colors): 5:1
     *
     * @param hand The player's [Hand] to evaluate. Must contain at least two cards.
     * @return A [SideBetResult] if a pair exists, or null otherwise.
     */
    fun evaluatePerfectPairs(hand: Hand): SideBetResult? {
        if (hand.cards.size < 2) return null
        val c1 = hand.cards[0]
        val c2 = hand.cards[1]

        if (!isPair(c1, c2)) return null

        return when {
            c1.suit == c2.suit ->
                SideBetResult(SideBetType.PERFECT_PAIRS, PERFECT_PAIR_PAYOUT, 0, SideBetOutcome.PERFECT_PAIR)
            isSameColor(c1.suit, c2.suit) ->
                SideBetResult(SideBetType.PERFECT_PAIRS, COLORED_PAIR_PAYOUT, 0, SideBetOutcome.COLORED_PAIR)
            else ->
                SideBetResult(SideBetType.PERFECT_PAIRS, MIXED_PAIR_PAYOUT, 0, SideBetOutcome.MIXED_PAIR)
        }
    }

    private fun isPair(
        c1: Card,
        c2: Card
    ): Boolean {
        // Must be the same RANK (not just the same value)
        return c1.rank == c2.rank
    }

    /**
     * Evaluates the player's first two cards and the dealer's upcard for "21+3" poker-style combinations.
     *
     * Payouts follow standard "9-1" or "Multi-Deck" rules:
     * - Suited Triple (3 identical cards): 100:1
     * - Straight Flush (3 cards in sequence and same suit): 40:1
     * - Three of a Kind (3 cards of same rank): 30:1
     * - Straight (3 cards in sequence): 10:1
     * - Flush (3 cards of same suit): 5:1
     *
     * @param playerHand The player's starting [Hand].
     * @param dealerUpcard The dealer's visible upcard.
     * @return A [SideBetResult] if a poker combination exists, or null otherwise.
     */
    fun evaluateTwentyOnePlusThree(
        playerHand: Hand,
        dealerUpcard: Card
    ): SideBetResult? {
        if (playerHand.cards.size < 2) return null
        // Bolt Performance Optimization: Replace listOf() allocation with explicit vars to avoid List instantiation and GC overhead
        val c1 = playerHand.cards[0]
        val c2 = playerHand.cards[1]
        val c3 = dealerUpcard

        return when {
            isSuitedTriple(
                c1,
                c2,
                c3
            ) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, SUITED_TRIPLE_PAYOUT, 0, SideBetOutcome.SUITED_TRIPLE)
            isStraightFlush(
                c1,
                c2,
                c3
            ) ->
                SideBetResult(
                    SideBetType.TWENTY_ONE_PLUS_THREE,
                    STRAIGHT_FLUSH_PAYOUT,
                    0,
                    SideBetOutcome.STRAIGHT_FLUSH
                )
            isThreeOfAKind(
                c1,
                c2,
                c3
            ) ->
                SideBetResult(
                    SideBetType.TWENTY_ONE_PLUS_THREE,
                    THREE_OF_A_KIND_PAYOUT,
                    0,
                    SideBetOutcome.THREE_OF_A_KIND
                )
            isStraight(
                c1,
                c2,
                c3
            ) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, STRAIGHT_PAYOUT, 0, SideBetOutcome.STRAIGHT)
            isFlush(
                c1,
                c2,
                c3
            ) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, FLUSH_PAYOUT, 0, SideBetOutcome.FLUSH)
            else -> null
        }
    }

    private fun isSameColor(
        s1: Suit,
        s2: Suit
    ): Boolean {
        return (s1 in RED_SUITS && s2 in RED_SUITS) || (s1 in BLACK_SUITS && s2 in BLACK_SUITS)
    }

    private fun isSuitedTriple(
        c1: Card,
        c2: Card,
        c3: Card
    ): Boolean = isThreeOfAKind(c1, c2, c3) && isFlush(c1, c2, c3)

    private fun isFlush(
        c1: Card,
        c2: Card,
        c3: Card
    ): Boolean {
        return c1.suit == c2.suit && c2.suit == c3.suit
    }

    private fun isThreeOfAKind(
        c1: Card,
        c2: Card,
        c3: Card
    ): Boolean {
        return c1.rank == c2.rank && c2.rank == c3.rank
    }

    private fun isStraight(
        c1: Card,
        c2: Card,
        c3: Card
    ): Boolean {
        var r0 = c1.rank.ordinal
        var r1 = c2.rank.ordinal
        var r2 = c3.rank.ordinal

        // Manual sort network for 3 elements
        if (r0 > r1) {
            val temp = r0
            r0 = r1
            r1 = temp
        }
        if (r1 > r2) {
            val temp = r1
            r1 = r2
            r2 = temp
        }
        if (r0 > r1) {
            val temp = r0
            r0 = r1
            r1 = temp
        }

        // Standard straight.
        if (r1 == r0 + 1 && r2 == r1 + 1) return true

        // Ace-low straight (A, 2, 3).
        // Rank ordinal: ACE is last (12). TWO is 0. THREE is 1.
        if (r0 == Rank.TWO.ordinal && r1 == Rank.THREE.ordinal && r2 == Rank.ACE.ordinal) return true

        return false
    }

    private fun isStraightFlush(
        c1: Card,
        c2: Card,
        c3: Card
    ): Boolean {
        return isFlush(c1, c2, c3) && isStraight(c1, c2, c3)
    }
}
