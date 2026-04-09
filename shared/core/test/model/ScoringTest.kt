package io.github.smithjustinn.blackjack.model
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.util.hand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoringTest {
    private fun assertScore(
        expected: Int,
        isSoft: Boolean,
        vararg ranks: Rank
    ) {
        val h = hand(*ranks)
        assertEquals(expected, h.score)
        assertEquals(isSoft, h.isSoft)
    }

    @Test
    fun aceValuations() {
        assertScore(17, isSoft = false, Rank.TEN, Rank.SEVEN) // no aces
        assertScore(18, isSoft = false, Rank.TEN, Rank.SEVEN, Rank.ACE) // hard 18
        assertScore(18, isSoft = true, Rank.ACE, Rank.SEVEN) // soft 18
        assertScore(12, isSoft = true, Rank.ACE, Rank.ACE) // two aces
        assertScore(12, isSoft = false, Rank.ACE, Rank.ACE, Rank.TEN) // two aces + ten
        assertScore(21, isSoft = false, Rank.TEN, Rank.KING, Rank.ACE) // ace prevents bust
        assertScore(22, isSoft = false, Rank.TEN, Rank.KING, Rank.TWO) // bust
        assertScore(19, isSoft = true, Rank.ACE, Rank.ACE, Rank.ACE, Rank.SIX) // three aces + six
        assertScore(13, isSoft = false, Rank.ACE, Rank.ACE, Rank.ACE, Rank.TEN) // three aces + ten
    }

    @Test
    fun isBust_trueWhenScoreOver21() {
        assertTrue(hand(Rank.TEN, Rank.KING, Rank.TWO).isBust)
    }

    @Test
    fun isBust_falseWhenScoreEqualOrBelow21() {
        assertFalse(hand(Rank.TEN, Rank.KING, Rank.ACE).isBust)
    }
}
