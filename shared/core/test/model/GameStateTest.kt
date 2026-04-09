package io.github.smithjustinn.blackjack.model
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import io.github.smithjustinn.blackjack.util.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class GameStateTest {
    @Test
    fun totalBet_calculatesCorrectly_inBettingPhase() {
        val state =
            GameState(
                status = GameStatus.BETTING,
                playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100), Hand(bet = 100)),
                handCount = 3,
                sideBets =
                    persistentMapOf(
                        SideBetType.PERFECT_PAIRS to 50,
                        SideBetType.TWENTY_ONE_PLUS_THREE to 25
                    )
            )
        // (100 + 100 + 100) + 50 + 25 = 375
        assertEquals(375, state.totalBet)
    }

    @Test
    fun totalBet_calculatesCorrectly_inPlayingPhase() {
        val state =
            GameState(
                status = GameStatus.PLAYING,
                playerHands =
                    persistentListOf(
                        hand(Rank.FIVE, Rank.SIX).copy(bet = 100),
                        hand(Rank.TEN, Rank.NINE).copy(bet = 200)
                    ),
                sideBets =
                    persistentMapOf(
                        SideBetType.PERFECT_PAIRS to 50
                    ),
                insuranceBet = 50
            )
        // (100 + 200) + 50 + 50 = 400
        assertEquals(400, state.totalBet)
    }

    @Test
    fun totalBet_excludesSideBets_whenSettled() {
        val state =
            GameState(
                status = GameStatus.PLAYING,
                playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                sideBets =
                    persistentMapOf(
                        SideBetType.PERFECT_PAIRS to 50
                    ),
                sideBetResults =
                    persistentMapOf(
                        SideBetType.PERFECT_PAIRS to
                            SideBetResult(SideBetType.PERFECT_PAIRS, 0, 0, SideBetOutcome.MIXED_PAIR)
                    )
            )
        // 100 main bet + 0 side bet (settled) = 100
        assertEquals(100, state.totalBet)
    }

    @Test
    fun totalBet_isZero_whenNoBets() {
        val state = GameState(status = GameStatus.BETTING)
        assertEquals(0, state.totalBet)
    }

    @Test
    fun totalNetPayout_returnsNull_whenNotTerminal() {
        val state =
            playingState(
                playerHand = hand(Rank.TEN, Rank.SIX),
                dealerHand = hand(Rank.TEN, Rank.SEVEN)
            )
        assertEquals(null, state.totalNetPayout())
    }

    @Test
    fun totalNetPayout_calculatesCorrectly_forSingleHandWin() {
        val state =
            playingState(
                bet = 100,
                playerHand = hand(Rank.TEN, Rank.TEN),
                dealerHand = hand(Rank.TEN, Rank.NINE)
            ).copy(status = GameStatus.PLAYER_WON)
        // Win returns 2*bet = 200. Net = 200 - 100 = 100.
        assertEquals(100, state.totalNetPayout())
    }

    @Test
    fun totalNetPayout_calculatesCorrectly_forSingleHandLoss() {
        val state =
            playingState(
                bet = 100,
                playerHand = hand(Rank.TEN, Rank.SEVEN),
                dealerHand = hand(Rank.TEN, Rank.EIGHT)
            ).copy(status = GameStatus.DEALER_WON)
        // Loss returns 0. Net = 0 - 100 = -100.
        assertEquals(-100, state.totalNetPayout())
    }

    @Test
    fun totalNetPayout_calculatesCorrectly_forSingleHandPush() {
        val state =
            playingState(
                bet = 100,
                playerHand = hand(Rank.TEN, Rank.EIGHT),
                dealerHand = hand(Rank.TEN, Rank.EIGHT)
            ).copy(status = GameStatus.PUSH)
        // Push returns 1*bet = 100. Net = 100 - 100 = 0.
        assertEquals(0, state.totalNetPayout())
    }

    @Test
    fun totalNetPayout_calculatesCorrectly_forNaturalBlackjack() {
        val state =
            playingState(
                bet = 100,
                playerHand = hand(Rank.ACE, Rank.JACK),
                dealerHand = hand(Rank.TEN, Rank.NINE)
            ).copy(status = GameStatus.PLAYER_WON)
        // Blackjack returns 2.5*bet (3:2) = 250. Net = 250 - 100 = 150.
        assertEquals(150, state.totalNetPayout())
    }

    @Test
    fun totalNetPayout_calculatesCorrectly_forMultiHand() {
        val state =
            multiHandPlayingState(
                balance = 1000,
                hands =
                    listOf(
                        hand(Rank.TEN, Rank.TEN), // Win
                        hand(Rank.TEN, Rank.SEVEN), // Loss
                        hand(Rank.TEN, Rank.EIGHT) // Push
                    ),
                bets = listOf(100, 100, 100),
                dealerHand = hand(Rank.TEN, Rank.EIGHT)
            ).copy(status = GameStatus.PLAYER_WON)
        // Hand 1: 200 - 100 = 100
        // Hand 2: 0 - 100 = -100
        // Hand 3: 100 - 100 = 0
        // Total = 100 - 100 + 0 = 0
        assertEquals(0, state.totalNetPayout())
    }

    // -- dealerDisplayScore: phase-visibility predicate --

    @Test
    fun dealerDisplayScore_returnsVisibleScore_duringPlaying() {
        val state =
            GameState(
                status = GameStatus.PLAYING,
                dealerHand = dealerHand(upRank = Rank.TEN, holeRank = Rank.SEVEN),
            )
        // Only the face-up Ten (10) should be visible
        assertEquals(10, state.dealerDisplayScore)
    }

    @Test
    fun dealerDisplayScore_returnsVisibleScore_duringDealing() {
        val state =
            GameState(
                status = GameStatus.DEALING,
                dealerHand = dealerHand(upRank = Rank.ACE, holeRank = Rank.KING),
            )
        // Only the face-up Ace (11) should be visible
        assertEquals(11, state.dealerDisplayScore)
    }

    @Test
    fun dealerDisplayScore_returnsFullScore_duringDealerTurn() {
        val state =
            GameState(
                status = GameStatus.DEALER_TURN,
                dealerHand = dealerHand(upRank = Rank.TEN, holeRank = Rank.SEVEN),
            )
        assertEquals(17, state.dealerDisplayScore)
    }

    @Test
    fun dealerDisplayScore_returnsFullScore_whenPlayerWon() {
        val state =
            GameState(
                status = GameStatus.PLAYER_WON,
                dealerHand = dealerHand(upRank = Rank.TEN, holeRank = Rank.SEVEN),
            )
        assertEquals(17, state.dealerDisplayScore)
    }

    @Test
    fun dealerDisplayScore_returnsFullScore_whenDealerWon() {
        val state =
            GameState(
                status = GameStatus.DEALER_WON,
                dealerHand = dealerHand(upRank = Rank.TEN, holeRank = Rank.SEVEN),
            )
        assertEquals(17, state.dealerDisplayScore)
    }

    @Test
    fun dealerDisplayScore_returnsFullScore_whenPush() {
        val state =
            GameState(
                status = GameStatus.PUSH,
                dealerHand = dealerHand(upRank = Rank.TEN, holeRank = Rank.SEVEN),
            )
        assertEquals(17, state.dealerDisplayScore)
    }

    @Test
    fun totalNetPayout_calculatesCorrectly_forSurrenderedHand() {
        val state =
            playingState(
                bet = 100,
                playerHand = hand(Rank.TEN, Rank.SIX).copy(isSurrendered = true),
                dealerHand = hand(Rank.TEN, Rank.SEVEN)
            ).copy(status = GameStatus.DEALER_WON)
        // Surrender refunds half (50). Net loss is -50.
        // Current buggy implementation returns 0 - 100 = -100.
        assertEquals(-50, state.totalNetPayout())
    }
}
