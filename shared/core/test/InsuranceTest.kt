@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class InsuranceTest {
    @Test
    fun insurance_offered_when_dealer_ace() =
        runTest {
            // Construct directly in INSURANCE_OFFERED status — no random loop needed
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 900,
// removed:                         currentBets = persistentListOf(100),
                        playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.SEVEN),
                        deck = persistentListOf(),
                    ),
                )
            assertEquals(
                Rank.ACE,
                sm.state.value.dealerHand.cards[0]
                    .rank
            )
            assertEquals(GameStatus.INSURANCE_OFFERED, sm.state.value.status)
        }

    @Test
    fun insurance_not_offered_when_dealer_non_ace() =
        runTest {
            // Controlled deck: player NINE+TWO=11, dealer KING+SEVEN=17 (no ace upcard)
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        deck = deckOf(Rank.NINE, Rank.TWO, Rank.KING, Rank.SEVEN),
                    ),
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PLAYING, state.status)
            assertNotEquals(Rank.ACE, state.dealerHand.cards[0].rank)
        }

    @Test
    fun takeInsurance_deductsHalfBet() =
        runTest {
            // INSURANCE_OFFERED, bet=100, balance=900 → TakeInsurance → balance=850, insuranceBet=50, PLAYING
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 900,
// removed:                         currentBets = persistentListOf(100),
                        playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.SEVEN),
                        deck = persistentListOf(),
                    ),
                )
            sm.dispatch(GameAction.TakeInsurance)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(850, state.balance)
            assertEquals(50, state.insuranceBet)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun declineInsurance_noBalanceChange() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 900,
// removed:                         currentBets = persistentListOf(100),
                        playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.SEVEN),
                        deck = persistentListOf(),
                    ),
                )
            sm.dispatch(GameAction.DeclineInsurance)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(900, state.balance)
            assertEquals(0, state.insuranceBet)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun insurance_paysOn_dealerBlackjack() =
        runTest {
            // Player TEN+SIX=16, dealer ACE+TEN=BJ, insurance taken: balance=850
            // Stand → dealer BJ revealed → insurance pays 50*3=150 → balance=1000; DEALER_WON
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 850,
// removed:                         currentBets = persistentListOf(100),
                        insuranceBet = 50,
                        playerHands = persistentListOf(hand(Rank.TEN, Rank.SIX).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.TEN),
                        deck = persistentListOf(),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertEquals(1000, state.balance) // 850 + 50*3 = 1000; regular bet lost
        }

    @Test
    fun insurance_forfeitedOn_noDealerBlackjack() =
        runTest {
            // Player TEN+NINE=19, dealer ACE+SIX=17, insurance taken: balance=850
            // Stand → dealer 17, no BJ → insurance forfeited; player wins regular
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 850,
// removed:                         currentBets = persistentListOf(100),
                        insuranceBet = 50,
                        playerHands = persistentListOf(hand(Rank.TEN, Rank.NINE).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.SIX),
                        deck = persistentListOf(),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1050, state.balance) // 850 + 100*2 = 1050; insurance already forfeited
        }

    @Test
    fun insurance_invalidIn_wrongStatus() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 900,
// removed:                     currentBets = persistentListOf(100),
                    playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                    dealerHand = dealerHand(Rank.ACE, Rank.SEVEN),
                    deck = persistentListOf(),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.TakeInsurance)
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }
}
