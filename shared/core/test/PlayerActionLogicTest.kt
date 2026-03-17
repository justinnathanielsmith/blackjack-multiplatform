package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerActionLogicTest {

    @Test
    fun stand_returnsNoop_whenGameNotPlaying() {
        val state = GameState(status = GameStatus.PLAYER_WON)
        val outcome = PlayerActionLogic.stand(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun stand_returnsTrueForAdvanceTurn_whenGameIsPlaying() {
        val state = playingState(
            playerHand = hand(Rank.TEN, Rank.TEN),
            dealerHand = dealerHand(Rank.TEN, Rank.NINE)
        )
        val outcome = PlayerActionLogic.stand(state)

        assertEquals(state, outcome.state)
        assertTrue(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun doubleDown_returnsNoop_whenGameNotPlaying() {
        val state = GameState(status = GameStatus.PLAYER_WON)
        val outcome = PlayerActionLogic.doubleDown(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun doubleDown_returnsNoop_whenCannotDoubleDown_dueToBalance() {
        val state = playingState(
            balance = 50,
            bet = 100,
            playerHand = hand(Rank.FIVE, Rank.SIX),
            dealerHand = dealerHand(Rank.TEN, Rank.NINE)
        )
        val outcome = PlayerActionLogic.doubleDown(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun doubleDown_returnsNoop_whenCannotDoubleDown_dueToCardCount() {
        val state = playingState(
            balance = 500,
            bet = 100,
            playerHand = hand(Rank.TWO, Rank.THREE, Rank.FOUR),
            dealerHand = dealerHand(Rank.TEN, Rank.NINE)
        )
        val outcome = PlayerActionLogic.doubleDown(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun doubleDown_returnsNoop_whenDeckIsEmpty() {
        val state = playingState(
            balance = 500,
            bet = 100,
            playerHand = hand(Rank.FIVE, Rank.SIX),
            dealerHand = dealerHand(Rank.TEN, Rank.NINE),
            deck = persistentListOf()
        )
        val outcome = PlayerActionLogic.doubleDown(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun doubleDown_updatesStateCorrectly_andAdvancesTurn() {
        val state = playingState(
            balance = 500,
            bet = 100,
            playerHand = hand(Rank.FIVE, Rank.SIX),
            dealerHand = dealerHand(Rank.TEN, Rank.NINE),
            deck = deckOf(Rank.EIGHT, Rank.NINE)
        )
        val outcome = PlayerActionLogic.doubleDown(state)

        val newState = outcome.state
        assertEquals(400, newState.balance)
        assertEquals(200, newState.playerBets[0])
        assertEquals(3, newState.playerHands[0].cards.size)
        assertEquals(Rank.EIGHT, newState.playerHands[0].cards[2].rank)
        assertEquals(1, newState.deck.size)
        assertEquals(Rank.NINE, newState.deck[0].rank)

        assertTrue(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.contains(GameEffect.PlayCardSound))
    }

    @Test
    fun doubleDown_addsCorrectEffects_whenDrawnCardIsTenOrMore() {
        val state = playingState(
            balance = 500,
            bet = 100,
            playerHand = hand(Rank.FIVE, Rank.SIX),
            dealerHand = dealerHand(Rank.TEN, Rank.NINE),
            deck = deckOf(Rank.TEN)
        )
        val outcome = PlayerActionLogic.doubleDown(state)

        assertTrue(outcome.effects.contains(GameEffect.HeavyCardThud))
        assertTrue(outcome.effects.contains(GameEffect.Pulse21)) // 5 + 6 + 10 = 21
    }

    @Test
    fun doubleDown_addsCorrectEffects_whenHandBusts() {
        val state = playingState(
            balance = 500,
            bet = 100,
            playerHand = hand(Rank.TEN, Rank.SIX), // 16
            dealerHand = dealerHand(Rank.TEN, Rank.NINE),
            deck = deckOf(Rank.TEN) // 26 -> Bust
        )
        val outcome = PlayerActionLogic.doubleDown(state)

        assertTrue(outcome.effects.contains(GameEffect.PlayLoseSound))
        assertTrue(outcome.effects.contains(GameEffect.Vibrate))
        assertTrue(outcome.effects.contains(GameEffect.ChipLoss(200)))
    }
}
