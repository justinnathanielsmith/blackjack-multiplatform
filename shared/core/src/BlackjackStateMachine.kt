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

@Suppress("TooManyFunctions")
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
                    is GameAction.DoubleDown -> handleDoubleDown()
                    is GameAction.TakeInsurance -> handleTakeInsurance()
                    is GameAction.DeclineInsurance -> handleDeclineInsurance()
                    is GameAction.Split -> handleSplit()
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

    @Suppress("CyclomaticComplexMethod")
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
        if (initialStatus == GameStatus.PLAYING && dealerCards[0].rank == Rank.ACE) {
            _state.value = _state.value.copy(status = GameStatus.INSURANCE_OFFERED)
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

        // Block hits on split aces
        if (currentState.splitHand != null && currentState.playerHand.cards[0].rank == Rank.ACE) return

        val newCard = currentState.deck.first()
        val remainingDeck = currentState.deck.drop(1)

        when {
            currentState.splitHand != null && !currentState.isPlayingSplitHand -> {
                // Playing primary hand during split
                val newPlayerHand = currentState.playerHand.copy(cards = currentState.playerHand.cards + newCard)
                _state.value =
                    if (newPlayerHand.isBust) {
                        // Primary busts → advance to split hand, game continues
                        currentState.copy(deck = remainingDeck, playerHand = newPlayerHand, isPlayingSplitHand = true)
                    } else {
                        currentState.copy(deck = remainingDeck, playerHand = newPlayerHand)
                    }
                emitEffect(GameEffect.PlayCardSound)
            }
            currentState.splitHand != null && currentState.isPlayingSplitHand -> {
                // Playing split hand
                val newSplitHand = currentState.splitHand.copy(cards = currentState.splitHand.cards + newCard)
                if (newSplitHand.isBust) {
                    // Split hand busts → go to dealer turn
                    _state.value =
                        currentState.copy(
                            deck = remainingDeck,
                            splitHand = newSplitHand,
                            status = GameStatus.DEALER_TURN
                        )
                    emitEffect(GameEffect.PlayCardSound)
                    scope.launch { mutex.withLock { runDealerTurn() } }
                } else {
                    _state.value = currentState.copy(deck = remainingDeck, splitHand = newSplitHand)
                    emitEffect(GameEffect.PlayCardSound)
                }
            }
            else -> {
                // Normal hit (no split)
                val newPlayerHand = currentState.playerHand.copy(cards = currentState.playerHand.cards + newCard)
                val newStatus = if (newPlayerHand.isBust) GameStatus.DEALER_WON else GameStatus.PLAYING
                _state.value = currentState.copy(deck = remainingDeck, playerHand = newPlayerHand, status = newStatus)
                emitEffect(GameEffect.PlayCardSound)
                if (newStatus == GameStatus.DEALER_WON) {
                    emitEffect(GameEffect.PlayLoseSound)
                    emitEffect(GameEffect.Vibrate)
                }
            }
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

    @Suppress("CyclomaticComplexMethod")
    private suspend fun runDealerTurn() {
        revealDealerHoleCard()
        delay(DEALER_TURN_DELAY_MS) // Visual pause for hole card reveal

        // Insurance payout: dealer natural BJ (2-card 21) pays 2:1 + returns stake = 3x
        val stateAfterReveal = _state.value
        val dealerHasNaturalBJ = stateAfterReveal.dealerHand.score == 21 && stateAfterReveal.dealerHand.cards.size == 2
        if (stateAfterReveal.insuranceBet > 0 && dealerHasNaturalBJ) {
            _state.value =
                stateAfterReveal.copy(
                    balance = stateAfterReveal.balance + stateAfterReveal.insuranceBet * 3
                )
        }

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

        val dealerScore = _state.value.dealerHand.score
        val dealerBust = _state.value.dealerHand.isBust
        val splitHand = _state.value.splitHand

        val finalStatus: GameStatus
        if (splitHand != null) {
            // Resolve each hand independently
            val primaryPayout = resolveHand(_state.value.playerHand, _state.value.currentBet, dealerScore, dealerBust)
            val splitPayout = resolveHand(splitHand, _state.value.splitBet, dealerScore, dealerBust)

            val primaryWins =
                !_state.value.playerHand.isBust && (dealerBust || _state.value.playerHand.score > dealerScore)
            val splitWins = !splitHand.isBust && (dealerBust || splitHand.score > dealerScore)
            val primaryPushes =
                !_state.value.playerHand.isBust && !dealerBust && _state.value.playerHand.score == dealerScore
            val splitPushes = !splitHand.isBust && !dealerBust && splitHand.score == dealerScore

            finalStatus =
                when {
                    primaryWins || splitWins -> GameStatus.PLAYER_WON
                    primaryPushes && splitPushes -> GameStatus.PUSH
                    else -> GameStatus.DEALER_WON
                }

            _state.value =
                _state.value.copy(
                    status = finalStatus,
                    balance = _state.value.balance + primaryPayout + splitPayout
                )
        } else {
            val playerScore = _state.value.playerHand.score
            finalStatus =
                when {
                    dealerBust -> GameStatus.PLAYER_WON
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
        }

        when (finalStatus) {
            GameStatus.PLAYER_WON -> emitEffect(GameEffect.PlayWinSound)
            GameStatus.DEALER_WON -> {
                emitEffect(GameEffect.PlayLoseSound)
                emitEffect(GameEffect.Vibrate)
            }
            else -> {}
        }
    }

    private fun resolveHand(
        hand: Hand,
        bet: Int,
        dealerScore: Int,
        dealerBust: Boolean
    ): Int =
        when {
            hand.isBust -> 0
            dealerBust -> bet * 2
            hand.score > dealerScore -> bet * 2
            hand.score == dealerScore -> bet
            else -> 0
        }

    private fun handleStand() {
        val currentState = _state.value
        if (currentState.status != GameStatus.PLAYING) return

        if (currentState.splitHand != null && !currentState.isPlayingSplitHand) {
            // Advance from primary hand to split hand
            _state.value = currentState.copy(isPlayingSplitHand = true)
            return
        }

        scope.launch {
            mutex.withLock {
                _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)
                runDealerTurn()
            }
        }
    }

    private fun handleSplit() {
        val state = _state.value
        if (state.status != GameStatus.PLAYING ||
            state.playerHand.cards.size != 2 ||
            state.playerHand.cards[0].rank != state.playerHand.cards[1].rank
        ) {
            return
        }
        if (state.balance < state.currentBet || state.splitHand != null || state.deck.size < 2) return

        val card1 = state.playerHand.cards[0]
        val card2 = state.playerHand.cards[1]
        val newPlayerHand = Hand(listOf(card1, state.deck[0]))
        val newSplitHand = Hand(listOf(card2, state.deck[1]))
        val isAceSplit = card1.rank == Rank.ACE

        _state.value =
            state.copy(
                deck = state.deck.drop(2),
                playerHand = newPlayerHand,
                splitHand = newSplitHand,
                balance = state.balance - state.currentBet,
                splitBet = state.currentBet,
                isPlayingSplitHand = false
            )
        emitEffect(GameEffect.PlayCardSound)

        if (isAceSplit) {
            // Aces get exactly one card each, then auto-stand both hands → dealer turn
            _state.value = _state.value.copy(isPlayingSplitHand = true)
            scope.launch {
                mutex.withLock {
                    _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)
                    runDealerTurn()
                }
            }
        }
    }

    @Suppress("ReturnCount")
    private fun handleDoubleDown() {
        val state = _state.value
        if (state.status != GameStatus.PLAYING) return
        if (state.playerHand.cards.size != 2) return
        if (state.balance < state.currentBet) return

        val drawnCard = state.deck.first()
        val remainingDeck = state.deck.drop(1)
        val newPlayerHand = Hand(state.playerHand.cards + drawnCard)
        val busted = newPlayerHand.isBust

        _state.value =
            state.copy(
                deck = remainingDeck,
                playerHand = newPlayerHand,
                balance = state.balance - state.currentBet,
                currentBet = state.currentBet * 2,
                status = if (busted) GameStatus.DEALER_WON else GameStatus.DEALER_TURN,
            )
        emitEffect(GameEffect.PlayCardSound)
        if (busted) {
            emitEffect(GameEffect.PlayLoseSound)
            emitEffect(GameEffect.Vibrate)
        } else {
            scope.launch {
                mutex.withLock {
                    runDealerTurn()
                }
            }
        }
    }

    private fun handleTakeInsurance() {
        val state = _state.value
        if (state.status != GameStatus.INSURANCE_OFFERED) return
        val insuranceBet = state.currentBet / 2
        _state.value =
            state.copy(
                balance = state.balance - insuranceBet,
                insuranceBet = insuranceBet,
                status = GameStatus.PLAYING,
            )
    }

    private fun handleDeclineInsurance() {
        val state = _state.value
        if (state.status != GameStatus.INSURANCE_OFFERED) return
        _state.value =
            state.copy(
                insuranceBet = 0,
                status = GameStatus.PLAYING,
            )
    }

    private fun emitEffect(effect: GameEffect) {
        _effects.tryEmit(effect)
    }
}
