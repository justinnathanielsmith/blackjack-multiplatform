package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

// Pure domain logic for initialising a new betting round — extracted from BlackjackStateMachine.
object NewGameLogic {
    /**
     * Computes the initial [GameState] for a new round.
     *
     * This is a pure function: it reads nothing from global state and has no side effects.
     * Bet normalisation and affordability checks that were previously embedded in
     * [BlackjackStateMachine] live here so they can be tested independently.
     *
     * @param balance The effective starting balance for this round (caller resolves nullable initialBalance).
     * @param rules The [GameRules] to apply.
     * @param handCount The number of player hands (seats) to initialise.
     * @param previousBets The last round's per-seat main bets, used for repeat-bet pre-population.
     * @param lastSideBets The last round's side bets, used for repeat-bet pre-population.
     * @return A [GameState] in [GameStatus.BETTING] with bets and balances resolved.
     */
    fun createInitialState(
        balance: Int,
        rules: GameRules = GameRules(),
        handCount: Int = 1,
        previousBets: PersistentList<Int> = persistentListOf(0),
        lastSideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    ): GameState {
        require(handCount in BlackjackConfig.MIN_INITIAL_HANDS..BlackjackConfig.MAX_INITIAL_HANDS) { 
            "handCount must be between ${BlackjackConfig.MIN_INITIAL_HANDS} and ${BlackjackConfig.MAX_INITIAL_HANDS}" 
        }
        // Normalize previousBets length to match handCount.
        // Post-split, previousBets might contain more bets than handCount.
        // We truncate them down to the pre-split seat count.
        val normalizedLastBets =
            if (previousBets.size >= handCount) {
                previousBets.subList(0, handCount).toPersistentList()
            } else {
                previousBets
                    .toMutableList()
                    .apply {
                        repeat(handCount - previousBets.size) { add(0) }
                    }.toPersistentList()
            }

        // Bolt Performance Optimization: Replace .sumOf with indexed loop to avoid Iterator allocation
        var totalLastBet = 0
        for (i in 0 until normalizedLastBets.size) {
            totalLastBet += normalizedLastBets[i]
        }
        val allAffordable = totalLastBet <= balance

        val finalBets: PersistentList<Int>
        val afterMainBetBalance: Int

        if (allAffordable) {
            finalBets = normalizedLastBets
            afterMainBetBalance = balance - totalLastBet
        } else {
            // If they can't afford exactly the previous bets, they start at zero to be safe
            finalBets = List(handCount) { 0 }.toPersistentList()
            afterMainBetBalance = balance
        }

        var totalSideBetCost = 0
        for ((_, betAmount) in lastSideBets) {
            totalSideBetCost += betAmount
        }

        val finalSideBets: PersistentMap<SideBetType, Int>
        val postSideBetBalance: Int

        if (allAffordable && totalSideBetCost <= afterMainBetBalance) {
            finalSideBets = lastSideBets
            postSideBetBalance = afterMainBetBalance - totalSideBetCost
        } else {
            finalSideBets = persistentMapOf()
            postSideBetBalance = afterMainBetBalance
        }

        return GameState(
            status = GameStatus.BETTING,
            balance = postSideBetBalance,
            sideBets = finalSideBets,
            lastSideBets = lastSideBets,
            playerHands =
                List(handCount) { i ->
                    Hand(bet = finalBets[i], lastBet = normalizedLastBets[i])
                }.toPersistentList(),
            activeHandIndex = 0,
            handCount = handCount,
            rules = rules,
        )
    }
}
