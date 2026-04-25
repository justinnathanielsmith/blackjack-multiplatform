package io.github.smithjustinn.blackjack.model
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.SideBetOutcome
import io.github.smithjustinn.blackjack.model.SideBetResult
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.util.dealerHand
import io.github.smithjustinn.blackjack.util.hand
import io.github.smithjustinn.blackjack.util.multiHandPlayingState
import io.github.smithjustinn.blackjack.util.playingState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun isPlayingPhase_trueOnlyDuringPlayingStatus() {
        val playing =
            playingState(
                playerHand = hand(Rank.TEN, Rank.SIX),
                dealerHand = dealerHand(upRank = Rank.TEN, holeRank = Rank.SEVEN),
            )
        assertTrue(playing.isPlayingPhase)

        val betting = GameState(status = GameStatus.BETTING)
        assertFalse(betting.isPlayingPhase)

        val terminal = playing.copy(status = GameStatus.PLAYER_WON)
        assertFalse(terminal.isPlayingPhase)
    }

    @Test
    fun isDealerActive_trueOnlyWhenActiveHandIndexIsNegativeOneDuringPlay() {
        val base =
            playingState(
                playerHand = hand(Rank.TEN, Rank.SIX),
                dealerHand = dealerHand(upRank = Rank.TEN, holeRank = Rank.SEVEN),
            )

        // Player turn: activeHandIndex = 0 → dealer not active
        assertFalse(base.isDealerActive)

        // Dealer's "turn" sentinel (-1) during PLAYING → dealer active
        val dealerSentinel = base.copy(activeHandIndex = -1)
        assertTrue(dealerSentinel.isDealerActive)

        // Sentinel outside PLAYING phase → not active
        val terminal = dealerSentinel.copy(status = GameStatus.DEALER_WON)
        assertFalse(terminal.isDealerActive)
    }

    // -- isDealerSlowRoll: dealer blackjack + Ace/10-value upcard predicate --

    @Test
    fun isDealerSlowRoll_trueWhenDealerBlackjackWithAceUpcard() {
        val state = GameState(dealerHand = dealerHand(upRank = Rank.ACE, holeRank = Rank.KING))
        assertTrue(state.isDealerSlowRoll)
    }

    @Test
    fun isDealerSlowRoll_trueWhenDealerBlackjackWithTenValueUpcard() {
        val state = GameState(dealerHand = dealerHand(upRank = Rank.TEN, holeRank = Rank.ACE))
        assertTrue(state.isDealerSlowRoll)
    }

    @Test
    fun isDealerSlowRoll_falseWhenDealerDoesNotHaveBlackjack() {
        // Dealer has 17 (Ten + Seven), not a natural blackjack
        val state = GameState(dealerHand = dealerHand(upRank = Rank.TEN, holeRank = Rank.SEVEN))
        assertFalse(state.isDealerSlowRoll)
    }

    @Test
    fun isHandActive_matchesActiveHandIndexDuringPlay() {
        val multi =
            multiHandPlayingState(
                balance = 1000,
                hands = listOf(hand(Rank.FIVE, Rank.SIX), hand(Rank.TEN, Rank.NINE)),
                bets = listOf(100, 100),
                activeHandIndex = 1,
                dealerHand = dealerHand(upRank = Rank.TEN, holeRank = Rank.SEVEN),
            )
        assertFalse(multi.isHandActive(0))
        assertTrue(multi.isHandActive(1))

        // Outside PLAYING phase, no hand is active.
        val betting = multi.copy(status = GameStatus.BETTING)
        assertFalse(betting.isHandActive(0))
        assertFalse(betting.isHandActive(1))
    }
}
