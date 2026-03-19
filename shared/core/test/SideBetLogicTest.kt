package io.github.smithjustinn.blackjack

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentMapOf

class SideBetLogicTest {
    @Test
    fun testPerfectPairs_Perfect() {
        val result =
            SideBetLogic.evaluatePerfectPairs(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.TEN, Suit.SPADES),
                        card(Rank.TEN, Suit.SPADES),
                    )
                ),
            )
        assertNotNull(result)
        assertEquals(25, result.payoutMultiplier)
        assertEquals("Perfect Pair", result.outcomeName)
    }

    @Test
    fun testPerfectPairs_Colored() {
        val result =
            SideBetLogic.evaluatePerfectPairs(
                hand(Rank.TEN, Rank.TEN), // uses default SPADES — both same color but different suits?
            )
        // Default is SPADES+SPADES = Perfect Pair; use explicit cards for Colored
        val coloredResult =
            SideBetLogic.evaluatePerfectPairs(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.TEN, Suit.SPADES),
                        card(Rank.TEN, Suit.CLUBS),
                    )
                ),
            )
        assertNotNull(coloredResult)
        assertEquals(12, coloredResult.payoutMultiplier)
        assertEquals("Colored Pair", coloredResult.outcomeName)
    }

    @Test
    fun testPerfectPairs_Mixed() {
        val result =
            SideBetLogic.evaluatePerfectPairs(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.TEN, Suit.SPADES),
                        card(Rank.TEN, Suit.HEARTS),
                    )
                ),
            )
        assertNotNull(result)
        assertEquals(5, result.payoutMultiplier)
        assertEquals("Mixed Pair", result.outcomeName)
    }

    @Test
    fun test21Plus3_SuitedTriple() {
        val result =
            SideBetLogic.evaluateTwentyOnePlusThree(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.KING, Suit.HEARTS),
                        card(Rank.KING, Suit.HEARTS),
                    )
                ),
                card(Rank.KING, Suit.HEARTS),
            )
        assertNotNull(result)
        assertEquals(100, result.payoutMultiplier)
        assertEquals("Suited Triple", result.outcomeName)
    }

    @Test
    fun test21Plus3_StraightFlush() {
        val result =
            SideBetLogic.evaluateTwentyOnePlusThree(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.NINE, Suit.CLUBS),
                        card(Rank.TEN, Suit.CLUBS),
                    )
                ),
                card(Rank.JACK, Suit.CLUBS),
            )
        assertNotNull(result)
        assertEquals(40, result.payoutMultiplier)
        assertEquals("Straight Flush", result.outcomeName)
    }

    @Test
    fun test21Plus3_Flush() {
        val result =
            SideBetLogic.evaluateTwentyOnePlusThree(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.TWO, Suit.DIAMONDS),
                        card(Rank.SIX, Suit.DIAMONDS),
                    )
                ),
                card(Rank.ACE, Suit.DIAMONDS),
            )
        assertNotNull(result)
        assertEquals(5, result.payoutMultiplier)
        assertEquals("Flush", result.outcomeName)
    }

    @Test
    fun test21Plus3_Straight_AceLow() {
        val result =
            SideBetLogic.evaluateTwentyOnePlusThree(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.ACE, Suit.SPADES),
                        card(Rank.TWO, Suit.HEARTS),
                    )
                ),
                card(Rank.THREE, Suit.CLUBS),
            )
        assertNotNull(result)
        assertEquals(10, result.payoutMultiplier)
        assertEquals("Straight", result.outcomeName)
    }

    @Test
    fun test21Plus3_Straight_Regular() {
        val result =
            SideBetLogic.evaluateTwentyOnePlusThree(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.FOUR, Suit.SPADES),
                        card(Rank.FIVE, Suit.HEARTS),
                    )
                ),
                card(Rank.SIX, Suit.CLUBS),
            )
        assertNotNull(result)
        assertEquals(10, result.payoutMultiplier)
        assertEquals("Straight", result.outcomeName)
    }

    @Test
    fun test21Plus3_ThreeOfAKind() {
        val result =
            SideBetLogic.evaluateTwentyOnePlusThree(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.QUEEN, Suit.SPADES),
                        card(Rank.QUEEN, Suit.HEARTS),
                    )
                ),
                card(Rank.QUEEN, Suit.CLUBS),
            )
        assertNotNull(result)
        assertEquals(30, result.payoutMultiplier)
        assertEquals("Three of a Kind", result.outcomeName)
    }

    @Test
    fun testResolveSideBets_win() {
        val sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 10)
        val playerHand = Hand(
            kotlinx.collections.immutable.persistentListOf(
                card(Rank.TEN, Suit.SPADES),
                card(Rank.TEN, Suit.SPADES),
            )
        )
        val resolution = SideBetLogic.resolveSideBets(sideBets, playerHand, card(Rank.FIVE, Suit.CLUBS))
        // Perfect Pair = 25x multiplier; payout = 10 * 25 + 10 = 260
        assertEquals(260, resolution.payoutTotal)
        assertTrue(resolution.results.containsKey(SideBetType.PERFECT_PAIRS))
    }

    @Test
    fun testResolveSideBets_loss() {
        val sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 10)
        val playerHand = Hand(
            kotlinx.collections.immutable.persistentListOf(
                card(Rank.TEN, Suit.SPADES),
                card(Rank.FIVE, Suit.HEARTS),
            )
        )
        val resolution = SideBetLogic.resolveSideBets(sideBets, playerHand, card(Rank.KING, Suit.CLUBS))
        assertEquals(0, resolution.payoutTotal)
        assertTrue(resolution.results.isEmpty())
    }

    @Test
    fun testPerfectPairs_NoMatch() {
        val result =
            SideBetLogic.evaluatePerfectPairs(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.TEN, Suit.SPADES),
                        card(Rank.FIVE, Suit.HEARTS),
                    )
                )
            )
        assertNull(result)
    }

    @Test
    fun test21Plus3_Straight_AceHigh() {
        // Q-K-A straight
        val result =
            SideBetLogic.evaluateTwentyOnePlusThree(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.QUEEN, Suit.SPADES),
                        card(Rank.KING, Suit.HEARTS),
                    )
                ),
                card(Rank.ACE, Suit.CLUBS),
            )
        assertNotNull(result)
        assertEquals(10, result.payoutMultiplier)
        assertEquals("Straight", result.outcomeName)
    }

    @Test
    fun test21Plus3_Straight_FaceCards() {
        // J-Q-K straight
        val result =
            SideBetLogic.evaluateTwentyOnePlusThree(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.JACK, Suit.SPADES),
                        card(Rank.QUEEN, Suit.HEARTS),
                    )
                ),
                card(Rank.KING, Suit.CLUBS),
            )
        assertNotNull(result)
        assertEquals(10, result.payoutMultiplier)
        assertEquals("Straight", result.outcomeName)
    }

    @Test
    fun test21Plus3_NoMatch() {
        val result =
            SideBetLogic.evaluateTwentyOnePlusThree(
                Hand(
                    kotlinx.collections.immutable.persistentListOf(
                        card(Rank.TWO, Suit.SPADES),
                        card(Rank.SEVEN, Suit.HEARTS),
                    )
                ),
                card(Rank.KING, Suit.CLUBS),
            )
        assertNull(result)
    }
}
