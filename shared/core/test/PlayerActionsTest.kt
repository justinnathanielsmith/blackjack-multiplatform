@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerActionsTest {
    // ── Hit ───────────────────────────────────────────────────────────────────

    @Test
    fun hit_addsCardToPlayerHand() =
        runTest {
            // Player EIGHT+THREE=11, deck has FIVE → 16 (no bust, game continues)
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.FIVE),
                    ),
                )
            sm.dispatch(GameAction.Hit)
            advanceUntilIdle()

            assertEquals(
                3,
                sm.state.value.playerHands[0]
                    .cards.size
            )
            assertEquals(GameStatus.PLAYING, sm.state.value.status)
            assertEquals(0, sm.state.value.deck.size)
        }

    @Test
    fun hit_bust_endsRoundWithDealerWon() =
        runTest {
            // Player TEN+EIGHT=18, deck has TEN → 28 (guaranteed bust)
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.EIGHT),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.TEN),
                    ),
                )
            sm.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = sm.state.value
            assertTrue(state.playerHands[0].isBust)
            assertEquals(GameStatus.DEALER_WON, state.status)
        }

    // ── Stand ─────────────────────────────────────────────────────────────────

    @Test
    fun stand_playerWin_rewardsBalance() =
        runTest {
            // Player TEN+KING=20, dealer TEN+SEVEN=17
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.KING),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1100, state.balance) // 900 + 100 * 2
        }

    @Test
    fun stand_push_returnsBet() =
        runTest {
            // Player TEN+NINE=19, dealer TEN+NINE=19
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.NINE),
                        dealerHand = hand(Rank.TEN, Rank.NINE),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PUSH, state.status)
            assertEquals(1000, state.balance) // 900 + 100 returned
        }

    @Test
    fun stand_dealerWin_keepsBalanceUnchanged() =
        runTest {
            // Player TEN+SIX=16, dealer TEN+NINE=19
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.SIX),
                        dealerHand = hand(Rank.TEN, Rank.NINE),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertEquals(900, state.balance) // unchanged, bet lost
        }

    @Test
    fun stand_revealsHoleCardBeforeDealerDraws() =
        runTest {
            // Player TEN+SIX=16, dealer TEN (face-up) + NINE (face-down) = 19
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.SIX),
                        dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            sm.state.value.dealerHand.cards.forEach { card ->
                assertFalse(card.isFaceDown)
            }
        }

    @Test
    fun bust_holeCardIsRevealed() =
        runTest {
            // Player TEN+TEN=20, draws FIVE → 25 bust; hole card should be revealed
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                        deck = deckOf(Rank.FIVE),
                    ),
                )
            sm.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertFalse(state.dealerHand.cards[1].isFaceDown)
        }

    // ── Multi-hand routing ────────────────────────────────────────────────────

    @Test
    fun hit_routesToActiveHand() =
        runTest {
            // activeHandIndex=0 → hit goes to hand[0], hand[1] unchanged
            val initialState =
                multiHandPlayingState(
                    balance = 800,
                    hands = listOf(hand(Rank.EIGHT, Rank.TWO), hand(Rank.EIGHT, Rank.THREE)),
                    bets = listOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.FOUR),
                )
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState)
            sm.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(3, state.playerHands[0].cards.size)
            assertEquals(2, state.playerHands[1].cards.size)
        }

    @Test
    fun hit_routesToSplitHand() =
        runTest {
            // activeHandIndex=1 → hit goes to hand[1], hand[0] unchanged
            val initialState =
                multiHandPlayingState(
                    balance = 800,
                    hands = listOf(hand(Rank.EIGHT, Rank.TWO), hand(Rank.EIGHT, Rank.THREE)),
                    bets = listOf(100, 100),
                    activeHandIndex = 1,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.FOUR),
                )
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState)
            sm.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(2, state.playerHands[0].cards.size)
            assertEquals(3, state.playerHands[1].cards.size)
        }

    @Test
    fun stand_advancesToSplitHand() =
        runTest {
            val initialState =
                multiHandPlayingState(
                    balance = 800,
                    hands = listOf(hand(Rank.EIGHT, Rank.TWO), hand(Rank.EIGHT, Rank.THREE)),
                    bets = listOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                )
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState)
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(1, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun stand_entersDealerTurnAfterLastSplitHand() =
        runTest {
            val initialState =
                multiHandPlayingState(
                    balance = 800,
                    hands = listOf(hand(Rank.EIGHT, Rank.TWO), hand(Rank.EIGHT, Rank.THREE)),
                    bets = listOf(100, 100),
                    activeHandIndex = 1,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                )
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState)
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertTrue(
                state.status == GameStatus.PLAYER_WON ||
                    state.status == GameStatus.DEALER_WON ||
                    state.status == GameStatus.PUSH,
            )
        }

    @Test
    fun stand_advancesToNextMultiHand() =
        runTest {
            val initialState =
                multiHandPlayingState(
                    balance = 800,
                    hands = listOf(hand(Rank.EIGHT, Rank.TWO), hand(Rank.EIGHT, Rank.THREE)),
                    bets = listOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                )
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState)
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(1, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun stand_entersDealerTurnOnLastMultiHand() =
        runTest {
            val initialState =
                multiHandPlayingState(
                    balance = 800,
                    hands = listOf(hand(Rank.EIGHT, Rank.TWO), hand(Rank.EIGHT, Rank.THREE)),
                    bets = listOf(100, 100),
                    activeHandIndex = 1,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                )
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState)
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertTrue(
                state.status == GameStatus.PLAYER_WON ||
                    state.status == GameStatus.DEALER_WON ||
                    state.status == GameStatus.PUSH,
            )
        }

    @Test
    fun hitBust_advancesToNextHand() =
        runTest {
            // Hand0: TEN+FIVE=15, draws TEN → bust; hand1 becomes active
            val initialState =
                multiHandPlayingState(
                    balance = 800,
                    hands = listOf(hand(Rank.TEN, Rank.FIVE), hand(Rank.EIGHT, Rank.THREE)),
                    bets = listOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = hand(Rank.SEVEN, Rank.SEVEN),
                    deck = deckOf(Rank.TEN),
                )
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState)
            sm.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(1, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
        }
}
