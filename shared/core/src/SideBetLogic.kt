package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

data class SideBetResolution(
    val payoutTotal: Int,
    val results: PersistentMap<SideBetType, SideBetResult>
)

object SideBetLogic {
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
                val payout = amount * result.payoutMultiplier
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

        return when {
            c1.rank == c2.rank && c1.suit == c2.suit ->
                SideBetResult(SideBetType.PERFECT_PAIRS, 25, 0, "Perfect Pair")
            c1.rank == c2.rank && isSameColor(c1.suit, c2.suit) ->
                SideBetResult(SideBetType.PERFECT_PAIRS, 12, 0, "Colored Pair")
            c1.rank == c2.rank ->
                SideBetResult(SideBetType.PERFECT_PAIRS, 5, 0, "Mixed Pair")
            else -> null
        }
    }

    fun evaluateTwentyOnePlusThree(
        playerHand: Hand,
        dealerUpcard: Card
    ): SideBetResult? {
        if (playerHand.cards.size < 2) return null
        val cards = listOf(playerHand.cards[0], playerHand.cards[1], dealerUpcard)

        return when {
            isSuitedTriple(cards) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, 100, 0, "Suited Triple")
            isStraightFlush(cards) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, 40, 0, "Straight Flush")
            isThreeOfAKind(cards) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, 30, 0, "Three of a Kind")
            isStraight(cards) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, 10, 0, "Straight")
            isFlush(cards) -> SideBetResult(SideBetType.TWENTY_ONE_PLUS_THREE, 5, 0, "Flush")
            else -> null
        }
    }

    private fun isSameColor(
        s1: Suit,
        s2: Suit
    ): Boolean {
        return (s1 in RED_SUITS && s2 in RED_SUITS) || (s1 in BLACK_SUITS && s2 in BLACK_SUITS)
    }

    private fun isSuitedTriple(cards: List<Card>): Boolean {
        return cards.all { it.rank == cards[0].rank && it.suit == cards[0].suit }
    }

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
        if (sortedRanks == listOf(0, 1, 12)) return true

        return false
    }

    private fun isStraightFlush(cards: List<Card>): Boolean {
        return isFlush(cards) && isStraight(cards)
    }
}
