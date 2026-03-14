package io.github.smithjustinn.blackjack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BlackjackStateMachineTest {
    @Test
    fun testPlaceBet_decreasesBalanceAndIncreasesCurrentBet() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(900, state.balance)
            assertEquals(100, state.currentBet)
            assertEquals(GameStatus.BETTING, state.status)
        }

    @Test
    fun testPlaceBet_multipleChipsAccumulate() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            stateMachine.dispatch(GameAction.PlaceBet(50))
            advanceUntilIdle()
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(850, state.balance)
            assertEquals(150, state.currentBet)
        }

    @Test
    fun testPlaceBet_rejectedWhenAmountExceedsBalance() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 100, currentBet = 0))
            stateMachine.dispatch(GameAction.PlaceBet(200))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(100, state.balance)
            assertEquals(0, state.currentBet)
        }

    @Test
    fun testPlaceBet_rejectedWhenAmountZeroOrNegative() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            stateMachine.dispatch(GameAction.PlaceBet(0))
            advanceUntilIdle()
            assertEquals(1000, stateMachine.state.value.balance)
            assertEquals(0, stateMachine.state.value.currentBet)

            stateMachine.dispatch(GameAction.PlaceBet(-50))
            advanceUntilIdle()
            assertEquals(1000, stateMachine.state.value.balance)
            assertEquals(0, stateMachine.state.value.currentBet)
        }

    @Test
    fun testPlaceBet_ignoredWhenNotInBettingPhase() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.PLAYING, balance = 1000, currentBet = 100))
            stateMachine.dispatch(GameAction.PlaceBet(50))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1000, state.balance)
            assertEquals(100, state.currentBet)
        }

    @Test
    fun testResetBet_restoresBalanceAndClearsCurrentBet() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 900, currentBet = 100))
            stateMachine.dispatch(GameAction.ResetBet)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1000, state.balance)
            assertEquals(0, state.currentBet)
            assertEquals(GameStatus.BETTING, state.status)
        }

    @Test
    fun testResetBet_ignoredWhenNotInBettingPhase() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.PLAYING, balance = 900, currentBet = 100))
            stateMachine.dispatch(GameAction.ResetBet)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(900, state.balance)
            assertEquals(100, state.currentBet)
        }

    @Test
    fun testDeal_ignoredWhenNoBet() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.BETTING, state.status)
            assertEquals(0, state.playerHand.cards.size)
            assertEquals(0, state.dealerHand.cards.size)
        }

    @Test
    fun testInitialDeal() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(2, state.playerHand.cards.size)
            assertEquals(2, state.dealerHand.cards.size)
            assertEquals(48, state.deck.size)
        }

    @Test
    fun testPlayerHit() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // Skip test if initial deal resulted in blackjack (game already over)
            if (stateMachine.state.value.status != GameStatus.PLAYING) return@runTest

            val initialPlayerCards = stateMachine.state.value.playerHand.cards.size
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            assertEquals(initialPlayerCards + 1, stateMachine.state.value.playerHand.cards.size)
            assertEquals(47, stateMachine.state.value.deck.size)
        }

    @Test
    fun testBust() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // Hit until bust
            while (stateMachine.state.value.status == GameStatus.PLAYING) {
                stateMachine.dispatch(GameAction.Hit)
                advanceUntilIdle()
            }

            if (stateMachine.state.value.playerHand.isBust) {
                assertEquals(GameStatus.DEALER_WON, stateMachine.state.value.status)
            }
        }

    @Test
    fun testPlayerWin_afterStand_rewardsBalance() =
        runTest {
            // Player: TEN + KING = 20, Dealer: TEN + SEVEN = 17 (won't draw, deck empty)
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.KING, Suit.HEARTS))),
                        dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1100, state.balance) // 900 + 100 * 2 (bet returned + equal winnings)
        }

    @Test
    fun testPush_afterStand_returnsBet() =
        runTest {
            // Player: TEN + NINE = 19, Dealer: TEN + NINE = 19
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.NINE, Suit.HEARTS))),
                        dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS))),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.PUSH, state.status)
            assertEquals(1000, state.balance) // 900 + 100 (bet returned)
        }

    @Test
    fun testDealerWin_afterStand_keepsBalanceUnchanged() =
        runTest {
            // Player: TEN + SIX = 16, Dealer: TEN + NINE = 19
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                        dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS))),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertEquals(900, state.balance) // unchanged, bet is lost
        }
}
