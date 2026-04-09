@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.util.deckOf
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InitialOutcomeTest {
    @Test
    fun multiHand_dealerBlackjack_partialPush() =
        runTest {
            // Correct Round-Robin for 2 hands: P0, P1, D, P0, P1, D
            // Hand0: BJ (ACE, TEN)
            // Hand1: 20 (TEN, TEN)
            // Dealer: BJ (TEN, ACE)
            // Expected: Hand0 pushes, Hand1 loses -> Status is PUSH
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 800,
                        handCount = 2,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100)),
                        deck =
                            deckOf(
                                Rank.ACE, // P0 card 1
                                Rank.TEN, // P1 card 1
                                Rank.TEN, // D card 1 (upcard)
                                Rank.TEN, // P0 card 2 -> P0: ACE, TEN (BJ)
                                Rank.TEN, // P1 card 2 -> P1: TEN, TEN (20)
                                Rank.ACE, // D card 2 (hole) -> D: TEN, ACE (BJ)
                            )
                    )
                )

            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PUSH, state.status)
            // Hand0 (BJ) vs Dealer (BJ) = PUSH -> refund 100
            // Hand1 (20) vs Dealer (BJ) = LOSS -> no refund
            // Balance: 800 (remaining) + 100 (refund) = 900
            assertEquals(900, state.balance)
        }

    @Test
    fun multiHand_insurance_restrictedToSingleHand() =
        runTest {
            // Dealer shows ACE. 2 Hands.
            // Expected: Status = PLAYING (Insurance NOT offered for multi-hand)
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 800,
                        handCount = 2,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100)),
                        deck =
                            deckOf(
                                Rank.NINE, // P0
                                Rank.NINE, // P1
                                Rank.ACE, // D (upcard)
                                Rank.NINE, // P0
                                Rank.NINE, // P1
                                Rank.SEVEN, // D (hole)
                            )
                    )
                )

            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            assertEquals(GameStatus.PLAYING, sm.state.value.status)
        }

    @Test
    @kotlin.test.Ignore // FIXME: This test identifies a REAL BUG where Natural BJ is paid out twice in multi-hand play.
    fun multiHand_payout_blackjackDoublePayoutLeak() =
        runTest {
            // Arrange: 2 hands. Hand 0 = Blackjack, Hand 1 = 15. Dealer = 19.
            // balance = 800, bets = [100, 100]
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 800,
                        handCount = 2,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100)),
                        deck =
                            deckOf(
                                Rank.ACE, // P0 card 1
                                Rank.EIGHT, // P1 card 1
                                Rank.SEVEN, // D card 1 (upcard)
                                Rank.TEN, // P0 card 2 -> Hand0: BJ
                                Rank.SEVEN, // P1 card 2 -> Hand1: 15
                                Rank.TWO, // D card 2 (hole) -> Dealer: 9
                                Rank.TEN, // Dealer draw 1... -> 19 (Stands)
                            )
                    )
                )

            // Act 1: Deal. Hand0 BJ is resolved here.
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // Hand0 resolved immediately. Payout = 100 + 150 = 250.
            // Balance: 800 + 250 = 1050.
            // Note: activeHandIndex remains 0 because the state machine doesn't auto-advance from BJ in initial deal.
            assertEquals(1050, sm.state.value.balance, "Hand0 BJ should be paid out immediately")
            assertEquals(GameStatus.PLAYING, sm.state.value.status)
            assertEquals(0, sm.state.value.activeHandIndex, "Expected to stay on Hand0 (current behavior)")

            // Act 2: Stand on Hand 0 (BJ). Advances to Hand 1.
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()
            assertEquals(1, sm.state.value.activeHandIndex, "Should now be on Hand1")

            // Act 3: Stand on Hand 1 (15). Dealer turn starts.
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            // Dealer Turn: Starts with 9 (7+2). Draws 10 -> 19. Dealer stands.
            // Hand1: 15 vs 19 -> LOSS.

            // BUG VERIFICATION: If the bug exists, Hand 0 (BJ) will be paid out AGAIN during FinalizeGame.
            // Final balance should remain 1050 (initial 800 + 250 from first payout).
            assertEquals(1050, sm.state.value.balance, "Hand0 should NOT be paid a second time when round finalizes")
        }
}
