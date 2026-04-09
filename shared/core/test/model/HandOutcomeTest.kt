package io.github.smithjustinn.blackjack.model
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import io.github.smithjustinn.blackjack.util.*
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

class HandOutcomeTest {
    @Test
    fun testPlayerBust_isAlwaysLoss() {
        // Player hand: 22 (bust)
        val h = hand(Rank.TEN, Rank.KING, Rank.TWO)
        // Even if dealer also busts or has lower score
        assertEquals(HandOutcome.LOSS, BlackjackRules.determineHandOutcome(h, dealerScore = 22, dealerBust = true))
        assertEquals(HandOutcome.LOSS, BlackjackRules.determineHandOutcome(h, dealerScore = 18, dealerBust = false))
    }

    @Test
    fun testNaturalBlackjack_dealerNoBlackjack_isNaturalWin() {
        // Natural BJ: 2 cards, 21 score, not from split
        val h = hand(Rank.ACE, Rank.KING)
        assertEquals(
            HandOutcome.NATURAL_WIN,
            BlackjackRules.determineHandOutcome(h, dealerScore = 20, dealerBust = false)
        )
        assertEquals(
            HandOutcome.NATURAL_WIN,
            BlackjackRules.determineHandOutcome(h, dealerScore = 22, dealerBust = true)
        )
    }

    @Test
    fun testNaturalBlackjack_dealerHasBlackjack_isPush() {
        val h = hand(Rank.ACE, Rank.KING)
        assertEquals(HandOutcome.PUSH, BlackjackRules.determineHandOutcome(h, dealerScore = 21, dealerBust = false))
    }

    @Test
    fun testPlayer21_notNatural_dealerNoBlackjack_isWin() {
        // 3 cards 21
        val h = hand(Rank.SEVEN, Rank.EIGHT, Rank.SIX)
        assertEquals(HandOutcome.WIN, BlackjackRules.determineHandOutcome(h, dealerScore = 20, dealerBust = false))

        // Split 21 (2 cards, score 21, but wasSplit = true)
        val splitHand = hand(Rank.ACE, Rank.KING).copy(wasSplit = true)
        assertEquals(
            HandOutcome.WIN,
            BlackjackRules.determineHandOutcome(splitHand, dealerScore = 20, dealerBust = false)
        )
    }

    @Test
    fun testPlayer21_notNatural_dealerHas21_isPush() {
        val h = hand(Rank.SEVEN, Rank.EIGHT, Rank.SIX)
        assertEquals(HandOutcome.PUSH, BlackjackRules.determineHandOutcome(h, dealerScore = 21, dealerBust = false))

        val splitHand = hand(Rank.ACE, Rank.KING).copy(wasSplit = true)
        assertEquals(
            HandOutcome.PUSH,
            BlackjackRules.determineHandOutcome(splitHand, dealerScore = 21, dealerBust = false)
        )
    }

    @Test
    fun testDealerBust_playerNotBust_isWin() {
        val h = hand(Rank.TEN, Rank.SEVEN) // 17
        assertEquals(HandOutcome.WIN, BlackjackRules.determineHandOutcome(h, dealerScore = 22, dealerBust = true))
    }

    @Test
    fun testPlayerHigherScore_isWin() {
        val h = hand(Rank.TEN, Rank.NINE) // 19
        assertEquals(HandOutcome.WIN, BlackjackRules.determineHandOutcome(h, dealerScore = 18, dealerBust = false))
    }

    @Test
    fun testPlayerEqualScore_isPush() {
        val h = hand(Rank.TEN, Rank.EIGHT) // 18
        assertEquals(HandOutcome.PUSH, BlackjackRules.determineHandOutcome(h, dealerScore = 18, dealerBust = false))
    }

    @Test
    fun testPlayerLowerScore_isLoss() {
        val h = hand(Rank.TEN, Rank.SEVEN) // 17
        assertEquals(HandOutcome.LOSS, BlackjackRules.determineHandOutcome(h, dealerScore = 18, dealerBust = false))
    }

    // ── calculateHandResults bug regression ───────────────────────────────────

    @Test
    fun calculateHandResults_naturalBJvsDealerBJ_anyWinIsFalse() {
        // Natural BJ (ACE+KING) vs dealer 21 → PUSH, not WIN. anyWin must be false.
        val playerHand = hand(Rank.ACE, Rank.KING)
        val state =
            GameState(
                status = GameStatus.DEALER_TURN,
                balance = 900,
                playerHands = persistentListOf(playerHand.copy(bet = 100)),
                dealerHand = hand(Rank.ACE, Rank.KING),
            )
        val results = BlackjackRules.calculateHandResults(state, dealerScore = 21, dealerBust = false)
        assertEquals(false, results.anyWin, "Natural BJ vs dealer BJ should NOT be a win")
        assertEquals(true, results.allPush, "Natural BJ vs dealer BJ should be a push")
        assertEquals(100, results.totalPayout, "Push returns the original bet")
    }
}
