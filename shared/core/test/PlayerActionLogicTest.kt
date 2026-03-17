package io.github.smithjustinn.blackjack

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerActionLogicTest {
    @Test
    fun hit_returnsNoop_whenGameNotPlaying() {
        val state = GameState(status = GameStatus.PLAYER_WON)
        val outcome = PlayerActionLogic.hit(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun hit_returnsNoop_whenHandIsSplitAceAndHasTwoCards() {
        val state =
            playingState(
                playerHand = Hand(persistentListOf(card(Rank.ACE), card(Rank.FIVE)), isFromSplitAce = true),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE)
            )
        val outcome = PlayerActionLogic.hit(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun hit_returnsNoop_whenDeckIsEmpty() {
        val state =
            playingState(
                playerHand = hand(Rank.TEN, Rank.FIVE),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = persistentListOf()
            )
        val outcome = PlayerActionLogic.hit(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun hit_addsCardToHand_removesFromDeck() {
        val state =
            playingState(
                playerHand = hand(Rank.TEN, Rank.FIVE),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.FOUR, Rank.TWO)
            )
        val outcome = PlayerActionLogic.hit(state)

        assertEquals(3, outcome.state.activeHand.cards.size)
        assertEquals(
            Rank.FOUR,
            outcome.state.activeHand.cards
                .last()
                .rank
        )
        assertEquals(1, outcome.state.deck.size)
        assertEquals(
            Rank.TWO,
            outcome.state.deck
                .first()
                .rank
        )
        assertFalse(outcome.shouldAdvanceTurn)
        assertEquals(listOf(GameEffect.PlayCardSound), outcome.effects)
    }

    @Test
    fun hit_advancesTurn_whenHandBusts() {
        val state =
            playingState(
                playerHand = hand(Rank.TEN, Rank.FIVE),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.TEN)
            )
        val outcome = PlayerActionLogic.hit(state)

        assertTrue(outcome.state.activeHand.isBust)
        assertTrue(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.contains(GameEffect.PlayCardSound))
    }

    @Test
    fun hit_addsHeavyCardThudEffect_whenCardValueIsTenOrMore() {
        val state =
            playingState(
                playerHand = hand(Rank.TWO, Rank.THREE),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.JACK)
            )
        val outcome = PlayerActionLogic.hit(state)

        assertTrue(outcome.effects.contains(GameEffect.HeavyCardThud))
    }

    @Test
    fun hit_addsPulse21Effect_whenHandScoreIs21() {
        val state =
            playingState(
                playerHand = hand(Rank.TEN, Rank.SIX),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.FIVE)
            )
        val outcome = PlayerActionLogic.hit(state)

        assertEquals(21, outcome.state.activeHand.score)
        assertTrue(outcome.effects.contains(GameEffect.Pulse21))
    }

    @Test
    fun hit_addsNearMissHighlightEffect_whenHandScoreIs11() {
        val state =
            playingState(
                playerHand = hand(Rank.FIVE, Rank.FOUR),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.TWO)
            )
        val outcome = PlayerActionLogic.hit(state)

        assertEquals(11, outcome.state.activeHand.score)
        assertTrue(outcome.effects.contains(GameEffect.NearMissHighlight(state.activeHandIndex)))
    }

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
        val state =
            playingState(
                playerHand = hand(Rank.TEN, Rank.TEN),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE)
            )
        val outcome = PlayerActionLogic.stand(state)

        assertEquals(state, outcome.state)
        assertTrue(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }
}
