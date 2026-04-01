@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import persistentListOf
import persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NewGameLogicTest {
    @Test
    fun shouldPadPreviousBetsWhenHandCountIncreases() =
        runTest {
            val balance = 1000
            val handCount = 3
            val previousBets = persistentListOf(100)

            val state =
                NewGameLogic.createInitialState(
                    balance = balance,
                    handCount = handCount,
                    previousBets = previousBets
                )

            assertEquals(3, state.playerHands.size)
            assertEquals(100, state.playerHands[0].bet)
            assertEquals(0, state.playerHands[1].bet)
            assertEquals(0, state.playerHands[2].bet)
            assertEquals(900, state.balance)
            assertEquals(100, state.playerHands[0].lastBet)
            assertEquals(0, state.playerHands[1].lastBet)
            assertEquals(0, state.playerHands[2].lastBet)
        }

    @Test
    fun shouldTruncatePreviousBetsWhenHandCountDecreases() =
        runTest {
            val balance = 1000
            val handCount = 1
            val previousBets = persistentListOf(100, 200, 300)

            val state =
                NewGameLogic.createInitialState(
                    balance = balance,
                    handCount = handCount,
                    previousBets = previousBets
                )

            assertEquals(1, state.playerHands.size)
            assertEquals(100, state.playerHands[0].bet)
            assertEquals(900, state.balance)
            assertEquals(100, state.playerHands[0].lastBet)
        }

    @Test
    fun shouldKeepPreviousBetsWhenHandCountMatches() =
        runTest {
            val balance = 1000
            val handCount = 2
            val previousBets = persistentListOf(100, 200)

            val state =
                NewGameLogic.createInitialState(
                    balance = balance,
                    handCount = handCount,
                    previousBets = previousBets
                )

            assertEquals(2, state.playerHands.size)
            assertEquals(100, state.playerHands[0].bet)
            assertEquals(200, state.playerHands[1].bet)
            assertEquals(700, state.balance)
        }

    @Test
    fun shouldApprovePreviousBetsIfExactlyAffordable() =
        runTest {
            val balance = 300
            val handCount = 3
            val previousBets = persistentListOf(100, 100, 100)

            val state =
                NewGameLogic.createInitialState(
                    balance = balance,
                    handCount = handCount,
                    previousBets = previousBets
                )

            assertEquals(300, state.playerHands.sumOf { it.bet })
            assertEquals(0, state.balance)
        }

    @Test
    fun shouldRejectPreviousBetsIfUnaffordable() =
        runTest {
            val balance = 200
            val handCount = 3
            val previousBets = persistentListOf(100, 100, 100)

            val state =
                NewGameLogic.createInitialState(
                    balance = balance,
                    handCount = handCount,
                    previousBets = previousBets
                )

            assertEquals(0, state.playerHands.sumOf { it.bet })
            assertEquals(200, state.balance)
            // lastBet should still be the normalized previous bets for UI cues
            assertEquals(100, state.playerHands[0].lastBet)
            assertEquals(100, state.playerHands[1].lastBet)
            assertEquals(100, state.playerHands[2].lastBet)
        }

    @Test
    fun shouldApproveSideBetsIfAffordableAfterMainBets() =
        runTest {
            val balance = 500
            val handCount = 1
            val previousBets = persistentListOf(100)
            val lastSideBets =
                persistentMapOf(
                    SideBetType.PERFECT_PAIRS to 50,
                    SideBetType.TWENTY_ONE_PLUS_THREE to 50
                )

            val state =
                NewGameLogic.createInitialState(
                    balance = balance,
                    handCount = handCount,
                    previousBets = previousBets,
                    lastSideBets = lastSideBets
                )

            assertEquals(100, state.playerHands[0].bet)
            assertEquals(2, state.sideBets.size)
            assertEquals(50, state.sideBets[SideBetType.PERFECT_PAIRS])
            assertEquals(50, state.sideBets[SideBetType.TWENTY_ONE_PLUS_THREE])
            assertEquals(300, state.balance) // 500 - 100 - 50 - 50 = 300
        }

    @Test
    fun shouldRejectSideBetsIfUnaffordableAfterMainBets() =
        runTest {
            val balance = 150
            val handCount = 1
            val previousBets = persistentListOf(100)
            val lastSideBets =
                persistentMapOf(
                    SideBetType.PERFECT_PAIRS to 100
                )

            val state =
                NewGameLogic.createInitialState(
                    balance = balance,
                    handCount = handCount,
                    previousBets = previousBets,
                    lastSideBets = lastSideBets
                )

            assertEquals(100, state.playerHands[0].bet)
            assertTrue(state.sideBets.isEmpty())
            assertEquals(50, state.balance) // 150 - 100 = 50
            assertEquals(lastSideBets, state.lastSideBets) // lastSideBets persists for UI
        }

    @Test
    fun shouldRejectBothIfTotalMainBetsUnaffordable() =
        runTest {
            val balance = 50
            val handCount = 1
            val previousBets = persistentListOf(100)
            val lastSideBets =
                persistentMapOf(
                    SideBetType.PERFECT_PAIRS to 50
                )

            val state =
                NewGameLogic.createInitialState(
                    balance = balance,
                    handCount = handCount,
                    previousBets = previousBets,
                    lastSideBets = lastSideBets
                )

            assertEquals(0, state.playerHands[0].bet)
            assertTrue(state.sideBets.isEmpty())
            assertEquals(50, state.balance)
        }

    @Test
    fun shouldInitializeWithCorrectStatusAndRules() =
        runTest {
            val rules = GameRules(dealerHitsSoft17 = false)
            val state =
                NewGameLogic.createInitialState(
                    balance = 1000,
                    rules = rules
                )

            assertEquals(GameStatus.BETTING, state.status)
            assertEquals(rules, state.rules)
            assertEquals(0, state.activeHandIndex)
        }
}
