package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.BlackjackConfig
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
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
         * changes or side effects. Useful for early exits where no feedback is needed.
         */
        fun noop(state: GameState): PlayerActionOutcome = PlayerActionOutcome(state)

        /**
         * Creates a [PlayerActionOutcome] that returns the provided [state] with a
         * [GameEffect.Vibrate] effect. Used for actions that are rejected due to
         * status guards or rule violations.
         */
        fun rejected(state: GameState): PlayerActionOutcome =
            PlayerActionOutcome(state, effects = listOf(GameEffect.Vibrate))
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
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.rejected(state)

        // Block hits on 21 or higher.
        if (state.activeHand.score >= BlackjackConfig.BLACKJACK_SCORE) {
            return PlayerActionOutcome.rejected(state)
        }

        // Block hits on split aces.
        if (state.activeHand.isFromSplitAce && state.activeHand.cards.size >= 2) {
            return PlayerActionOutcome.rejected(state)
        }

        val newCard = state.deck.firstOrNull() ?: return PlayerActionOutcome.noop(state)
        val remainingDeck = state.deck.drop(1).toPersistentList()
        val newHand = state.activeHand.copy(cards = state.activeHand.cards.add(newCard))
        val updatedHands = state.playerHands.set(state.activeHandIndex, newHand)
        val updatedState = state.copy(deck = remainingDeck, playerHands = updatedHands)

        val effects = getEffectsForHit(newCard, newHand, state.activeHandIndex)
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
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.rejected(state)
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
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.rejected(state)
        val hand = state.activeHand
        val canLogicDouble = hand.cards.size == 2 && (!hand.wasSplit || state.rules.allowDoubleAfterSplit)
        if (canLogicDouble && state.balance < hand.bet) {
            return PlayerActionOutcome.rejected(state)
        }
        if (!canLogicDouble) return PlayerActionOutcome.rejected(state)

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

        val effects = getEffectsForDoubleDown(drawnCard, newHand)

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
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.rejected(state)
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
            return PlayerActionOutcome.rejected(state)
        }
        if (!canLogicSplit) return PlayerActionOutcome.rejected(state)
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

    /**
     * Surrenders the active hand, refunding half the bet and ending the player's turn.
     *
     * Blocked if:
     * - The game is not in [GameStatus.PLAYING].
     * - The active hand does not have exactly 2 cards.
     * - The house rules do not permit surrender.
     *
     * Emits [GameEffect.PlayLoseSound] and [GameEffect.ChipLoss] with the forfeited amount.
     *
     * @param state The current [GameState].
     * @return A [PlayerActionOutcome] with the refunded balance and turn advanced.
     */
    fun surrender(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.PLAYING ||
            state.activeHand.cards.size != 2 ||
            !state.rules.allowSurrender
        ) {
            return PlayerActionOutcome.rejected(state)
        }
        val refund = state.activeBet / 2
        val surrenderedHand = state.activeHand.copy(isSurrendered = true)
        val updatedHands = state.playerHands.set(state.activeHandIndex, surrenderedHand)
        val newState = state.copy(balance = state.balance + refund, playerHands = updatedHands)
        val effects = listOf(GameEffect.PlayLoseSound, GameEffect.ChipLoss(state.activeBet - refund))
        return PlayerActionOutcome(state = newState, effects = effects, shouldAdvanceTurn = true)
    }

    /**
     * Places an insurance bet equal to half the main bet when the dealer shows an Ace.
     *
     * Blocked if:
     * - The game is not in [GameStatus.INSURANCE_OFFERED].
     * - The player lacks sufficient balance to cover the insurance bet.
     *
     * After placing the bet, delegates to [resolveInsuranceOutcome] to check for dealer Blackjack.
     *
     * @param state The current [GameState].
     * @return A [PlayerActionOutcome] carrying the updated balance and resolved status.
     */
    fun takeInsurance(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.INSURANCE_OFFERED) return PlayerActionOutcome.rejected(state)
        val insuranceBet = state.currentBet / 2
        if (insuranceBet > state.balance) return PlayerActionOutcome.rejected(state)
        val newState =
            state.copy(
                balance = state.balance - insuranceBet,
                insuranceBet = insuranceBet,
            )
        return resolveInsuranceOutcome(newState)
    }

    /**
     * Declines the insurance offer, clearing any pending insurance wager.
     *
     * Blocked if the game is not in [GameStatus.INSURANCE_OFFERED].
     *
     * Delegates to [resolveInsuranceOutcome] to proceed with the round.
     *
     * @param state The current [GameState].
     * @return A [PlayerActionOutcome] with the resolved next status.
     */
    fun declineInsurance(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.INSURANCE_OFFERED) return PlayerActionOutcome.rejected(state)
        val newState = state.copy(insuranceBet = 0)
        return resolveInsuranceOutcome(newState)
    }

    /**
     * Shared resolution path for both [takeInsurance] and [declineInsurance].
     *
     * If the dealer holds Blackjack, transitions to [GameStatus.DEALER_TURN] so the middleware
     * can execute the reveal sequence. Otherwise, transitions to [GameStatus.PLAYING].
     *
     * The [PlayerActionOutcome.shouldAdvanceTurn] is `false` here — the caller ([GameReducer])
     * is responsible for emitting [ReducerCommand.RunDealerTurn] when needed.
     *
     * @param state The [GameState] after the insurance bet has been resolved.
     * @return A [PlayerActionOutcome] with the correct next status.
     */
    fun resolveInsuranceOutcome(state: GameState): PlayerActionOutcome =
        if (state.dealerHand.score == BlackjackRules.BLACKJACK_SCORE) {
            PlayerActionOutcome(state = state.copy(status = GameStatus.DEALER_TURN))
        } else {
            PlayerActionOutcome(state = state.copy(status = GameStatus.PLAYING))
        }

    private fun getEffectsForHit(
        newCard: io.github.smithjustinn.blackjack.model.Card,
        newHand: Hand,
        activeHandIndex: Int
    ): List<GameEffect> =
        buildList {
            add(GameEffect.PlayCardSound)
            if (newCard.rank.value < HIGH_CARD_VALUE) add(GameEffect.LightTick)
            if (newCard.rank.value >= HIGH_CARD_VALUE) add(GameEffect.HeavyCardThud)
            if (newHand.score == BlackjackConfig.BLACKJACK_SCORE) add(GameEffect.Pulse21)
            if (newHand.score == NEAR_MISS_SCORE) add(GameEffect.NearMissHighlight(activeHandIndex))
            if (newHand.isBust) add(GameEffect.BustThud)
        }

    private fun getEffectsForDoubleDown(
        drawnCard: io.github.smithjustinn.blackjack.model.Card,
        newHand: Hand
    ): List<GameEffect> =
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
}
