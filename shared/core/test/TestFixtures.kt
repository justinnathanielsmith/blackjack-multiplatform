@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
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
        currentBet = bet,
        playerHands = persistentListOf(playerHand),
        playerBets = persistentListOf(bet),
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
        playerHands = hands.toPersistentList(),
        playerBets = bets.toPersistentList(),
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
