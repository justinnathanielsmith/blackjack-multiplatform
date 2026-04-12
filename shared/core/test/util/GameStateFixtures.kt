@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.util

import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi

// ── Betting phase ─────────────────────────────────────────────────────────────

/** Fresh betting state: 1 seat, full balance, no bet placed. */
fun defaultBettingState(): GameState = bettingState()

/**
 * Betting state with a pre-placed [bet] on seat 0, ready to deal.
 * Balance is [balance] (already reflects the deducted bet).
 */
fun bettingReadyToDeal(
    bet: Int = 100,
    balance: Int = 900,
): GameState =
    bettingState(balance = balance).copy(
        playerHands = persistentListOf(Hand(bet = bet)),
    )

/**
 * Creates `count` number of empty hands, each equipped with `bet`.
 */
fun listOfEmptyHands(
    count: Int,
    bet: Int = 100
): kotlinx.collections.immutable.PersistentList<Hand> {
    val list = mutableListOf<Hand>()
    repeat(count) { list.add(Hand(bet = bet)) }
    return list.toPersistentList()
}

/**
 * Sets up a betting phase state with multiple active seats configured.
 */
fun multiHandBettingState(
    seats: Int,
    bet: Int = 100,
    balance: Int = 800,
): GameState =
    bettingState(balance = balance, handCount = seats).copy(
        playerHands = listOfEmptyHands(seats, bet),
    )

// ── Playing phase — hard hands ────────────────────────────────────────────────

/**
 * Safe hard-14 playing state (EIGHT+SIX vs dealer 7-up).
 * Player won't bust on a single hit — good general-purpose fixture.
 */
fun defaultPlayingState(
    bet: Int = 100,
    balance: Int = 900,
): GameState =
    playingState(
        bet = bet,
        balance = balance,
        playerHand = hand(Rank.EIGHT, Rank.SIX),
        dealerHand = dealerHand(Rank.SEVEN, Rank.TEN),
    )

/**
 * Hard-16 vs dealer TEN — player one card from bust.
 * Place a TEN in the deck to force bust on hit.
 */
fun nearBustPlayingState(
    bet: Int = 100,
    balance: Int = 900,
): GameState =
    playingState(
        bet = bet,
        balance = balance,
        playerHand = hand(Rank.TEN, Rank.SIX),
        dealerHand = dealerHand(Rank.TEN, Rank.NINE),
        deck = deckOf(Rank.TEN),
    )

/**
 * Hard-11 vs dealer 6 — canonical double-down setup (player FIVE+SIX).
 * Place any ten-value card in the deck to complete the double.
 */
fun doubleDownReadyState(
    bet: Int = 100,
    balance: Int = 900,
): GameState =
    playingState(
        bet = bet,
        balance = balance,
        playerHand = hand(Rank.FIVE, Rank.SIX),
        dealerHand = dealerHand(Rank.SIX, Rank.TEN),
        deck = deckOf(Rank.TEN),
    )

// ── Playing phase — soft hands ────────────────────────────────────────────────

/**
 * Soft-17 playing state (ACE+SIX vs dealer 7-up).
 * Classic soft-hand scenario for strategy and logic tests.
 */
fun softHandPlayingState(
    bet: Int = 100,
    balance: Int = 900,
): GameState =
    playingState(
        bet = bet,
        balance = balance,
        playerHand = hand(Rank.ACE, Rank.SIX),
        dealerHand = dealerHand(Rank.SEVEN, Rank.TEN),
    )

// ── Playing phase — pairs / split ─────────────────────────────────────────────

/**
 * Two Aces in the player's hand — canonical split-aces scenario.
 * [balance] reflects both seats already funded (2 × 100 bet deducted by default).
 */
fun splitAcesState(balance: Int = 800,): GameState =
    playingState(
        bet = 100,
        balance = balance,
        playerHand = hand(Rank.ACE, Rank.ACE),
        dealerHand = dealerHand(Rank.SEVEN, Rank.TEN),
    )

/**
 * Two Eights in the player's hand — split-eights scenario.
 */
fun splitEightsState(balance: Int = 800,): GameState =
    playingState(
        bet = 100,
        balance = balance,
        playerHand = hand(Rank.EIGHT, Rank.EIGHT),
        dealerHand = dealerHand(Rank.SEVEN, Rank.TEN),
    )

// ── Insurance ─────────────────────────────────────────────────────────────────

/**
 * Dealer shows an Ace with hole card face-down, status = INSURANCE_OFFERED.
 * Player has a neutral hard-14.
 */
fun insuranceOfferedState(
    balance: Int = 900,
    bet: Int = 100,
): GameState =
    playingState(
        bet = bet,
        balance = balance,
        playerHand = hand(Rank.EIGHT, Rank.SIX),
        dealerHand = dealerHand(Rank.ACE, Rank.TEN),
    ).copy(status = GameStatus.INSURANCE_OFFERED)

// ── Terminal / outcome snapshots ──────────────────────────────────────────────

/**
 * Player holds blackjack (ACE+KING, score = 21) vs dealer 7-up.
 * Status = PLAYER_WON — useful for outcome-display and payout tests.
 */
fun playerBlackjackState(balance: Int = 900,): GameState =
    playingState(
        bet = 100,
        balance = balance,
        playerHand = hand(Rank.ACE, Rank.KING),
        dealerHand = dealerHand(Rank.SEVEN, Rank.TEN),
    ).copy(status = GameStatus.PLAYER_WON)

/**
 * Player's hand has busted (TEN+EIGHT+SEVEN = 25), status = DEALER_WON.
 * Useful for tests that verify bust-state display or guard-clause rejection.
 */
fun playerBustState(
    bet: Int = 100,
    balance: Int = 900,
): GameState =
    playingState(
        bet = bet,
        balance = balance,
        playerHand = hand(Rank.TEN, Rank.EIGHT, Rank.SEVEN),
        dealerHand = dealerHand(Rank.SEVEN, Rank.TEN),
    ).copy(status = GameStatus.DEALER_WON)

/**
 * Dealer has busted (TEN+SIX+NINE = 25), status = PLAYER_WON.
 * Useful for tests that verify dealer-bust payout paths.
 */
fun dealerBustState(
    bet: Int = 100,
    balance: Int = 900,
): GameState =
    playingState(
        bet = bet,
        balance = balance,
        playerHand = hand(Rank.TEN, Rank.EIGHT),
        dealerHand = hand(Rank.TEN, Rank.SIX, Rank.NINE),
    ).copy(status = GameStatus.PLAYER_WON)
