package io.github.smithjustinn.blackjack

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BlackjackStateMachine(
    private val scope: CoroutineScope,
    initialState: GameState = GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0)
) {
    companion object {
        private const val INITIAL_DEAL_CARDS = 4
        private const val DEALER_STAND_THRESHOLD = 17
        private const val DEALER_TURN_DELAY_MS = 600L
    }

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 64)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    private val mutex = Mutex()

    fun dispatch(action: GameAction) {
        scope.launch {
            mutex.withLock {
                when (action) {
                    is GameAction.NewGame -> handleNewGame(action.initialBalance)
                    is GameAction.PlaceBet -> handlePlaceBet(action.amount)
                    is GameAction.ResetBet -> handleResetBet()
                    is GameAction.Deal -> handleDeal()
                    is GameAction.Hit -> handleHit()
                    is GameAction.Stand -> handleStand()
                }
            }
        }
    }

    private fun handlePlaceBet(amount: Int) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        if (amount <= 0 || amount > current.balance) return
        _state.value =
            current.copy(
                balance = current.balance - amount,
                currentBet = current.currentBet + amount
            )
    }

    private fun handleResetBet() {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        _state.value =
            current.copy(
                balance = current.balance + current.currentBet,
                currentBet = 0
            )
    }

    private fun handleDeal() {
        val current = _state.value
        if (current.status != GameStatus.BETTING || current.currentBet <= 0) return
        val fullDeck =
            Suit.entries
                .flatMap { suit ->
                    Rank.entries.map { rank -> Card(rank, suit) }
                }.shuffled()
        val playerHand = Hand(fullDeck.take(2))
        val dealerCards = fullDeck.drop(2).take(2)
        val dealerHandHidden = Hand(listOf(dealerCards[0], dealerCards[1].copy(isFaceDown = true)))
        val remainingDeck = fullDeck.drop(INITIAL_DEAL_CARDS)
        val initialStatus =
            when {
                playerHand.score == 21 && dealerHandHidden.score == 21 -> GameStatus.PUSH
                playerHand.score == 21 -> GameStatus.PLAYER_WON
                dealerHandHidden.score == 21 -> GameStatus.DEALER_WON
                else -> GameStatus.PLAYING
            }
        // Reveal hole card when dealer's hand is exposed (PUSH or DEALER_WON); keep hidden otherwise
        val dealerHand =
            when (initialStatus) {
                GameStatus.PUSH, GameStatus.DEALER_WON ->
                    Hand(dealerHandHidden.cards.map { it.copy(isFaceDown = false) })
                else -> dealerHandHidden
            }
        val balanceUpdate =
            when (initialStatus) {
                GameStatus.PLAYER_WON -> current.currentBet * 2
                GameStatus.PUSH -> current.currentBet
                else -> 0
            }

        _state.value =
            GameState(
                deck = remainingDeck,
                playerHand = playerHand,
                dealerHand = dealerHand,
                status = initialStatus,
                balance = current.balance + balanceUpdate,
                currentBet = current.currentBet
            )
        emitEffect(GameEffect.PlayCardSound)
        if (initialStatus == GameStatus.PLAYER_WON) {
            emitEffect(GameEffect.PlayWinSound)
        }
    }

    private fun handleNewGame(initialBalance: Int? = null) {
        val currentState = _state.value
        _state.value =
            GameState(
                status = GameStatus.BETTING,
                balance = initialBalance ?: currentState.balance,
                currentBet = 0
            )
    }

    private fun handleHit() {
        val currentState = _state.value
        if (currentState.status != GameStatus.PLAYING) return

        val newCard = currentState.deck.first()
        val newPlayerHand = currentState.playerHand.copy(cards = currentState.playerHand.cards + newCard)
        val newStatus = if (newPlayerHand.isBust) GameStatus.DEALER_WON else GameStatus.PLAYING

        _state.value =
            currentState.copy(
                deck = currentState.deck.drop(1),
                playerHand = newPlayerHand,
                status = newStatus
            )
        emitEffect(GameEffect.PlayCardSound)
        if (newStatus == GameStatus.DEALER_WON) {
            emitEffect(GameEffect.PlayLoseSound)
            emitEffect(GameEffect.Vibrate)
        }
    }

    private fun revealDealerHoleCard() {
        _state.value =
            _state.value.copy(
                dealerHand =
                    _state.value.dealerHand.copy(
                        cards =
                            _state.value.dealerHand.cards
                                .map { it.copy(isFaceDown = false) }
                    )
            )
    }

    private fun handleStand() {
        val currentState = _state.value
        if (currentState.status != GameStatus.PLAYING) return

        scope.launch {
            mutex.withLock {
                _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)

                revealDealerHoleCard()
                delay(DEALER_TURN_DELAY_MS) // Visual pause for hole card reveal

                var currentDealerHand = _state.value.dealerHand
                var currentDeck = _state.value.deck

                while (currentDealerHand.score < DEALER_STAND_THRESHOLD) {
                    val newCard = currentDeck.firstOrNull() ?: break
                    currentDealerHand = currentDealerHand.copy(cards = currentDealerHand.cards + newCard)
                    currentDeck = currentDeck.drop(1)

                    _state.value =
                        _state.value.copy(
                            deck = currentDeck,
                            dealerHand = currentDealerHand
                        )
                    emitEffect(GameEffect.PlayCardSound)
                    delay(DEALER_TURN_DELAY_MS) // Visual pacing for dealer hits
                }

                val playerScore = _state.value.playerHand.score
                val dealerScore = _state.value.dealerHand.score

                val finalStatus =
                    when {
                        _state.value.dealerHand.isBust -> GameStatus.PLAYER_WON
                        playerScore > dealerScore -> GameStatus.PLAYER_WON
                        playerScore < dealerScore -> GameStatus.DEALER_WON
                        else -> GameStatus.PUSH
                    }

                val balanceUpdate =
                    when (finalStatus) {
                        GameStatus.PLAYER_WON -> _state.value.currentBet * 2
                        GameStatus.PUSH -> _state.value.currentBet
                        else -> 0
                    }

                _state.value =
                    _state.value.copy(
                        status = finalStatus,
                        balance = _state.value.balance + balanceUpdate
                    )

                when (finalStatus) {
                    GameStatus.PLAYER_WON -> emitEffect(GameEffect.PlayWinSound)
                    GameStatus.DEALER_WON -> {
                        emitEffect(GameEffect.PlayLoseSound)
                        emitEffect(GameEffect.Vibrate)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun emitEffect(effect: GameEffect) {
        _effects.tryEmit(effect)
    }
}
