@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

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
    BlackjackStateMachine(
        CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        initialState,
        isTest = true,
    )

fun TestScope.testMachine(): BlackjackStateMachine =
    BlackjackStateMachine(
        CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        isTest = true,
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
    rules: GameRules = GameRules(),
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
    if (status != GameStatus.PLAYING) return GameAction.Stand // Should not happen in fuzzing loop

    val hand = activeHand
    val dealerUpcard = dealerHand.cards[0].rank.value

    // 1. Pairs Strategy
    if (canSplit()) {
        val rank = hand.cards[0].rank
        val key = if (rank == Rank.ACE) "A,A" else "${rank.value},${rank.value}"
        val action =
            StrategyProvider
                .getPairsStrategy()
                .find { it.playerValue == key }
                ?.actions
                ?.get(dealerUpcard)

        if (action == StrategyAction.SPLIT) return GameAction.Split
    }

    // 2. Soft Strategy
    if (hand.isSoft && hand.cards.size == 2) {
        val nonAceRank = if (hand.cards[0].rank == Rank.ACE) hand.cards[1].rank else hand.cards[0].rank
        val key = "A,${nonAceRank.value}"
        val action =
            StrategyProvider
                .getSoftStrategy()
                .find { it.playerValue == key }
                ?.actions
                ?.get(dealerUpcard)

        return when (action) {
            StrategyAction.DOUBLE -> if (canDoubleDown()) GameAction.DoubleDown else GameAction.Stand
            StrategyAction.STAND -> GameAction.Stand
            else -> GameAction.Hit
        }
    }

    // 3. Hard Strategy
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
        StrategyAction.DOUBLE -> if (canDoubleDown()) GameAction.DoubleDown else GameAction.Hit
        StrategyAction.STAND -> GameAction.Stand
        else -> GameAction.Hit
    }
}
