package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

// ── Card / Hand builders ──────────────────────────────────────────────────────

fun card(rank: Rank, suit: Suit = Suit.SPADES, faceDown: Boolean = false): Card =
    Card(rank, suit, faceDown)

fun hand(vararg pairs: Pair<Rank, Boolean>): Hand =
    Hand(persistentListOf(*pairs.map { (rank, down) -> card(rank, faceDown = down) }.toTypedArray()))

fun hand(vararg ranks: Rank): Hand =
    Hand(persistentListOf(*ranks.map { card(it) }.toTypedArray()))

fun deckOf(vararg ranks: Rank): PersistentList<Card> =
    persistentListOf(*ranks.map { card(it) }.toTypedArray())

// ── Common GameState presets ──────────────────────────────────────────────────

/** Standard in-progress playing state: player has 2 known cards vs a known dealer hand. */
fun playingState(
    balance: Int = 900,
    bet: Int = 100,
    playerHand: Hand,
    dealerHand: Hand,
    deck: PersistentList<Card> = persistentListOf(),
    rules: GameRules = GameRules(),
): GameState = GameState(
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
): GameState = GameState(
    status = GameStatus.PLAYING,
    balance = balance,
    playerHands = persistentListOf(*hands.toTypedArray()),
    playerBets = persistentListOf(*bets.toIntArray().toTypedArray()),
    activeHandIndex = activeHandIndex,
    handCount = hands.size,
    dealerHand = dealerHand,
    deck = deck,
)

/** Convenience: dealer hand with hole card face-down. */
fun dealerHand(upRank: Rank, holeRank: Rank): Hand =
    Hand(persistentListOf(card(upRank, Suit.CLUBS), card(holeRank, Suit.DIAMONDS, faceDown = true)))
