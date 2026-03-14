package io.github.smithjustinn.blackjack

import kotlin.test.Test
import kotlin.test.assertEquals

class HandTest {
    @Test
    fun testVisibleScore_countsOnlyFaceUpCards() {
        val hand =
            Hand(
                listOf(
                    Card(Rank.EIGHT, Suit.HEARTS),
                    Card(Rank.TEN, Suit.SPADES, isFaceDown = true)
                )
            )
        assertEquals(8, hand.visibleScore)
        assertEquals(18, hand.score)
    }

    @Test
    fun testVisibleScore_allFaceUp_matchesScore() {
        val hand =
            Hand(
                listOf(
                    Card(Rank.KING, Suit.HEARTS),
                    Card(Rank.SEVEN, Suit.SPADES)
                )
            )
        assertEquals(hand.score, hand.visibleScore)
    }

    @Test
    fun testVisibleScore_allFaceDown_returnsZero() {
        val hand =
            Hand(
                listOf(
                    Card(Rank.TEN, Suit.HEARTS, isFaceDown = true),
                    Card(Rank.ACE, Suit.SPADES, isFaceDown = true)
                )
            )
        assertEquals(0, hand.visibleScore)
    }

    @Test
    fun testVisibleScore_softensAceCorrectly() {
        // Face-up: ACE + NINE = 20 (not busting), face-down: TEN
        val hand =
            Hand(
                listOf(
                    Card(Rank.ACE, Suit.HEARTS),
                    Card(Rank.NINE, Suit.SPADES),
                    Card(Rank.TEN, Suit.CLUBS, isFaceDown = true)
                )
            )
        assertEquals(20, hand.visibleScore)
        assertEquals(20, hand.score)
    }
}
