package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import io.github.smithjustinn.blackjack.util.*
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanSplitTest {
    @Test
    fun canSplit_returnsTrue_whenConditionsAreMet() {
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.EIGHT, Rank.EIGHT).copy(bet = 100)),
                balance = 1000,
                status = GameStatus.PLAYING
            )
        assertTrue(state.canSplit)
    }

    @Test
    fun canSplit_returnsFalse_whenMaxHandsReached() {
        val state =
            GameState(
                playerHands =
                    persistentListOf(
                        hand(Rank.EIGHT, Rank.EIGHT).copy(bet = 100),
                        hand(Rank.TWO, Rank.THREE).copy(bet = 100),
                        hand(Rank.FOUR, Rank.FIVE).copy(bet = 100),
                        hand(Rank.SIX, Rank.SEVEN).copy(bet = 100),
                    ),
                balance = 1000,
                status = GameStatus.PLAYING
            )
        assertFalse(state.canSplit)
    }

    @Test
    fun canSplit_returnsFalse_whenInsufficientBalance() {
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.EIGHT, Rank.EIGHT).copy(bet = 100)),
                balance = 50,
                status = GameStatus.PLAYING
            )
        assertFalse(state.canSplit)
    }

    @Test
    fun canSplit_returnsFalse_whenRanksDoNotMatch() {
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.EIGHT, Rank.NINE).copy(bet = 100)),
                balance = 1000,
                status = GameStatus.PLAYING
            )
        assertFalse(state.canSplit)
    }

    @Test
    fun canSplit_returnsFalse_whenRanksHaveSameValueButDifferentRank() {
        // King and Queen both have value 10, but different rank.
        // canSplit logic: activeHand.cards[0].rank == activeHand.cards[1].rank
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.KING, Rank.QUEEN).copy(bet = 100)),
                balance = 1000,
                status = GameStatus.PLAYING
            )
        assertFalse(state.canSplit)
    }

    @Test
    fun canSplit_returnsFalse_whenNotTwoCards() {
        val state1 =
            GameState(
                playerHands = persistentListOf(hand(Rank.EIGHT).copy(bet = 100)),
                balance = 1000,
                status = GameStatus.PLAYING
            )
        assertFalse(state1.canSplit)

        val state3 =
            GameState(
                playerHands = persistentListOf(hand(Rank.EIGHT, Rank.EIGHT, Rank.EIGHT).copy(bet = 100)),
                balance = 1000,
                status = GameStatus.PLAYING
            )
        assertFalse(state3.canSplit)
    }

    @Test
    fun canSplit_respectsActiveHandIndex() {
        val state =
            GameState(
                playerHands =
                    persistentListOf(
                        hand(Rank.TEN, Rank.TEN).copy(bet = 100), // Hand 0: splitable
                        hand(Rank.FIVE, Rank.SIX).copy(bet = 100), // Hand 1: not splitable
                    ),
                activeHandIndex = 1,
                balance = 1000,
                status = GameStatus.PLAYING
            )
        assertFalse(state.canSplit)

        val state0 = state.copy(activeHandIndex = 0)
        assertTrue(state0.canSplit)
    }
}
