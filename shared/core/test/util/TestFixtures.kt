@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.util
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.logic.GameRules
import io.github.smithjustinn.blackjack.logic.StrategyAction
import io.github.smithjustinn.blackjack.logic.StrategyProvider
import io.github.smithjustinn.blackjack.middleware.GameFlowConfig
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.Suit
import io.github.smithjustinn.blackjack.state.BlackjackStateMachine
import io.github.smithjustinn.blackjack.state.DefaultBlackjackStateMachine
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

// ── State machine factory ─────────────────────────────────────────────────────

/**
 * Creates a [BlackjackStateMachine] with [isTest]=true, auto-wiring the test scheduler's
 * scope so delays are skipped and the machine is cancelled when the test finishes.
 */
fun TestScope.testMachine(initialState: GameState,): BlackjackStateMachine =
    DefaultBlackjackStateMachine(
        CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        initialState,
        config = GameFlowConfig(
            dealerTurnDelayMs = 0L,
            dealCardDelayMs = 0L,
            dealerCriticalPreDelayMs = 0L,
            revealDelayMs = 0L,
            slowRollDelayMs = 0L
        )
    )

fun TestScope.testMachine(): BlackjackStateMachine =
    DefaultBlackjackStateMachine(
        CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        config = GameFlowConfig(
            dealerTurnDelayMs = 0L,
            dealCardDelayMs = 0L,
            dealerCriticalPreDelayMs = 0L,
            revealDelayMs = 0L,
            slowRollDelayMs = 0L
        )
    )

// ── Card / Hand builders ──────────────────────────────────────────────────────

fun card(
    rank: Rank,
    suit: Suit = Suit.SPADES,
    faceDown: Boolean = false
): Card = Card(rank, suit, faceDown)

fun hand(vararg pairs: Pair<Rank, Boolean>): Hand =
    Hand(pairs.map { (rank, down) -> card(rank, faceDown = down) }.toPersistentList())

fun hand(vararg ranks: Rank): Hand = Hand(ranks.map { card(it) }.toPersistentList())

fun deckOf(vararg ranks: Rank): PersistentList<Card> = ranks.map { card(it) }.toPersistentList()

// ── Common GameState presets ──────────────────────────────────────────────────

/** Standard in-progress playing state: player has 2 known cards vs a known dealer hand. */
fun playingState(
    balance: Int = 900,
    bet: Int = 100,
    playerHand: Hand,
    dealerHand: Hand,
    deck: PersistentList<Card> = persistentListOf(),
    rules: GameRules = GameRules(deterministicReshuffle = true),
): GameState =
    GameState(
        status = GameStatus.PLAYING,
        balance = balance,
// removed:         currentBets = persistentListOf(bet),
        playerHands = persistentListOf(playerHand.copy(bet = bet)),
        dealerHand = dealerHand,
        deck = deck,
        rules = rules,
    )

/** Standard initial betting state. */
fun bettingState(
    balance: Int = 1000,
    handCount: Int = 1,
    rules: GameRules = GameRules(deterministicReshuffle = true),
): GameState =
    GameState(
        status = GameStatus.BETTING,
        balance = balance,
        handCount = handCount,
        playerHands = List(handCount) { Hand() }.toPersistentList(),
        rules = rules,
    )

/** Multi-hand in-progress playing state. */
fun multiHandPlayingState(
    balance: Int,
    hands: List<Hand>,
    bets: List<Int>,
    activeHandIndex: Int = 0,
    dealerHand: Hand,
    deck: PersistentList<Card> = persistentListOf(),
): GameState =
    GameState(
        status = GameStatus.PLAYING,
        balance = balance,
        playerHands = hands.zip(bets) { h, b -> h.copy(bet = b) }.toPersistentList(),
        activeHandIndex = activeHandIndex,
        handCount = hands.size,
        dealerHand = dealerHand,
        deck = deck,
    )

/** Convenience: dealer hand with hole card face-down. */
fun dealerHand(
    upRank: Rank,
    holeRank: Rank
): Hand = Hand(persistentListOf(card(upRank, Suit.CLUBS), card(holeRank, Suit.DIAMONDS, faceDown = true)))

/**
 * A basic strategy bot that decides the next [GameAction] based on [StrategyProvider].
 */
fun GameState.decideAction(): GameAction {
    if (status == GameStatus.INSURANCE_OFFERED) return GameAction.DeclineInsurance
    if (status != GameStatus.PLAYING) return GameAction.Stand

    val hand = activeHand
    val dealerUpcard = dealerHand.cards[0].rank.value

    return decideActionPairs(hand, dealerUpcard)
        ?: decideActionSoft(hand, dealerUpcard)
        ?: decideActionHard(hand, dealerUpcard)
}

private fun GameState.decideActionPairs(
    hand: Hand,
    dealerUpcard: Int
): GameAction? {
    if (!canSplit) return null
    val rank = hand.cards[0].rank
    val key = if (rank == Rank.ACE) "A,A" else "${rank.value},${rank.value}"
    val action =
        StrategyProvider
            .getPairsStrategy()
            .find { it.playerValue == key }
            ?.actions
            ?.get(dealerUpcard)

    return if (action == StrategyAction.SPLIT) GameAction.Split else null
}

private fun GameState.decideActionSoft(
    hand: Hand,
    dealerUpcard: Int
): GameAction? {
    if (!hand.isSoft || hand.cards.size != 2) return null
    val nonAceRank = if (hand.cards[0].rank == Rank.ACE) hand.cards[1].rank else hand.cards[0].rank
    val key = "A,${nonAceRank.value}"
    val action =
        StrategyProvider
            .getSoftStrategy()
            .find { it.playerValue == key }
            ?.actions
            ?.get(dealerUpcard)

    return when (action) {
        StrategyAction.DOUBLE -> if (canDoubleDown) GameAction.DoubleDown else GameAction.Stand
        StrategyAction.STAND -> GameAction.Stand
        StrategyAction.HIT -> GameAction.Hit
        else -> null
    }
}

private fun GameState.decideActionHard(
    hand: Hand,
    dealerUpcard: Int
): GameAction {
    val score = hand.score
    val key =
        when {
            score >= 17 -> "17+"
            score <= 8 -> "8 or less"
            else -> score.toString()
        }
    val action =
        StrategyProvider
            .getHardStrategy()
            .find { it.playerValue == key }
            ?.actions
            ?.get(dealerUpcard)

    return when (action) {
        StrategyAction.DOUBLE -> if (canDoubleDown) GameAction.DoubleDown else GameAction.Hit
        StrategyAction.STAND -> GameAction.Stand
        else -> GameAction.Hit
    }
}
