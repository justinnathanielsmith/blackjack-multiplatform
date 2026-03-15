package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SideBetLogicTest {
    @Test
    fun testPerfectPairs_Perfect() {
        val hand =
            Hand(
                persistentListOf(
                    Card(Rank.TEN, Suit.SPADES),
                    Card(Rank.TEN, Suit.SPADES)
                )
            )
        val result = SideBetLogic.evaluatePerfectPairs(hand)
        assertNotNull(result)
        assertEquals(25, result.payoutMultiplier)
        assertEquals("Perfect Pair", result.outcomeName)
    }

    @Test
    fun testPerfectPairs_Colored() {
        val hand =
            Hand(
                persistentListOf(
                    Card(Rank.TEN, Suit.SPADES),
                    Card(Rank.TEN, Suit.CLUBS)
                )
            )
        val result = SideBetLogic.evaluatePerfectPairs(hand)
        assertNotNull(result)
        assertEquals(12, result.payoutMultiplier)
        assertEquals("Colored Pair", result.outcomeName)
    }

    @Test
    fun testPerfectPairs_Mixed() {
        val hand =
            Hand(
                persistentListOf(
                    Card(Rank.TEN, Suit.SPADES),
                    Card(Rank.TEN, Suit.HEARTS)
                )
            )
        val result = SideBetLogic.evaluatePerfectPairs(hand)
        assertNotNull(result)
        assertEquals(5, result.payoutMultiplier)
        assertEquals("Mixed Pair", result.outcomeName)
    }

    @Test
    fun test21Plus3_SuitedTriple() {
        val playerHand =
            Hand(
                persistentListOf(
                    Card(Rank.KING, Suit.HEARTS),
                    Card(Rank.KING, Suit.HEARTS)
                )
            )
        val dealerUpcard = Card(Rank.KING, Suit.HEARTS)
        val result = SideBetLogic.evaluateTwentyOnePlusThree(playerHand, dealerUpcard)
        assertNotNull(result)
        assertEquals(100, result.payoutMultiplier)
        assertEquals("Suited Triple", result.outcomeName)
    }

    @Test
    fun test21Plus3_StraightFlush() {
        val playerHand =
            Hand(
                persistentListOf(
                    Card(Rank.NINE, Suit.CLUBS),
                    Card(Rank.TEN, Suit.CLUBS)
                )
            )
        val dealerUpcard = Card(Rank.JACK, Suit.CLUBS)
        val result = SideBetLogic.evaluateTwentyOnePlusThree(playerHand, dealerUpcard)
        assertNotNull(result)
        assertEquals(40, result.payoutMultiplier)
        assertEquals("Straight Flush", result.outcomeName)
    }

    @Test
    fun test21Plus3_Flush() {
        val playerHand =
            Hand(
                persistentListOf(
                    Card(Rank.TWO, Suit.DIAMONDS),
                    Card(Rank.SIX, Suit.DIAMONDS)
                )
            )
        val dealerUpcard = Card(Rank.ACE, Suit.DIAMONDS)
        val result = SideBetLogic.evaluateTwentyOnePlusThree(playerHand, dealerUpcard)
        assertNotNull(result)
        assertEquals(5, result.payoutMultiplier)
        assertEquals("Flush", result.outcomeName)
    }

    @Test
    fun test21Plus3_Straight_AceLow() {
        val playerHand =
            Hand(
                persistentListOf(
                    Card(Rank.ACE, Suit.SPADES),
                    Card(Rank.TWO, Suit.HEARTS)
                )
            )
        val dealerUpcard = Card(Rank.THREE, Suit.CLUBS)
        val result = SideBetLogic.evaluateTwentyOnePlusThree(playerHand, dealerUpcard)
        assertNotNull(result)
        assertEquals(10, result.payoutMultiplier)
        assertEquals("Straight", result.outcomeName)
    }
}
