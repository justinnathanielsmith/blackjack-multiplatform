package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
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
                    is GameAction.NewGame -> handleNewGame(action.initialBalance, action.rules)
                    is GameAction.Surrender -> handleSurrender()
                    is GameAction.PlaceBet -> handlePlaceBet(action.amount)
                    is GameAction.ResetBet -> handleResetBet()
                    is GameAction.Deal -> handleDeal()
                    is GameAction.Hit -> handleHit()
                    is GameAction.Stand -> handleStand()
                    is GameAction.DoubleDown -> handleDoubleDown()
                    is GameAction.TakeInsurance -> handleTakeInsurance()
                    is GameAction.DeclineInsurance -> handleDeclineInsurance()
                    is GameAction.Split -> handleSplit()
                    is GameAction.SelectHandCount -> handleSelectHandCount(action.count)
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

    private fun handleSelectHandCount(count: Int) {
        if (_state.value.status != GameStatus.BETTING) return
        if (count !in 1..3) return
        _state.value = _state.value.copy(handCount = count)
    }

    private fun handleDeal() {
        val current = _state.value
        if (current.status != GameStatus.BETTING || current.currentBet <= 0) return

        val extraCost = current.currentBet * (current.handCount - 1)
        if (current.balance < extraCost) return

        val fullDeck =
            if (current.deck.isNotEmpty()) {
                current.deck
            } else {
                (1..current.rules.deckCount)
                    .flatMap {
                        Suit.entries.flatMap { suit ->
                            Rank.entries.map { rank -> Card(rank, suit) }
                        }
                    }
                    .shuffled()
            }

        // Round-robin deal: hand i gets deck[i] and deck[i + handCount]
        val hands =
            List(current.handCount) { i ->
                Hand(persistentListOf(fullDeck[i], fullDeck[i + current.handCount]))
            }.toPersistentList()
        val deckOffset = current.handCount * 2
        val dealerCards = fullDeck.drop(deckOffset).take(2)
        val dealerHandHidden = Hand(persistentListOf(dealerCards[0], dealerCards[1].copy(isFaceDown = true)))
        val remainingDeck = fullDeck.drop(deckOffset + 2).toPersistentList()

        val newBalance = current.balance - extraCost
        val bets = List(current.handCount) { current.currentBet }.toPersistentList()

        val initialStatus = determineInitialStatus(hands, dealerHandHidden, current.handCount)

        val dealerHand =
            when (initialStatus) {
                GameStatus.PUSH, GameStatus.DEALER_WON ->
                    Hand(dealerHandHidden.cards.map { it.copy(isFaceDown = false) }.toPersistentList())
                else -> dealerHandHidden
            }

        val balanceUpdate =
            when (initialStatus) {
                GameStatus.PLAYER_WON -> (current.currentBet * current.rules.blackjackPayout.numerator) / current.rules.blackjackPayout.denominator + current.currentBet
                GameStatus.PUSH -> current.currentBet
                else -> 0
            }

        _state.value =
            GameState(
                deck = remainingDeck,
                playerHands = hands,
                playerBets = bets,
                activeHandIndex = 0,
                handCount = current.handCount,
                dealerHand = dealerHand,
                status = initialStatus,
                balance = newBalance + balanceUpdate,
                currentBet = current.currentBet,
                rules = current.rules,
            )
        emitEffect(GameEffect.PlayCardSound)
        if (initialStatus == GameStatus.PLAYER_WON) {
            emitEffect(GameEffect.PlayWinSound)
        }
        if (initialStatus == GameStatus.PLAYING && dealerCards[0].rank == Rank.ACE) {
            _state.value = _state.value.copy(status = GameStatus.INSURANCE_OFFERED)
        }
    }

    private fun determineInitialStatus(
        hands: List<Hand>,
        dealerHand: Hand,
        handCount: Int
    ): GameStatus {
        if (handCount != 1) return GameStatus.PLAYING
        return when {
            hands[0].score == 21 && dealerHand.score == 21 -> GameStatus.PUSH
            hands[0].score == 21 -> GameStatus.PLAYER_WON
            dealerHand.score == 21 -> GameStatus.DEALER_WON
            else -> GameStatus.PLAYING
        }
    }

    private fun handleNewGame(initialBalance: Int? = null, rules: GameRules = GameRules()) {
        val currentState = _state.value
        _state.value =
            GameState(
                status = GameStatus.BETTING,
                balance = initialBalance ?: currentState.balance,
                currentBet = 0,
                playerHands = persistentListOf(Hand()),
                playerBets = persistentListOf(0),
                activeHandIndex = 0,
                handCount = 1,
                rules = rules,
            )
    }

    private fun handleSurrender() {
        val state = _state.value
        if (state.status != GameStatus.PLAYING) return
        if (state.activeHand.cards.size != 2) return
        if (!state.rules.allowSurrender) return

        val refund = state.activeBet / 2
        _state.value =
            state.copy(
                balance = state.balance + refund,
                status = GameStatus.DEALER_WON,
            )
        emitEffect(GameEffect.PlayLoseSound)
    }

    private fun handleHit() {
        val state = _state.value
        if (state.status != GameStatus.PLAYING) return

        // Block hits on split aces
        val isAceSplit =
            state.playerHands.size > 1 &&
                state.activeHand.cards
                    .firstOrNull()
                    ?.rank == Rank.ACE
        if (isAceSplit && state.activeHand.cards.size >= 2) return

        val newCard = state.deck.first()
        val remainingDeck = state.deck.drop(1).toPersistentList()
        val newHand = state.activeHand.copy(cards = state.activeHand.cards.add(newCard))
        val updatedHands =
            state.playerHands.toPersistentList().set(state.activeHandIndex, newHand)

        if (newHand.isBust) {
            val newState = state.copy(deck = remainingDeck, playerHands = updatedHands)
            _state.value = newState
            emitEffect(GameEffect.PlayCardSound)
            advanceOrEndTurn(newState)
        } else {
            _state.value = state.copy(deck = remainingDeck, playerHands = updatedHands)
            emitEffect(GameEffect.PlayCardSound)
        }
    }

    private fun advanceOrEndTurn(state: GameState) {
        if (state.activeHandIndex < state.playerHands.size - 1) {
            _state.value = state.copy(activeHandIndex = state.activeHandIndex + 1)
        } else {
            scope.launch {
                mutex.withLock {
                    _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)
                    runDealerTurn()
                }
            }
        }
    }

    private fun handleStand() {
        val state = _state.value
        if (state.status != GameStatus.PLAYING) return
        advanceOrEndTurn(state)
    }

    private fun handleSplit() {
        val state = _state.value
        if (state.status != GameStatus.PLAYING || !state.canSplit()) return
        if (state.deck.size < 2) return

        val card1 = state.activeHand.cards[0]
        val card2 = state.activeHand.cards[1]
        val newPrimaryHand = Hand(persistentListOf(card1, state.deck[0]))
        val newSplitHand = Hand(persistentListOf(card2, state.deck[1]))
        val isAceSplit = card1.rank == Rank.ACE

        val updatedHands =
            state.playerHands
                .toPersistentList()
                .set(state.activeHandIndex, newPrimaryHand)
                .add(state.activeHandIndex + 1, newSplitHand)
        val updatedBets =
            state.playerBets
                .toPersistentList()
                .add(state.activeHandIndex + 1, state.currentBet)

        val newState =
            state.copy(
                deck = state.deck.drop(2).toPersistentList(),
                playerHands = updatedHands,
                playerBets = updatedBets,
                balance = state.balance - state.currentBet,
            )
        _state.value = newState
        emitEffect(GameEffect.PlayCardSound)
        emitEffect(GameEffect.PlayCardSound)

        if (isAceSplit) {
            // Ace split: both ace hands receive exactly one card and can't be hit → dealer turn
            scope.launch {
                mutex.withLock {
                    _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)
                    runDealerTurn()
                }
            }
        }
    }

    private fun handleDoubleDown() {
        val state = _state.value
        if (state.status != GameStatus.PLAYING) return
        if (!state.canDoubleDown()) return

        val drawnCard = state.deck.first()
        val remainingDeck = state.deck.drop(1).toPersistentList()
        val newHand = state.activeHand.copy(cards = state.activeHand.cards.add(drawnCard))
        val updatedHands =
            state.playerHands.toPersistentList().set(state.activeHandIndex, newHand)

        // Double the bet for this hand; deduct the extra (original activeBet) from balance
        val newBets =
            state.playerBets.toPersistentList().set(state.activeHandIndex, state.activeBet * 2)
        val newBalance = state.balance - state.activeBet

        val newState =
            state.copy(
                deck = remainingDeck,
                playerHands = updatedHands,
                playerBets = newBets,
                balance = newBalance,
            )
        _state.value = newState
        emitEffect(GameEffect.PlayCardSound)

        if (newHand.isBust) {
            emitEffect(GameEffect.PlayLoseSound)
            emitEffect(GameEffect.Vibrate)
        }
        advanceOrEndTurn(newState)
    }

    private fun revealDealerHoleCard() {
        _state.value =
            _state.value.copy(
                dealerHand =
                    _state.value.dealerHand.copy(
                        cards =
                            _state.value.dealerHand.cards
                                .map { it.copy(isFaceDown = false) }
                                .toPersistentList()
                    )
            )
    }

    private suspend fun runDealerTurn() {
        if (_state.value.status != GameStatus.DEALER_TURN) {
            _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)
        }
        revealDealerHoleCard()
        delay(DEALER_TURN_DELAY_MS) // Visual pause for hole card reveal

        handleInsurancePayout()

        var currentDealerHand = _state.value.dealerHand
        var currentDeck = _state.value.deck
        val rules = _state.value.rules

        while (shouldDealerDraw(currentDealerHand, rules)) {
            val newCard = currentDeck.firstOrNull() ?: break
            currentDealerHand = currentDealerHand.copy(cards = currentDealerHand.cards.add(newCard))
            currentDeck = currentDeck.drop(1).toPersistentList()

            _state.value =
                _state.value.copy(
                    deck = currentDeck,
                    dealerHand = currentDealerHand
                )
            emitEffect(GameEffect.PlayCardSound)
            delay(DEALER_TURN_DELAY_MS) // Visual pacing for dealer hits
        }

        finalizeGame()
    }

    private fun shouldDealerDraw(hand: Hand, rules: GameRules): Boolean {
        if (hand.score < 17) return true
        if (hand.score == 17 && rules.dealerHitsSoft17 && hand.isSoft) return true
        return false
    }

    private fun finalizeGame() {
        val state = _state.value
        val dealerScore = state.dealerHand.score
        val dealerBust = state.dealerHand.isBust

        var totalPayout = 0
        var anyWin = false
        var allPush = true

        for (i in state.playerHands.indices) {
            val hand = state.playerHands[i]
            val bet = state.playerBets[i]
            val payout = resolveHand(hand, bet, dealerScore, dealerBust)
            totalPayout += payout

            val handWins = !hand.isBust && (dealerBust || hand.score > dealerScore)
            val handPushes = !hand.isBust && !dealerBust && hand.score == dealerScore
            if (handWins) anyWin = true
            if (!handPushes) allPush = false
        }

        val finalStatus =
            when {
                anyWin -> GameStatus.PLAYER_WON
                allPush -> GameStatus.PUSH
                else -> GameStatus.DEALER_WON
            }

        _state.value =
            state.copy(
                status = finalStatus,
                balance = state.balance + totalPayout,
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

    private fun handleInsurancePayout() {
        val current = _state.value
        val dealerHasNaturalBJ =
            current.dealerHand.score == 21 && current.dealerHand.cards.size == 2
        if (current.insuranceBet > 0 && dealerHasNaturalBJ) {
            _state.value =
                current.copy(
                    balance = current.balance + current.insuranceBet * 3
                )
        }
    }

    private fun resolveHand(
        hand: Hand,
        bet: Int,
        dealerScore: Int,
        dealerBust: Boolean,
    ): Int =
        when {
            hand.isBust -> 0
            dealerBust -> bet * 2
            hand.score > dealerScore -> bet * 2
            hand.score == dealerScore -> bet
            else -> 0
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
