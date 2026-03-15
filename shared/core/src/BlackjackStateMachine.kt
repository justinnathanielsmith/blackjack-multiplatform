package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
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
        private const val DEAL_CARD_DELAY_MS = 400L
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
                    is GameAction.NewGame ->
                        handleNewGame(
                            action.initialBalance,
                            action.rules,
                            action.handCount,
                            action.lastBet
                        )
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
                    is GameAction.PlaceSideBet -> handlePlaceSideBet(action.type, action.amount)
                    is GameAction.ResetSideBets -> handleResetSideBets()
                }
            }
        }
    }

    private fun handlePlaceBet(amount: Int) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        val totalCost = amount * current.handCount
        if (amount <= 0 || totalCost > current.balance) return
        _state.value =
            current.copy(
                balance = current.balance - totalCost,
                currentBet = current.currentBet + amount
            )
    }

    private fun handleResetBet() {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        _state.value =
            current.copy(
                balance = current.balance + current.currentBet * current.handCount,
                currentBet = 0
            )
    }

    private fun handleSelectHandCount(count: Int) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        if (count !in 1..3) return
        val delta = count - current.handCount
        if (delta == 0) return
        val balanceAdjustment = current.currentBet * delta
        if (balanceAdjustment > current.balance) return
        _state.value =
            current.copy(
                handCount = count,
                balance = current.balance - balanceAdjustment
            )
    }

    private suspend fun handleDeal() {
        val current = _state.value
        if (current.status != GameStatus.BETTING || current.currentBet <= 0) return

        // Set status to DEALING to block interactions and show "Dealing..."
        _state.value = current.copy(status = GameStatus.DEALING)

        val fullDeck = getInitialDeck(current)
        var deck = fullDeck.toPersistentList()

        // Initialize empty hands
        var playerHands = List(current.handCount) { Hand() }.toPersistentList()
        var dealerHand = Hand()
        val bets = List(current.handCount) { current.currentBet }.toPersistentList()

        _state.value = _state.value.copy(
            playerHands = playerHands,
            dealerHand = dealerHand,
            playerBets = bets,
            deck = deck
        )

        // Round-robin deal: 2 cards each
        for (round in 0..1) {
            // Player hands
            for (i in 0 until current.handCount) {
                delay(DEAL_CARD_DELAY_MS)
                val card = deck[0]
                deck = deck.removeAt(0)
                playerHands = playerHands.set(i, Hand(playerHands[i].cards.add(card)))
                _state.value = _state.value.copy(
                    playerHands = playerHands,
                    deck = deck
                )
                emitEffect(GameEffect.PlayCardSound)
            }

            // Dealer hand
            delay(DEAL_CARD_DELAY_MS)
            val card = deck[0]
            deck = deck.removeAt(0)
            val dealerCard = if (round == 1) card.copy(isFaceDown = true) else card
            dealerHand = Hand(dealerHand.cards.add(dealerCard))
            _state.value = _state.value.copy(
                dealerHand = dealerHand,
                deck = deck
            )
            emitEffect(GameEffect.PlayCardSound)
        }

        delay(DEAL_CARD_DELAY_MS)

        // Evaluate Side Bets
        val sideBetUpdate = SideBetLogic.resolveSideBets(
            sideBets = current.sideBets,
            playerHand = playerHands[0],
            dealerUpcard = dealerHand.cards[0]
        )

        // Determine Initial Status (Blackjack etc.)
        val dealerHandRevealed = Hand(dealerHand.cards.map { it.copy(isFaceDown = false) }.toPersistentList())
        val initialStatus = determineInitialStatus(playerHands, dealerHandRevealed, current.handCount)

        val finalDealerHand = when (initialStatus) {
            GameStatus.PUSH, GameStatus.DEALER_WON -> dealerHandRevealed
            else -> dealerHand
        }

        val balanceUpdate = when (initialStatus) {
            GameStatus.PLAYER_WON ->
                (current.currentBet * current.rules.blackjackPayout.numerator) /
                        current.rules.blackjackPayout.denominator + current.currentBet
            GameStatus.PUSH -> current.currentBet
            else -> 0
        }

        _state.value = _state.value.copy(
            status = initialStatus,
            dealerHand = finalDealerHand,
            balance = current.balance + balanceUpdate + sideBetUpdate.payoutTotal,
            sideBetResults = sideBetUpdate.results
        )

        if (initialStatus == GameStatus.PLAYER_WON || sideBetUpdate.payoutTotal > 0) {
            emitEffect(GameEffect.PlayWinSound)
        }

        if (initialStatus == GameStatus.PLAYING && dealerHand.cards[0].rank == Rank.ACE) {
            _state.value = _state.value.copy(status = GameStatus.INSURANCE_OFFERED)
        }
    }

    private fun determineInitialStatus(
        hands: List<Hand>,
        dealerHand: Hand,
        handCount: Int
    ): GameStatus {
        val dealerHasBJ = dealerHand.score == 21 && dealerHand.cards.size == 2

        if (handCount == 1) {
            val playerHasBJ = hands[0].score == 21 && hands[0].cards.size == 2
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

    private fun getInitialDeck(current: GameState): List<Card> {
        return current.deck.ifEmpty {
            (1..current.rules.deckCount)
                .flatMap {
                    Suit.entries.flatMap { suit ->
                        Rank.entries.map { rank -> Card(rank, suit) }
                    }
                }.shuffled()
        }
    }

    private fun handleNewGame(
        initialBalance: Int? = null,
        rules: GameRules = GameRules(),
        handCount: Int = 1,
        lastBet: Int = 0,
    ) {
        val currentState = _state.value
        val newBalance = initialBalance ?: currentState.balance
        val maxAffordableBet = if (handCount > 0) newBalance / handCount else newBalance
        val clampedBet = lastBet.coerceIn(0, maxAffordableBet)
        _state.value =
            GameState(
                status = GameStatus.BETTING,
                balance = newBalance - clampedBet * handCount,
                currentBet = clampedBet,
                playerHands = persistentListOf(Hand()),
                playerBets = persistentListOf(0),
                activeHandIndex = 0,
                handCount = handCount,
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

    private suspend fun handleHit() {
        val outcome = PlayerActionLogic.hit(_state.value)
        if (outcome == PlayerActionOutcome.noop(_state.value)) return
        _state.value = outcome.state
        outcome.effects.forEach(::emitEffect)
        if (outcome.shouldAdvanceTurn) {
            advanceOrEndTurn(outcome.state)
        }
    }

    private suspend fun advanceOrEndTurn(state: GameState) {
        if (state.activeHandIndex < state.playerHands.size - 1) {
            _state.value = state.copy(activeHandIndex = state.activeHandIndex + 1)
        } else {
            _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)
            runDealerTurn()
        }
    }

    private suspend fun handleStand() {
        val outcome = PlayerActionLogic.stand(_state.value)
        if (outcome == PlayerActionOutcome.noop(_state.value)) return
        _state.value = outcome.state
        outcome.effects.forEach(::emitEffect)
        if (outcome.shouldAdvanceTurn) {
            advanceOrEndTurn(outcome.state)
        }
    }

    private suspend fun handleSplit() {
        val outcome = PlayerActionLogic.split(_state.value)
        if (outcome == PlayerActionOutcome.noop(_state.value)) return
        _state.value = outcome.state
        outcome.effects.forEach(::emitEffect)
        if (outcome.shouldAdvanceTurn) {
            advanceOrEndTurn(outcome.state)
        }
    }

    private suspend fun handleDoubleDown() {
        val outcome = PlayerActionLogic.doubleDown(_state.value)
        if (outcome == PlayerActionOutcome.noop(_state.value)) return
        _state.value = outcome.state
        outcome.effects.forEach(::emitEffect)
        if (outcome.shouldAdvanceTurn) {
            advanceOrEndTurn(outcome.state)
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

    private fun shouldDealerDraw(
        hand: Hand,
        rules: GameRules
    ): Boolean {
        if (hand.score < DEALER_STAND_THRESHOLD) return true
        if (hand.score == DEALER_STAND_THRESHOLD && rules.dealerHitsSoft17 && hand.isSoft) return true
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
            val payout = resolveHand(hand, bet, dealerScore, dealerBust, state.rules)
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
        rules: GameRules,
    ): Int {
        if (hand.isBust) return 0

        val isNaturalBJ = hand.cards.size == 2 && hand.score == 21 && !hand.wasSplit

        return when {
            isNaturalBJ && dealerScore != 21 -> {
                bet + (bet * rules.blackjackPayout.numerator) / rules.blackjackPayout.denominator
            }
            dealerBust || hand.score > dealerScore -> bet * 2
            hand.score == dealerScore -> bet
            else -> 0
        }
    }

    private suspend fun handleTakeInsurance() {
        val state = _state.value
        if (state.status != GameStatus.INSURANCE_OFFERED) return
        val insuranceBet = state.currentBet / 2
        if (insuranceBet > state.balance) return

        _state.value = state.copy(
            balance = state.balance - insuranceBet,
            insuranceBet = insuranceBet,
            status = GameStatus.PLAYING,
        )

        if (_state.value.dealerHand.score == 21) {
            runDealerTurn()
        }
    }

    private suspend fun handleDeclineInsurance() {
        val state = _state.value
        if (state.status != GameStatus.INSURANCE_OFFERED) return
        _state.value = state.copy(
            insuranceBet = 0,
            status = GameStatus.PLAYING,
        )

        if (_state.value.dealerHand.score == 21) {
            runDealerTurn()
        }
    }

    private fun handlePlaceSideBet(
        type: SideBetType,
        amount: Int
    ) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        if (amount <= 0 || amount > current.balance) return
        val newSideBets = current.sideBets.put(type, (current.sideBets[type] ?: 0) + amount)

        _state.value =
            current.copy(
                balance = current.balance - amount,
                sideBets = newSideBets
            )
    }

    private fun handleResetSideBets() {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        val totalRefund = current.sideBets.values.sum()
        _state.value =
            current.copy(
                balance = current.balance + totalRefund,
                sideBets = persistentMapOf()
            )
    }

    private fun emitEffect(effect: GameEffect) {
        _effects.tryEmit(effect)
    }
}
