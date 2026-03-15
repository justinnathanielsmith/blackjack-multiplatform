package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoringTest {

    @Test
    fun testHandScore_noAces() {
        val hand = Hand(persistentListOf(
            Card(Rank.TEN, Suit.HEARTS),
            Card(Rank.SEVEN, Suit.SPADES)
        ))
        assertEquals(17, hand.score)
        assertFalse(hand.isSoft)
    }

    @Test
    fun testHandScore_singleAce_hard() {
        val hand = Hand(persistentListOf(
            Card(Rank.TEN, Suit.HEARTS),
            Card(Rank.SEVEN, Suit.SPADES),
            Card(Rank.ACE, Suit.CLUBS)
        ))
        assertEquals(18, hand.score)
        assertFalse(hand.isSoft)
    }

    @Test
    fun testHandScore_singleAce_soft() {
        val hand = Hand(persistentListOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.SEVEN, Suit.SPADES)
        ))
        assertEquals(18, hand.score)
        assertTrue(hand.isSoft)
    }

    @Test
    fun testHandScore_multipleAces_soft() {
        val hand = Hand(persistentListOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.ACE, Suit.HEARTS)
        ))
        assertEquals(12, hand.score)
        assertTrue(hand.isSoft)
    }

    @Test
    fun testHandScore_multipleAces_hard() {
        val hand = Hand(persistentListOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.TEN, Suit.SPADES)
        ))
        assertEquals(12, hand.score)
        assertFalse(hand.isSoft)
    }

    @Test
    fun testHandScore_multipleAces_complex_soft() {
        val hand = Hand(persistentListOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.ACE, Suit.SPADES),
            Card(Rank.SIX, Suit.DIAMONDS)
        ))
        // 11 + 1 + 1 + 6 = 19
        assertEquals(19, hand.score)
        assertTrue(hand.isSoft)
    }

    @Test
    fun testHandScore_multipleAces_complex_hard() {
        val hand = Hand(persistentListOf(
            Card(Rank.ACE, Suit.CLUBS),
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.ACE, Suit.SPADES),
            Card(Rank.TEN, Suit.DIAMONDS)
        ))
        // 1 + 1 + 1 + 10 = 13
        assertEquals(13, hand.score)
        assertFalse(hand.isSoft)
    }

    @Test
    fun testHandScore_bust() {
        val hand = Hand(persistentListOf(
            Card(Rank.TEN, Suit.CLUBS),
            Card(Rank.KING, Suit.HEARTS),
            Card(Rank.TWO, Suit.SPADES)
        ))
        assertEquals(22, hand.score)
        assertTrue(hand.isBust)
    }

    @Test
    fun testHandScore_aceCanPreventBust() {
        val hand = Hand(persistentListOf(
            Card(Rank.TEN, Suit.CLUBS),
            Card(Rank.KING, Suit.HEARTS),
            Card(Rank.ACE, Suit.SPADES)
        ))
        // 10 + 10 + 1 = 21
        assertEquals(21, hand.score)
        assertFalse(hand.isBust)
        assertFalse(hand.isSoft)
    }
}
