package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
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
        assertEquals(listOf(GameEffect.PlayCardSound, GameEffect.LightTick), outcome.effects)
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
    fun hit_returnsNoop_whenHandScoreIs21() {
        val state =
            playingState(
                playerHand = hand(Rank.TEN, Rank.ACE), // 21
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.FIVE)
            )
        val outcome = PlayerActionLogic.hit(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
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

        val expectedHand = state.activeHand.copy(isStanding = true)
        assertEquals(
            state.copy(playerHands = state.playerHands.set(state.activeHandIndex, expectedHand)),
            outcome.state
        )
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
        val state =
            playingState(
                balance = 50,
                bet = 100,
                playerHand = hand(Rank.FIVE, Rank.SIX),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE)
            )
        val outcome = PlayerActionLogic.doubleDown(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.contains(GameEffect.Vibrate))
    }

    @Test
    fun doubleDown_returnsNoop_whenCannotDoubleDown_dueToCardCount() {
        val state =
            playingState(
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
        val state =
            playingState(
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
        val state =
            playingState(
                balance = 500,
                bet = 100,
                playerHand = hand(Rank.FIVE, Rank.SIX),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.EIGHT, Rank.NINE)
            )
        val outcome = PlayerActionLogic.doubleDown(state)

        val newState = outcome.state
        assertEquals(400, newState.balance)
        assertEquals(200, newState.playerHands[0].bet)
        assertEquals(3, newState.playerHands[0].cards.size)
        assertEquals(Rank.EIGHT, newState.playerHands[0].cards[2].rank)
        assertEquals(1, newState.deck.size)
        assertEquals(Rank.NINE, newState.deck[0].rank)

        assertTrue(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.contains(GameEffect.PlayCardSound))
    }

    @Test
    fun doubleDown_addsCorrectEffects_whenDrawnCardIsTenOrMore() {
        val state =
            playingState(
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
        val state =
            playingState(
                balance = 500,
                bet = 100,
                playerHand = hand(Rank.TEN, Rank.SIX), // 16
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.TEN) // 26 -> Bust
            )
        val outcome = PlayerActionLogic.doubleDown(state)

        assertTrue(outcome.effects.contains(GameEffect.PlayLoseSound))
        assertTrue(outcome.effects.contains(GameEffect.BustThud))
        assertTrue(outcome.effects.contains(GameEffect.ChipLoss(200)))
    }

    @Test
    fun split_returnsNoop_whenGameNotPlaying() {
        val state = GameState(status = GameStatus.PLAYER_WON)
        val outcome = PlayerActionLogic.split(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun split_returnsNoop_whenCannotSplit_dueToDifferentRanks() {
        val state =
            playingState(
                balance = 500,
                bet = 100,
                playerHand = hand(Rank.TEN, Rank.NINE),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE)
            )
        val outcome = PlayerActionLogic.split(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun split_returnsNoop_whenDeckHasLessThanTwoCards() {
        val state =
            playingState(
                balance = 500,
                bet = 100,
                playerHand = hand(Rank.EIGHT, Rank.EIGHT),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.TWO) // Only 1 card
            )
        val outcome = PlayerActionLogic.split(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun split_updatesStateCorrectly_andDoesNotAdvanceTurn_forNonAceSplit() {
        val state =
            playingState(
                balance = 500,
                bet = 100,
                playerHand = hand(Rank.EIGHT, Rank.EIGHT),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.TWO, Rank.THREE, Rank.FOUR)
            )
        val outcome = PlayerActionLogic.split(state)

        val newState = outcome.state
        assertEquals(400, newState.balance)
        assertEquals(2, newState.playerHands.size)

        val hand1 = newState.playerHands[0]
        val hand2 = newState.playerHands[1]

        assertEquals(2, hand1.cards.size)
        assertEquals(Rank.EIGHT, hand1.cards[0].rank)
        assertEquals(Rank.TWO, hand1.cards[1].rank)
        assertTrue(hand1.wasSplit)
        assertFalse(hand1.isFromSplitAce)

        assertEquals(2, hand2.cards.size)
        assertEquals(Rank.EIGHT, hand2.cards[0].rank)
        assertEquals(Rank.THREE, hand2.cards[1].rank)
        assertTrue(hand2.wasSplit)
        assertFalse(hand2.isFromSplitAce)

        assertEquals(100, newState.playerHands[0].bet)
        assertEquals(100, newState.playerHands[1].bet)

        assertEquals(1, newState.deck.size)
        assertEquals(Rank.FOUR, newState.deck[0].rank)

        assertFalse(outcome.shouldAdvanceTurn)
        assertEquals(listOf(GameEffect.PlayCardSound, GameEffect.PlayCardSound), outcome.effects)
        assertEquals(0, newState.activeHandIndex)
    }

    @Test
    fun split_updatesStateCorrectly_andAdvancesTurn_forAceSplit() {
        val state =
            playingState(
                balance = 500,
                bet = 100,
                playerHand = hand(Rank.ACE, Rank.ACE),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
                deck = deckOf(Rank.NINE, Rank.TEN, Rank.JACK)
            )
        val outcome = PlayerActionLogic.split(state)

        val newState = outcome.state
        assertEquals(400, newState.balance)
        assertEquals(2, newState.playerHands.size)

        val hand1 = newState.playerHands[0]
        val hand2 = newState.playerHands[1]

        assertEquals(2, hand1.cards.size)
        assertEquals(Rank.ACE, hand1.cards[0].rank)
        assertEquals(Rank.NINE, hand1.cards[1].rank)
        assertTrue(hand1.wasSplit)
        assertTrue(hand1.isFromSplitAce)

        assertEquals(2, hand2.cards.size)
        assertEquals(Rank.ACE, hand2.cards[0].rank)
        assertEquals(Rank.TEN, hand2.cards[1].rank)
        assertTrue(hand2.wasSplit)
        assertTrue(hand2.isFromSplitAce)

        assertEquals(100, newState.playerHands[0].bet)
        assertEquals(100, newState.playerHands[1].bet)

        assertEquals(1, newState.deck.size)
        assertEquals(Rank.JACK, newState.deck[0].rank)

        assertTrue(outcome.shouldAdvanceTurn)
        assertEquals(listOf(GameEffect.PlayCardSound, GameEffect.PlayCardSound), outcome.effects)
        assertEquals(1, newState.activeHandIndex)
    }
}
