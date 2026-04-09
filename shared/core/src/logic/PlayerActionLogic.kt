package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Represents the result of a player action processed by [PlayerActionLogic].
 *
 * @property state The next [GameState] produced by the action, reflecting the next transition
 *           in the game's reactive pipeline.
 * @property effects Auditory and visual [GameEffect]s to be emitted by the UI (e.g., flipping cards, thuds).
 * @property shouldAdvanceTurn True if the action ends the player's current turn for the
 *           [GameState.activeHand], signaling the state machine to move to the next hand or dealer.
 */
data class PlayerActionOutcome(
    val state: GameState,
    val effects: List<GameEffect> = emptyList(),
    val shouldAdvanceTurn: Boolean = false
) {
    companion object {
        /**
         * Creates a [PlayerActionOutcome] that returns the provided [state] without any
         * changes or side effects. Useful for blocked actions or early exits.
         *
         * @param state The current or default state to return.
         * @return A "no-op" outcome.
         */
        fun noop(state: GameState): PlayerActionOutcome = PlayerActionOutcome(state)
    }
}

/**
 * The core engine for player decision logic in Blackjack.
 *
 * This implementation handles standard and advanced moves (Hit, Stand, Double Down, Split) and
 * ensures they comply with house rules and state invariants. It encapsulates:
 * 1. **Deck Management**: Ensuring cards are drawn correctly and consistently from the shoe.
 * 2. **Bet Orchestration**: Deducting balances for additional wagers (Split, Double Down).
 * 3. **Juice & Feedback**: Selection and emission of relevant [GameEffect]s for a premium feel.
 *
 * All functions are pure state transitions that return a [PlayerActionOutcome].
 */
object PlayerActionLogic {
    // Score threshold sourced from BlackjackConfig — single authoritative constant for the whole domain.
    private const val HIGH_CARD_VALUE = 10
    private const val NEAR_MISS_SCORE = 11
    private const val CARDS_TO_DEAL_ON_SPLIT = 2

    /**
     * Deals an extra card to the active hand if permissible.
     *
     * Hitting is blocked if:
     * - The game is not in [GameStatus.PLAYING].
     * - The active hand's score is already 21 or more.
     * - The hand was created from split Aces (standard Blackjack rule).
     *
     * Emits relevant [GameEffect]s based on the drawn card value (thuds, ticks, 21-pulse, near-miss).
     * If the hand busts, [PlayerActionOutcome.shouldAdvanceTurn] is set to true.
     *
     * @param state The current [GameState] determining the active hand and deck shoe.
     * @return The resulting state and effects.
     */
    fun hit(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.noop(state)

        // Block hits on 21 or higher.
        if (state.activeHand.score >= BlackjackConfig.BLACKJACK_SCORE) {
            return PlayerActionOutcome.noop(state)
        }

        // Block hits on split aces.
        if (state.activeHand.isFromSplitAce && state.activeHand.cards.size >= 2) {
            return PlayerActionOutcome.noop(state)
        }

        val newCard = state.deck.firstOrNull() ?: return PlayerActionOutcome.noop(state)
        val remainingDeck = state.deck.drop(1).toPersistentList()
        val newHand = state.activeHand.copy(cards = state.activeHand.cards.add(newCard))
        val updatedHands = state.playerHands.set(state.activeHandIndex, newHand)
        val updatedState = state.copy(deck = remainingDeck, playerHands = updatedHands)

        val effects =
            buildList {
                add(GameEffect.PlayCardSound)
                if (newCard.rank.value < HIGH_CARD_VALUE) add(GameEffect.LightTick)
                if (newCard.rank.value >= HIGH_CARD_VALUE) add(GameEffect.HeavyCardThud)
                if (newHand.score == BlackjackConfig.BLACKJACK_SCORE) add(GameEffect.Pulse21)
                if (newHand.score == NEAR_MISS_SCORE) add(GameEffect.NearMissHighlight(state.activeHandIndex))
                if (newHand.isBust) add(GameEffect.BustThud)
            }
        return PlayerActionOutcome(
            state = updatedState,
            effects = effects,
            shouldAdvanceTurn = newHand.isBust
        )
    }

    /**
     * Finalizes the player's turn for the active hand without drawing further cards.
     *
     * Ends the player's interaction for the current hand and marks it as [Hand.isStanding].
     * Always sets [PlayerActionOutcome.shouldAdvanceTurn] to true.
     *
     * @param state The current [GameState] to transition from.
     * @return A state where the active hand is standing.
     */
    fun stand(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.noop(state)
        val standingHand = state.activeHand.copy(isStanding = true)
        val updatedHands = state.playerHands.set(state.activeHandIndex, standingHand)
        return PlayerActionOutcome(state = state.copy(playerHands = updatedHands), shouldAdvanceTurn = true)
    }

