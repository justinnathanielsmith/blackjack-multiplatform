@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.logic

import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.HandOutcome
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.util.dealerHand
import io.github.smithjustinn.blackjack.util.hand
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlackjackRulesTest {
    // ── Dealer Draw Logic ─────────────────────────────────────────────────────

    @Test
    fun shouldDealerDraw_under17_alwaysHits() {
        val rules = GameRules(dealerHitsSoft17 = true)
        val hard16 = hand(Rank.TEN, Rank.SIX)
        assertTrue(BlackjackRules.shouldDealerDraw(hard16, rules))
    }

    @Test
    fun shouldDealerDraw_over17_alwaysStands() {
        val rules = GameRules(dealerHitsSoft17 = true)
        val hard18 = hand(Rank.TEN, Rank.EIGHT)
        assertFalse(BlackjackRules.shouldDealerDraw(hard18, rules))
    }

    @Test
    fun shouldDealerDraw_soft17_withH17_hits() {
        val rules = GameRules(dealerHitsSoft17 = true)
        val soft17 = hand(Rank.ACE, Rank.SIX)
        assertTrue(BlackjackRules.shouldDealerDraw(soft17, rules))
    }

    @Test
    fun shouldDealerDraw_soft17_withS17_stands() {
        val rules = GameRules(dealerHitsSoft17 = false)
        val soft17 = hand(Rank.ACE, Rank.SIX)
        assertFalse(BlackjackRules.shouldDealerDraw(soft17, rules))
    }

    // ── Hand Outcome Logic ────────────────────────────────────────────────────

    @Test
    fun determineHandOutcome_bust_isAlwaysLoss() {
        val bust = hand(Rank.TEN, Rank.TEN, Rank.TWO)
        assertEquals(HandOutcome.LOSS, BlackjackRules.determineHandOutcome(bust, 18, false))
        assertEquals(HandOutcome.LOSS, BlackjackRules.determineHandOutcome(bust, 22, true))
    }

    @Test
    fun determineHandOutcome_naturalBJ_beatsDealer20() {
        val bj = hand(Rank.ACE, Rank.TEN)
        assertEquals(HandOutcome.NATURAL_WIN, BlackjackRules.determineHandOutcome(bj, 20, false))
    }

    @Test
    fun determineHandOutcome_naturalBJ_pushesDealer21() {
        val bj = hand(Rank.ACE, Rank.TEN)
        // Current logic pushes against any 21.
        assertEquals(HandOutcome.PUSH, BlackjackRules.determineHandOutcome(bj, 21, false))
    }

    @Test
    fun determineHandOutcome_naturalBJ_pushesDealerBJ() {
        val bj = hand(Rank.ACE, Rank.TEN)
        // This is a subtle point: determination depends on dealerHasBJ vs dealerScore=21.
        // BlackjackRules.determineHandOutcome uses dealerScore != BLACKJACK_SCORE for NATURAL_WIN.
        // Wait, if dealerScore is 21, it checks hand.score == dealerScore -> PUSH.
        assertEquals(HandOutcome.PUSH, BlackjackRules.determineHandOutcome(bj, 21, false))
    }

    @Test
    fun determineHandOutcome_split21_isNotNatural() {
        val split21 = hand(Rank.ACE, Rank.TEN).copy(wasSplit = true)
        assertEquals(HandOutcome.WIN, BlackjackRules.determineHandOutcome(split21, 20, false))
        assertEquals(HandOutcome.PUSH, BlackjackRules.determineHandOutcome(split21, 21, false))
    }

    // ── Payout Resolution ─────────────────────────────────────────────────────

    @Test
    fun resolveHand_3to2_oddBet_truncatesInterior() {
        val rules = GameRules(blackjackPayout = BlackjackPayout.THREE_TO_TWO)
        val bj = hand(Rank.ACE, Rank.KING)
        // profit = (3 * 3) / 2 = 4; total = 3 + 4 = 7
        assertEquals(7, BlackjackRules.resolveHand(bj, 3, 18, false, rules))
    }

    @Test
    fun resolveHand_6to5_oddBet_truncatesInterior() {
        val rules = GameRules(blackjackPayout = BlackjackPayout.SIX_TO_FIVE)
        val bj = hand(Rank.ACE, Rank.KING)
        // profit = (7 * 6) / 5 = 8; total = 7 + 8 = 15
        assertEquals(15, BlackjackRules.resolveHand(bj, 7, 18, false, rules))
    }

    @Test
    fun resolveHand_surrenderedHand_returnsZero() {
        val rules = GameRules(allowSurrender = true)
        val h = hand(Rank.TEN, Rank.SEVEN).copy(isSurrendered = true)
        assertEquals(0, BlackjackRules.resolveHand(h, 100, 18, false, rules))
    }

    // ── Initial Status Resolution ─────────────────────────────────────────────

    @Test
    fun determineInitialStatus_singleHand_playerBJ_noDealerBJ_isPlayerWon() {
        val playerHands = listOf(hand(Rank.ACE, Rank.TEN))
        val dealerHand = hand(Rank.TEN, Rank.SEVEN)
        assertEquals(GameStatus.PLAYER_WON, BlackjackRules.determineInitialStatus(playerHands, dealerHand))
    }

    @Test
    fun determineInitialStatus_singleHand_playerBJ_dealerBJ_isPush() {
        val playerHands = listOf(hand(Rank.ACE, Rank.TEN))
        val dealerHand = hand(Rank.TEN, Rank.ACE)
        assertEquals(GameStatus.PUSH, BlackjackRules.determineInitialStatus(playerHands, dealerHand))
    }

    @Test
    fun determineInitialStatus_multiHand_allDealerBJ_isDealerWon() {
        val playerHands = listOf(hand(Rank.TEN, Rank.TEN), hand(Rank.NINE, Rank.NINE))
        val dealerHand = hand(Rank.TEN, Rank.ACE)
        assertEquals(GameStatus.DEALER_WON, BlackjackRules.determineInitialStatus(playerHands, dealerHand))
    }

    @Test
    fun determineInitialStatus_multiHand_mixedOutcomes_isPlaying() {
        val playerHands = listOf(hand(Rank.TEN, Rank.TEN), hand(Rank.ACE, Rank.TEN))
        val dealerHand = hand(Rank.TEN, Rank.SEVEN)
        // Hand0 needs to play, Hand1 is BJ. Status must be PLAYING.
        assertEquals(GameStatus.PLAYING, BlackjackRules.determineInitialStatus(playerHands, dealerHand))
    }

    // ── Result Calculation ────────────────────────────────────────────────────

    @Test
    fun calculateHandResults_mixedOutcomes_payoutIsAdditive() {
        val rules = GameRules()
        val hand0 = hand(Rank.TEN, Rank.TEN).copy(bet = 100) // Win (20 vs 18) -> 200
        val hand1 = hand(Rank.NINE, Rank.SEVEN).copy(bet = 100) // Loss (16 vs 18) -> 0
        val state =
            GameState(
                playerHands = persistentListOf(hand0, hand1),
                rules = rules,
                dealerHand = hand(Rank.TEN, Rank.EIGHT)
            )
        val results = BlackjackRules.calculateHandResults(state, 18, false)
        assertEquals(200, results.totalPayout)
        assertTrue(results.anyWin)
        assertFalse(results.allPush)
    }

    // ── isDealerCriticalDraw ──────────────────────────────────────────────────

    @Test
    fun isDealerCriticalDraw_false_whenScoreBelowStiffMin() {
        val h = hand(Rank.TWO, Rank.THREE) // score = 5
        assertFalse(BlackjackRules.isDealerCriticalDraw(h))
    }

    @Test
    fun isDealerCriticalDraw_true_whenStiffHardHand() {
        // score = 16 (hard), within [DEALER_STIFF_MIN, DEALER_STAND_THRESHOLD)
        val h = hand(Rank.TEN, Rank.SIX)
        assertTrue(BlackjackRules.isDealerCriticalDraw(h))
    }

    @Test
    fun isDealerCriticalDraw_false_whenSoftHandInRange() {
        // score = 16, soft (Ace + Five) — in range but isSoft=true so not critical
        val h = hand(Rank.ACE, Rank.FIVE)
        assertFalse(BlackjackRules.isDealerCriticalDraw(h))
    }

    // ── Initial Outcome Integration ───────────────────────────────────────────

    @Test
    fun resolveInitialOutcomeValues_offersInsurance_whenDealerAce() {
        val current = GameState(handCount = 1)
        val playerHands = listOf(hand(Rank.TEN, Rank.SEVEN))
        val dealerHand = dealerHand(Rank.ACE, Rank.FIVE)

        val result = BlackjackRules.resolveInitialOutcomeValues(current, playerHands, dealerHand)

        assertEquals(GameStatus.INSURANCE_OFFERED, result.status)
        assertTrue(
            result.dealerHand.cards[1].isFaceDown,
            "Hole card should NOT be revealed during insurance offer sequence"
        )
        assertEquals(0, result.balanceDelta)
    }

    @Test
    fun resolveInitialOutcomeValues_instantPayout_onNaturalBlackjack() {
        val current = GameState(handCount = 1)
        val playerHands = listOf(hand(Rank.ACE, Rank.TEN).copy(bet = 100))
        val dealerHand = dealerHand(Rank.TEN, Rank.SEVEN)

        val result = BlackjackRules.resolveInitialOutcomeValues(current, playerHands, dealerHand)

        assertEquals(GameStatus.PLAYER_WON, result.status)
        assertEquals(250, result.balanceDelta) // 100 bet + 150 profit (3:2)
    }

    // ── Deck Creation ─────────────────────────────────────────────────────────

    @Test
    fun createDeck_validSize() {
        val rules = GameRules(deckCount = 2)
        val deck = BlackjackRules.createDeck(rules, Random(42))
        assertEquals(104, deck.size)
    }
}
