package io.github.smithjustinn.blackjack

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
enum class Suit {
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
}

@Serializable
enum class Rank(
    val value: Int
) {
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(10),
    QUEEN(10),
    KING(10),
    ACE(11)
}

@Immutable
@Serializable
data class Card(
    val rank: Rank,
    val suit: Suit,
    val isFaceDown: Boolean = false
)

@Immutable
@Serializable
data class Hand(
    val cards: PersistentList<Card> = persistentListOf()
) {
    val score: Int
        get() {
            var s = cards.sumOf { it.rank.value }
            var aces = cards.count { it.rank == Rank.ACE }
            while (s > 21 && aces > 0) {
                s -= 10
                aces -= 1
            }
            return s
        }

    val visibleScore: Int
        get() {
            val faceUpCards = cards.filter { !it.isFaceDown }
            var s = faceUpCards.sumOf { it.rank.value }
            var aces = faceUpCards.count { it.rank == Rank.ACE }
            while (s > 21 && aces > 0) {
                s -= 10
                aces -= 1
            }
            return s
        }

    val isBust: Boolean get() = score > 21

    val isSoft: Boolean
        get() {
            if (cards.none { it.rank == Rank.ACE }) return false
            var s = cards.sumOf { it.rank.value }
            var aces = cards.count { it.rank == Rank.ACE }
            val hardScore = cards.sumOf { if (it.rank == Rank.ACE) 1 else it.rank.value }
            // If the current score is different from the score where all aces are 1, it's soft.
            // Actually, a hand is soft if it contains an Ace that is being counted as 11.
            // Our score calculation already handles this.
            return score != hardScore
        }
}

@Serializable
enum class BlackjackPayout(val numerator: Int, val denominator: Int) {
    THREE_TO_TWO(3, 2),
    SIX_TO_FIVE(6, 5),
}

@Serializable
data class GameRules(
    val dealerHitsSoft17: Boolean = true,
    val allowDoubleAfterSplit: Boolean = true,
    val allowSurrender: Boolean = false,
    val blackjackPayout: BlackjackPayout = BlackjackPayout.THREE_TO_TWO,
    val deckCount: Int = 6,
)

@Serializable
enum class GameStatus {
    BETTING,
    IDLE,
    PLAYING,
    INSURANCE_OFFERED,
    DEALER_TURN,
    PLAYER_WON,
    DEALER_WON,
    PUSH
}

@Immutable
@Serializable
data class GameState(
    val deck: PersistentList<Card> = persistentListOf(),
    val playerHands: PersistentList<Hand> = persistentListOf(Hand()),
    val playerBets: PersistentList<Int> = persistentListOf(0),
    val activeHandIndex: Int = 0,
    val handCount: Int = 1,
    val dealerHand: Hand = Hand(),
    val status: GameStatus = GameStatus.IDLE,
    val balance: Int = 1000,
    val currentBet: Int = 0,
    val insuranceBet: Int = 0,
    val rules: GameRules = GameRules(),
) {
    companion object {
        const val MAX_HANDS = 4
    }

    val activeHand: Hand get() = playerHands[activeHandIndex]
    val activeBet: Int get() = playerBets[activeHandIndex]

    fun canDoubleDown(): Boolean =
        activeHand.cards.size == 2 &&
            balance >= activeBet &&
            (activeHandIndex == 0 || rules.allowDoubleAfterSplit)

    fun canSplit(): Boolean =
        playerHands.size < MAX_HANDS &&
            activeHand.cards.size == 2 &&
            activeHand.cards[0].rank == activeHand.cards[1].rank &&
            balance >= currentBet
}

sealed class GameAction {
    data class NewGame(
        val initialBalance: Int? = null,
        val rules: GameRules = GameRules(),
    ) : GameAction()

    data object Surrender : GameAction()

    data class PlaceBet(
        val amount: Int
    ) : GameAction()

    data object ResetBet : GameAction()

    data object Deal : GameAction()

    data object Hit : GameAction()

    data object Stand : GameAction()

    data object DoubleDown : GameAction()

    data object TakeInsurance : GameAction()

    data object DeclineInsurance : GameAction()

    data object Split : GameAction()

    data class SelectHandCount(
        val count: Int
    ) : GameAction()
}

sealed class GameEffect {
    data object PlayCardSound : GameEffect()

    data object PlayWinSound : GameEffect()

    data object PlayLoseSound : GameEffect()

    data object Vibrate : GameEffect()
}