    /**
     * Doubles the current bet for the active hand and draws exactly one additional card.
     *
     * Valid when:
     * - The hand has exactly 2 cards.
     * - After split, if the rules allow doubling-after-split.
     * - The player balance is sufficient to match the total existing bet.
     *
     * Deducts the extra bet amount from the [GameState.balance] and permanently ends the
     * hand's turn. Emits [GameEffect.HeavyCardThud] for high-value cards and [GameEffect.BustThud] if
     * the player busts.
     *
     * @param state The current [GameState].
     * @return The next state with the doubled wager and final drawn card.
     */
    fun doubleDown(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.noop(state)
        val hand = state.activeHand
        val canLogicDouble = hand.cards.size == 2 && (!hand.wasSplit || state.rules.allowDoubleAfterSplit)
        if (canLogicDouble && state.balance < hand.bet) {
            return PlayerActionOutcome(state, listOf(GameEffect.Vibrate))
        }
        if (!canLogicDouble) return PlayerActionOutcome.noop(state)

        val drawnCard = state.deck.firstOrNull() ?: return PlayerActionOutcome.noop(state)
        val remainingDeck = state.deck.drop(1).toPersistentList()
        val originalBet = state.activeHand.bet
        val newHand =
            state.activeHand.copy(
                cards = state.activeHand.cards.add(drawnCard),
                bet = originalBet * 2,
                isDoubleDown = true
            )
        val updatedHands = state.playerHands.set(state.activeHandIndex, newHand)

        // Double the bet for this hand; deduct the extra (original bet) from balance.
        val newBalance = state.balance - originalBet

        val updatedState =
            state.copy(
                deck = remainingDeck,
                playerHands = updatedHands,
                balance = newBalance,
            )

        val effects =
            buildList {
                add(GameEffect.PlayCardSound)
                if (drawnCard.rank.value >= HIGH_CARD_VALUE) add(GameEffect.HeavyCardThud)
                if (newHand.score == BlackjackConfig.BLACKJACK_SCORE) add(GameEffect.Pulse21)
                if (newHand.isBust) {
                    add(GameEffect.PlayLoseSound)
                    add(GameEffect.BustThud)
                    add(GameEffect.ChipLoss(newHand.bet))
                }
            }

        return PlayerActionOutcome(
            state = updatedState,
            effects = effects,
            shouldAdvanceTurn = true
        )
    }

    /**
     * Splits a paired hand into two separate hands, each receiving a new card and a matching bet.
     *
     * Splitting is valid only when:
     * - The active hand has exactly 2 cards of equal rank (or value, if rules allow).
     * - The maximum number of hands ([BlackjackConfig.MAX_HANDS]) has not been reached.
     * - The player's balance can cover the identical wager for the second hand.
     *
     * Special handling for Ace splitting (only one card deal, automatically ends turn).
     * Pushes the new hand into the player's list and deducts the stake from the balance.
     *
     * @param state The current [GameState].
     * @return The state updated with the split hands and two dealt card effects.
     */
    fun split(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.noop(state)
        val hand = state.activeHand
        val c0 = hand.cards.getOrNull(0)?.rank
        val c1 = hand.cards.getOrNull(1)?.rank
        val rankMatch =
            if (state.rules.splitOnValueOnly) {
                c0?.value == c1?.value
            } else {
                c0 == c1
            }
        val canLogicSplit =
            state.playerHands.size < BlackjackConfig.MAX_HANDS &&
                hand.cards.size == 2 &&
                rankMatch == true

        if (canLogicSplit && state.balance < hand.bet) {
            return PlayerActionOutcome(state, listOf(GameEffect.Vibrate))
        }
        if (!canLogicSplit) return PlayerActionOutcome.noop(state)
        if (state.deck.size < CARDS_TO_DEAL_ON_SPLIT) return PlayerActionOutcome.noop(state)

        val card1 = state.activeHand.cards[0]
        val card2 = state.activeHand.cards[1]

        // Use sequential draws to ensure deck consistency
        val cardFromDeck1 = state.deck[0]
        val cardFromDeck2 = state.deck[1]

        val splitBet = state.activeHand.bet
        val newPrimaryHand = Hand(persistentListOf(card1, cardFromDeck1), bet = splitBet)
        val newSplitHand = Hand(persistentListOf(card2, cardFromDeck2), bet = splitBet)
        val isAceSplit = card1.rank == Rank.ACE

        val updatedHands =
            state.playerHands
                .set(
                    state.activeHandIndex,
                    newPrimaryHand.copy(wasSplit = true, isFromSplitAce = isAceSplit)
                ).add(
                    state.activeHandIndex + 1,
                    newSplitHand.copy(wasSplit = true, isFromSplitAce = isAceSplit)
                )

        val updatedState =
            state.copy(
                deck = state.deck.drop(CARDS_TO_DEAL_ON_SPLIT).toPersistentList(),
                playerHands = updatedHands,
                balance = state.balance - splitBet,
            )

        val finalState =
            if (isAceSplit) {
                // Skip both new hands for Ace split turn progression.
                // One increment here, and shouldAdvanceTurn=true triggers another in the state machine.
                updatedState.copy(activeHandIndex = updatedState.activeHandIndex + 1)
            } else {
                updatedState
            }

        return PlayerActionOutcome(
            state = finalState,
            effects = listOf(GameEffect.PlayCardSound, GameEffect.PlayCardSound),
            shouldAdvanceTurn = isAceSplit
        )
    }
}
