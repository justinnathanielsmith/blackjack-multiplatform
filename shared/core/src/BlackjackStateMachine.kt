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
    initialState: GameState = GameState()
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
                    is GameAction.NewGame -> handleNewGame()
                    is GameAction.Hit -> handleHit()
                    is GameAction.Stand -> handleStand()
                }
            }
        }
    }

    private fun handleNewGame() {
        val fullDeck =
            Suit.entries
                .flatMap { suit ->
                    Rank.entries.map { rank -> Card(rank, suit) }
                }.shuffled()

        val playerHand = Hand(fullDeck.take(2))
        val dealerHand = Hand(fullDeck.drop(2).take(2))
        val remainingDeck = fullDeck.drop(INITIAL_DEAL_CARDS)

        val initialStatus =
            when {
                playerHand.score == 21 && dealerHand.score == 21 -> GameStatus.PUSH
                playerHand.score == 21 -> GameStatus.PLAYER_WON
                dealerHand.score == 21 -> GameStatus.DEALER_WON
                else -> GameStatus.PLAYING
            }

        _state.value =
            GameState(
                deck = remainingDeck,
                playerHand = playerHand,
                dealerHand = dealerHand,
                status = initialStatus
            )
        emitEffect(GameEffect.PlayCardSound)
        if (initialStatus == GameStatus.PLAYER_WON) {
            emitEffect(GameEffect.PlayWinSound)
        }
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

    private fun handleStand() {
        val currentState = _state.value
        if (currentState.status != GameStatus.PLAYING) return

        scope.launch {
            mutex.withLock {
                _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)
                
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

                _state.value =
                    _state.value.copy(
                        status = finalStatus
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
