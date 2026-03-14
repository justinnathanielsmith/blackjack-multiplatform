package io.github.smithjustinn.blackjack

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

@Serializable
data class Card(
    val rank: Rank,
    val suit: Suit,
    val isFaceDown: Boolean = false
)

@Serializable
data class Hand(
    val cards: List<Card> = emptyList()
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
}

@Serializable
enum class GameStatus {
    BETTING,
    IDLE,
    PLAYING,
    DEALER_TURN,
    PLAYER_WON,
    DEALER_WON,
    PUSH
}

@Serializable
data class GameState(
    val deck: List<Card> = emptyList(),
    val playerHand: Hand = Hand(),
    val dealerHand: Hand = Hand(),
    val status: GameStatus = GameStatus.IDLE,
    val balance: Int = 1000,
    val currentBet: Int = 0
)

sealed class GameAction {
    data class NewGame(
        val initialBalance: Int? = null
    ) : GameAction()

    data class PlaceBet(
        val amount: Int
    ) : GameAction()

    data object ResetBet : GameAction()

    data object Deal : GameAction()

    data object Hit : GameAction()

    data object Stand : GameAction()
}

sealed class GameEffect {
    data object PlayCardSound : GameEffect()

    data object PlayWinSound : GameEffect()

    data object PlayLoseSound : GameEffect()

    data object Vibrate : GameEffect()
}
