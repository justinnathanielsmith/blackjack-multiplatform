package io.github.smithjustinn.blackjack

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
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
    val cards: PersistentList<Card> = persistentListOf(),
    val wasSplit: Boolean = false,
    val isFromSplitAce: Boolean = false
) {
    val score: Int
        get() {
            var s = 0
            var aces = 0
            for (card in cards) {
                s += card.rank.value
                if (card.rank == Rank.ACE) {
                    aces++
                }
            }
            while (s > 21 && aces > 0) {
                s -= 10
                aces -= 1
            }
            return s
        }

    val visibleScore: Int
        get() {
            var s = 0
            var aces = 0
            for (card in cards) {
                if (!card.isFaceDown) {
                    s += card.rank.value
                    if (card.rank == Rank.ACE) {
                        aces++
                    }
                }
            }
            while (s > 21 && aces > 0) {
                s -= 10
                aces -= 1
            }
            return s
        }

    val isBust: Boolean get() = score > 21

    val isSoft: Boolean
        get() {
            var hasAce = false
            var hardScore = 0
            for (card in cards) {
                if (card.rank == Rank.ACE) {
                    hasAce = true
                    hardScore += 1
                } else {
                    hardScore += card.rank.value
                }
            }
            if (!hasAce) return false
            // If the current score is different from the score where all aces are 1, it's soft.
            // Actually, a hand is soft if it contains an Ace that is being counted as 11.
            // Our score calculation already handles this.
            return score != hardScore
        }
}

@Serializable
enum class BlackjackPayout(
    val numerator: Int,
    val denominator: Int
) {
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
    DEALING,
    IDLE,
    PLAYING,
    INSURANCE_OFFERED,
    DEALER_TURN,
    PLAYER_WON,
    DEALER_WON,
    PUSH
}

fun GameStatus.isTerminal() = this == GameStatus.PLAYER_WON || this == GameStatus.DEALER_WON || this == GameStatus.PUSH

@Serializable
enum class SideBetType {
    TWENTY_ONE_PLUS_THREE,
    PERFECT_PAIRS
}

@Immutable
@Serializable
data class SideBetResult(
    val type: SideBetType,
    val payoutMultiplier: Int,
    val payoutAmount: Int,
    // e.g., "Flush", "Perfect Pair"
    val outcomeName: String
)

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
    val sideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    val sideBetResults: PersistentMap<SideBetType, SideBetResult> = persistentMapOf(),
    val lastSideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    val rules: GameRules = GameRules(),
    val dealerDrawIsCritical: Boolean = false,
) {
    companion object {
        const val MAX_HANDS = 4
    }

    val totalBet: Int
        get() {
            val mainBetsTotal =
                if (status == GameStatus.BETTING) {
                    currentBet * handCount
                } else {
                    playerBets.sum()
                }

            // Only count active side bets (before they are settled)
            val sideBetsTotal =
                if (sideBetResults.isEmpty()) {
                    sideBets.values.sum()
                } else {
                    0
                }

            return mainBetsTotal + sideBetsTotal + insuranceBet
        }

    val activeHand: Hand get() = playerHands[activeHandIndex]
    val activeBet: Int get() = playerBets[activeHandIndex]

    fun canDoubleDown(): Boolean =
        activeHand.cards.size == 2 &&
            balance >= activeBet &&
            (!activeHand.wasSplit || rules.allowDoubleAfterSplit)

    fun canSplit(): Boolean =
        playerHands.size < MAX_HANDS &&
            activeHand.cards.size == 2 &&
            activeHand.cards[0].rank == activeHand.cards[1].rank &&
            balance >= activeBet
}

enum class HandOutcome { NATURAL_WIN, WIN, PUSH, LOSS }

object BlackjackRules {
    const val BLACKJACK_SCORE = 21
    const val DEALER_STAND_THRESHOLD = 17
    const val DEALER_STIFF_MIN = 12
    const val CARDS_PER_DECK = 52

    fun shouldDealerDraw(
        hand: Hand,
        rules: GameRules
    ): Boolean {
        if (hand.score < DEALER_STAND_THRESHOLD) return true
        if (hand.score == DEALER_STAND_THRESHOLD && rules.dealerHitsSoft17 && hand.isSoft) return true
        return false
    }

    fun determineHandOutcome(
        hand: Hand,
        dealerScore: Int,
        dealerBust: Boolean
    ): HandOutcome {
        if (hand.isBust) return HandOutcome.LOSS
        val isNaturalBJ = hand.cards.size == 2 && hand.score == 21 && !hand.wasSplit
        return when {
            isNaturalBJ && dealerScore != 21 -> HandOutcome.NATURAL_WIN
            dealerBust || hand.score > dealerScore -> HandOutcome.WIN
            hand.score == dealerScore -> HandOutcome.PUSH
            else -> HandOutcome.LOSS
        }
    }

