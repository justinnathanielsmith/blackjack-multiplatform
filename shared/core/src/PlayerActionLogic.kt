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

        // Block hits on 21 or higher.
        if (state.activeHand.score >= BLACKJACK_SCORE) {
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
        val standingHand = state.activeHand.copy(isStanding = true)
        val updatedHands = state.playerHands.set(state.activeHandIndex, standingHand)
        return PlayerActionOutcome(state = state.copy(playerHands = updatedHands), shouldAdvanceTurn = true)
    }

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
                if (newHand.score == BLACKJACK_SCORE) add(GameEffect.Pulse21)
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
            state.playerHands.size < GameState.MAX_HANDS &&
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
