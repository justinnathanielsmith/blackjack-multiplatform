package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

data class PlayerActionOutcome(
    val state: GameState,
    val effects: List<GameEffect> = emptyList(),
    val shouldAdvanceTurn: Boolean = false
) {
    companion object {
        fun noop(state: GameState): PlayerActionOutcome = PlayerActionOutcome(state)
    }
}

object PlayerActionLogic {
    private const val BLACKJACK_SCORE = 21
    private const val HIGH_CARD_VALUE = 10
    private const val NEAR_MISS_SCORE = 11
    private const val CARDS_TO_DEAL_ON_SPLIT = 2

    fun hit(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.noop(state)

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
                if (newHand.score == BLACKJACK_SCORE) add(GameEffect.Pulse21)
                if (newHand.score == NEAR_MISS_SCORE) add(GameEffect.NearMissHighlight(state.activeHandIndex))
                if (newHand.isBust) add(GameEffect.BustThud)
            }
        return PlayerActionOutcome(
            state = updatedState,
            effects = effects,
            shouldAdvanceTurn = newHand.isBust
        )
    }

    fun stand(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.noop(state)
        return PlayerActionOutcome(state = state, shouldAdvanceTurn = true)
    }

    fun doubleDown(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.PLAYING || !state.canDoubleDown()) {
            return PlayerActionOutcome.noop(state)
        }

        val drawnCard = state.deck.firstOrNull() ?: return PlayerActionOutcome.noop(state)
        val remainingDeck = state.deck.drop(1).toPersistentList()
        val newHand = state.activeHand.copy(cards = state.activeHand.cards.add(drawnCard))
        val updatedHands = state.playerHands.set(state.activeHandIndex, newHand)

        // Double the bet for this hand; deduct the extra (original activeBet) from balance.
        val newBets = state.playerBets.set(state.activeHandIndex, state.activeBet * 2)
        val newBalance = state.balance - state.activeBet

        val updatedState =
            state.copy(
                deck = remainingDeck,
                playerHands = updatedHands,
                playerBets = newBets,
                balance = newBalance,
            )

        val effects =
            buildList {
                add(GameEffect.PlayCardSound)
                if (drawnCard.rank.value >= HIGH_CARD_VALUE) add(GameEffect.HeavyCardThud)
                if (newHand.score == BLACKJACK_SCORE) add(GameEffect.Pulse21)
                if (newHand.isBust) {
                    add(GameEffect.PlayLoseSound)
                    add(GameEffect.BustThud)
                    add(GameEffect.ChipLoss(state.activeBet * 2))
                }
            }

        return PlayerActionOutcome(
            state = updatedState,
            effects = effects,
            shouldAdvanceTurn = true
        )
    }

    fun split(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.PLAYING || !state.canSplit()) {
            return PlayerActionOutcome.noop(state)
        }
        if (state.deck.size < CARDS_TO_DEAL_ON_SPLIT) return PlayerActionOutcome.noop(state)

        val card1 = state.activeHand.cards[0]
        val card2 = state.activeHand.cards[1]

        // Use sequential draws to ensure deck consistency
        val cardFromDeck1 = state.deck[0]
        val cardFromDeck2 = state.deck[1]

        val newPrimaryHand = Hand(persistentListOf(card1, cardFromDeck1))
        val newSplitHand = Hand(persistentListOf(card2, cardFromDeck2))
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
        val updatedBets =
            state.playerBets
                .add(state.activeHandIndex + 1, state.activeBet)

        val updatedState =
            state.copy(
                deck = state.deck.drop(CARDS_TO_DEAL_ON_SPLIT).toPersistentList(),
                playerHands = updatedHands,
                playerBets = updatedBets,
                balance = state.balance - state.activeBet,
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
