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
    fun hit(state: GameState): PlayerActionOutcome {
        if (state.status != GameStatus.PLAYING) return PlayerActionOutcome.noop(state)

        // Block hits on split aces.
        if (state.activeHand.isFromSplitAce && state.activeHand.cards.size >= 2) {
            return PlayerActionOutcome.noop(state)
        }

        val newCard = state.deck.firstOrNull() ?: return PlayerActionOutcome.noop(state)
        val remainingDeck = state.deck.drop(1).toPersistentList()
        val newHand = state.activeHand.copy(cards = state.activeHand.cards.add(newCard))
        val updatedHands = state.playerHands.toPersistentList().set(state.activeHandIndex, newHand)
        val updatedState = state.copy(deck = remainingDeck, playerHands = updatedHands)

        val effects =
            buildList {
                add(GameEffect.PlayCardSound)
                if (newCard.rank.value >= 10) add(GameEffect.HeavyCardThud)
                if (newHand.score == 21) add(GameEffect.Pulse21)
                if (newHand.score == 11) add(GameEffect.NearMissHighlight(state.activeHandIndex))
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
        val updatedHands = state.playerHands.toPersistentList().set(state.activeHandIndex, newHand)

        // Double the bet for this hand; deduct the extra (original activeBet) from balance.
        val newBets = state.playerBets.toPersistentList().set(state.activeHandIndex, state.activeBet * 2)
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
                if (drawnCard.rank.value >= 10) add(GameEffect.HeavyCardThud)
                if (newHand.score == 21) add(GameEffect.Pulse21)
                if (newHand.isBust) {
                    add(GameEffect.PlayLoseSound)
                    add(GameEffect.Vibrate)
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
        if (state.deck.size < 2) return PlayerActionOutcome.noop(state)

        val card1 = state.activeHand.cards[0]
        val card2 = state.activeHand.cards[1]
        val newPrimaryHand = Hand(persistentListOf(card1, state.deck[0]))
        val newSplitHand = Hand(persistentListOf(card2, state.deck[1]))
        val isAceSplit = card1.rank == Rank.ACE

        val updatedHands =
            state.playerHands
                .toPersistentList()
                .set(
                    state.activeHandIndex,
                    newPrimaryHand.copy(wasSplit = true, isFromSplitAce = isAceSplit)
                ).add(
                    state.activeHandIndex + 1,
                    newSplitHand.copy(wasSplit = true, isFromSplitAce = isAceSplit)
                )
        val updatedBets =
            state.playerBets
                .toPersistentList()
                .add(state.activeHandIndex + 1, state.activeBet)

        val updatedState =
            state.copy(
                deck = state.deck.drop(2).toPersistentList(),
                playerHands = updatedHands,
                playerBets = updatedBets,
                balance = state.balance - state.activeBet,
            )

        val finalState =
            if (isAceSplit) {
                updatedState.copy(activeHandIndex = updatedState.activeHandIndex + 1)
            } else {
                updatedState
            }

        println("split resulting state: hands=${finalState.playerHands.size}, shouldAdvance=$isAceSplit")

        return PlayerActionOutcome(
            state = finalState,
            effects = listOf(GameEffect.PlayCardSound, GameEffect.PlayCardSound),
            shouldAdvanceTurn = isAceSplit
        )
    }
}
