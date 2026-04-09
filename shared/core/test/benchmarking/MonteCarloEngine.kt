package io.github.smithjustinn.blackjack.benchmarking
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.logic.BlackjackRules
import io.github.smithjustinn.blackjack.logic.GameRules
import io.github.smithjustinn.blackjack.logic.NewGameLogic
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.state.ReducerCommand
import io.github.smithjustinn.blackjack.state.reduce
import io.github.smithjustinn.blackjack.util.decideAction
import kotlinx.collections.immutable.toPersistentList
import kotlin.random.Random

/**
 * A headless, synchronous engine that drives Blackjack games directly via [reduce].
 * Bypasses the coroutine/delay layer for fast, deterministic fuzzing and EV analysis.
 */
internal class MonteCarloEngine(
    val random: Random,
    val rules: GameRules = GameRules(),
    initialBalance: Int = 100000
) {
    var state: GameState = GameState(balance = initialBalance, rules = rules, status = GameStatus.BETTING)
    var cumulativeDealerProfit: Int = 0
    val history = mutableListOf<GameAction>()
    val initialBalanceStored = initialBalance

    private fun step(action: GameAction) {
        history.add(action)
        val result = reduce(state, action)
        state = result.state

        // Fulfill commands synchronously
        result.commands.forEach { cmd ->
            when (cmd) {
                is ReducerCommand.RunDealSequence -> executeRunDealSequence()
                is ReducerCommand.RunDealerTurn -> executeRunDealerTurn()
            }
        }
    }

    private fun executeRunDealSequence() {
        // Reshuffle if strictly empty in test mode (matching BlackjackStateMachine.getDeck logic)
        if (state.deck.isEmpty()) {
            val deck = BlackjackRules.createDeck(state.rules, random).toPersistentList()
            state = reduce(state, GameAction.SetDeck(deck)).state
        }

        // Deal 2 rounds
        for (round in 0..1) {
            for (i in 0 until state.handCount) {
                state = reduce(state, GameAction.DealCardToPlayer(i)).state
            }
            state = reduce(state, GameAction.DealCardToDealer(faceDown = round == 1)).state
        }

        // Apply initial outcome
        state = reduce(state, GameAction.ApplyInitialOutcome).state
    }

    private fun executeRunDealerTurn() {
        // Reveal
        state = reduce(state, GameAction.RevealDealerHole).state

        // Draw loop
        while (BlackjackRules.shouldDealerDraw(state.dealerHand, state.rules)) {
            if (state.deck.isEmpty()) break
            state = reduce(state, GameAction.DealerDraw).state
        }

        // Finalize
        state = reduce(state, GameAction.FinalizeGame).state
    }

    /**
     * Runs a single full round of Blackjack.
     * @param bet The bet amount per hand.
     * @param handCount The initial number of hands.
     * @param sideBets Optional side bets to place.
     */
    fun playRound(
        bet: Int,
        handCount: Int = 1,
        sideBets: Map<SideBetType, Int> = emptyMap()
    ): GameState {
        history.clear()

        // 1. Initial Setup (Use NewGameLogic to ensure fresh start with empty hands)
        state =
            NewGameLogic.createInitialState(
                balance = state.balance,
                rules = state.rules,
                handCount = handCount
            )

        // 2. Configure round
        sideBets.forEach { (type, amount) ->
            step(GameAction.PlaceSideBet(type, amount))
        }
        for (i in 0 until handCount) {
            step(GameAction.PlaceBet(bet, i))
        }

        // 3. Deal
        step(GameAction.Deal)

        // 4. Player Turn
        while (state.status == GameStatus.PLAYING || state.status == GameStatus.INSURANCE_OFFERED) {
            val action = state.decideAction()
            step(action)
        }

        // 5. Update profit tracking after terminal state
        // Dealer profit = (Total Bet) - (Total Payout to Balance change)
        // Wait, easier calculation: dealer profit = InitialTotalChipset - currentBalance - currentWagersOnTable
        // Since we are terminal, currentWagersOnTable == 0.
        // We track it per round.
        return state
    }
}
