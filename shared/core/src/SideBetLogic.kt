package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

data class SideBetResolution(
    val payoutTotal: Int,
    val results: PersistentMap<SideBetType, SideBetResult>
)

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

    fun evaluatePerfectPairs(hand: Hand): SideBetResult? {
        if (hand.cards.size < 2) return null
        val c1 = hand.cards[0]
        val c2 = hand.cards[1]

        if (!isPair(c1, c2)) return null

        return when {
            c1.suit == c2.suit ->
                SideBetResult(SideBetType.PERFECT_PAIRS, PERFECT_PAIR_PAYOUT, 0, "Perfect Pair")
            isSameColor(c1.suit, c2.suit) ->
                SideBetResult(SideBetType.PERFECT_PAIRS, COLORED_PAIR_PAYOUT, 0, "Colored Pair")
            else ->
                SideBetResult(SideBetType.PERFECT_PAIRS, MIXED_PAIR_PAYOUT, 0, "Mixed Pair")
        }
    }

    private fun isPair(
        c1: Card,
        c2: Card
    ): Boolean {
        // Must be the same RANK (not just the same value)
        return c1.rank == c2.rank
    }

    fun evaluateTwentyOnePlusThree(
        playerHand: Hand,
        dealerUpcard: Card
    ): SideBetResult? {
        if (playerHand.cards.size < 2) return null
        val cards = listOf(playerHand.cards[0], playerHand.cards[1], dealerUpcard)

        return when {
            isSuitedTriple(
                cards
            ) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, SUITED_TRIPLE_PAYOUT, 0, "Suited Triple")
            isStraightFlush(
                cards
            ) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, STRAIGHT_FLUSH_PAYOUT, 0, "Straight Flush")
            isThreeOfAKind(
                cards
            ) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, THREE_OF_A_KIND_PAYOUT, 0, "Three of a Kind")
            isStraight(cards) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, STRAIGHT_PAYOUT, 0, "Straight")
            isFlush(cards) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, FLUSH_PAYOUT, 0, "Flush")
            else -> null
        }
    }

    private fun isSameColor(
        s1: Suit,
        s2: Suit
    ): Boolean {
        return (s1 in RED_SUITS && s2 in RED_SUITS) || (s1 in BLACK_SUITS && s2 in BLACK_SUITS)
    }

    private fun isSuitedTriple(cards: List<Card>): Boolean = isThreeOfAKind(cards) && isFlush(cards)

    private fun isFlush(cards: List<Card>): Boolean {
        return cards.all { it.suit == cards[0].suit }
    }

    private fun isThreeOfAKind(cards: List<Card>): Boolean {
        return cards.all { it.rank == cards[0].rank }
    }

    private fun isStraight(cards: List<Card>): Boolean {
        val sortedRanks = cards.map { it.rank.ordinal }.sorted()
        // Standard straight.
        if (sortedRanks[1] == sortedRanks[0] + 1 && sortedRanks[2] == sortedRanks[1] + 1) return true

        // Ace-low straight (A, 2, 3).
        // Rank ordinal: ACE is last (12). TWO is 0. THREE is 1.
        if (sortedRanks == listOf(Rank.TWO.ordinal, Rank.THREE.ordinal, Rank.ACE.ordinal)) return true

        return false
    }

    private fun isStraightFlush(cards: List<Card>): Boolean {
        return isFlush(cards) && isStraight(cards)
    }
}