    fun resolveHand(
        hand: Hand,
        bet: Int,
        dealerScore: Int,
        dealerBust: Boolean,
        rules: GameRules,
    ): Int =
        when (determineHandOutcome(hand, dealerScore, dealerBust)) {
            HandOutcome.NATURAL_WIN -> bet + (bet * rules.blackjackPayout.numerator) / rules.blackjackPayout.denominator
            HandOutcome.WIN -> bet * 2
            HandOutcome.PUSH -> bet
            HandOutcome.LOSS -> 0
        }

    fun calculateHandResults(
        state: GameState,
        dealerScore: Int,
        dealerBust: Boolean,
    ): Triple<Int, Boolean, Boolean> {
        var totalPayout = 0
        var anyWin = false
        var allPush = true
        for (i in state.playerHands.indices) {
            val hand = state.playerHands[i]
            val bet = state.playerBets[i]
            totalPayout += resolveHand(hand, bet, dealerScore, dealerBust, state.rules)
            val handWins = !hand.isBust && (dealerBust || hand.score > dealerScore)
            val handPushes = !hand.isBust && !dealerBust && hand.score == dealerScore
            if (handWins) anyWin = true
            if (!handPushes) allPush = false
        }
        return Triple(totalPayout, anyWin, allPush)
    }

    fun determineInitialStatus(
        hands: List<Hand>,
        dealerHand: Hand,
        handCount: Int
    ): GameStatus {
        val dealerHasBJ = dealerHand.score == BLACKJACK_SCORE && dealerHand.cards.size == 2

        if (handCount == 1) {
            val playerHasBJ = hands[0].score == BLACKJACK_SCORE && hands[0].cards.size == 2
            return when {
                playerHasBJ && dealerHasBJ -> GameStatus.PUSH
                playerHasBJ -> GameStatus.PLAYER_WON
                dealerHasBJ -> GameStatus.DEALER_WON
                else -> GameStatus.PLAYING
            }
        }

        // Multi-hand: If dealer has Blackjack, it's immediately terminal for simplicity
        if (dealerHasBJ) return GameStatus.DEALER_WON

        return GameStatus.PLAYING
    }

    fun resolveInitialOutcomeValues(
        current: GameState,
        playerHands: List<Hand>,
        dealerHand: Hand,
    ): Triple<GameStatus, Hand, Int> {
        val playerHasBJ =
            current.handCount == 1 && playerHands[0].score == BLACKJACK_SCORE && playerHands[0].cards.size == 2
        val shouldOfferInsurance = current.handCount == 1 && !playerHasBJ && dealerHand.cards[0].rank == Rank.ACE
        val dealerHandRevealed = Hand(dealerHand.cards.map { it.copy(isFaceDown = false) }.toPersistentList())

        val initialStatus =
            if (shouldOfferInsurance) {
                GameStatus.INSURANCE_OFFERED
            } else {
                determineInitialStatus(playerHands, dealerHandRevealed, current.handCount)
            }

        val finalDealerHand =
            if (initialStatus == GameStatus.PUSH || initialStatus == GameStatus.DEALER_WON) {
                dealerHandRevealed
            } else {
                dealerHand
            }

        val balanceUpdate =
            when (initialStatus) {
                GameStatus.PLAYER_WON ->
                    (current.currentBet * current.rules.blackjackPayout.numerator) /
                        current.rules.blackjackPayout.denominator + current.currentBet
                GameStatus.PUSH -> current.currentBet
                else -> 0
            }

        return Triple(initialStatus, finalDealerHand, balanceUpdate)
    }

    fun createDeck(
        rules: GameRules,
        random: kotlin.random.Random
    ): List<Card> {
        val deckSize = rules.deckCount * CARDS_PER_DECK
        val newDeck = ArrayList<Card>(deckSize)
        for (i in 1..rules.deckCount) {
            for (suit in Suit.entries) {
                for (rank in Rank.entries) {
                    newDeck.add(Card(rank, suit))
                }
            }
        }
        newDeck.shuffle(random)
        return newDeck
    }
}

sealed class GameAction {
    data class NewGame(
        val initialBalance: Int? = null,
        val rules: GameRules = GameRules(),
        val handCount: Int = 1,
        val lastBet: Int = 0,
        val lastSideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
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

    data class UpdateRules(
        val rules: GameRules
    ) : GameAction()

    data class PlaceSideBet(
        val type: SideBetType,
        val amount: Int
    ) : GameAction()

    data object ResetSideBets : GameAction()
}

sealed class GameEffect {
    data object PlayCardSound : GameEffect()

    data object PlayWinSound : GameEffect()

    data object PlayLoseSound : GameEffect()

    data object Vibrate : GameEffect()

    data object DealerCriticalDraw : GameEffect()

    data class NearMissHighlight(
        val handIndex: Int
    ) : GameEffect()

    data object HeavyCardThud : GameEffect()

    data object Pulse21 : GameEffect()

    data object LightTick : GameEffect()

    data object WinPulse : GameEffect()

    data object BustThud : GameEffect()

    data class ChipEruption(
        val amount: Int,
        val sideBetType: SideBetType? = null
    ) : GameEffect()

    data class ChipLoss(
        val amount: Int
    ) : GameEffect()

    data object PlayPlinkSound : GameEffect()
}
