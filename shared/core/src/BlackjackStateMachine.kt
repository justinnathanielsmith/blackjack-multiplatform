@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.utils.secureRandom
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BlackjackStateMachine(
    private val scope: CoroutineScope,
    initialState: GameState = GameState(status = GameStatus.BETTING, balance = 1000),
    private val isTest: Boolean = false,
    private val logger: Logger = Logger.withTag("BlackjackStateMachine")
) {
    companion object {
        private const val DEALER_TURN_DELAY_MS = 600L
        private const val DEAL_CARD_DELAY_MS = 400L
        private const val DEALER_CRITICAL_PRE_DELAY_MS = 900L
        private const val REVEAL_DELAY_MS = 1500L
        private const val SLOW_ROLL_DELAY_MS = 2500L
    }

    private val dealerTurnDelayMs: Long get() = if (isTest) 0L else DEALER_TURN_DELAY_MS
    private val dealCardDelayMs: Long get() = if (isTest) 0L else DEAL_CARD_DELAY_MS
    private val dealerCriticalPreDelayMs: Long get() = if (isTest) 0L else DEALER_CRITICAL_PRE_DELAY_MS

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 64)
    private val isShutdown = MutableStateFlow(false)
    val effects: Flow<GameEffect> =
        channelFlow {
            val collectJob = launch { _effects.collect { send(it) } }
            isShutdown.first { it }
            collectJob.cancelAndJoin()
        }

    private val actionChannel = Channel<GameAction>(Channel.BUFFERED)

    init {
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try {
                for (action in actionChannel) {
                    processAction(action)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                logger.d { "SM action loop cancelled" }
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                logger.e(e) { "SM init block caught fatal error" }
            } finally {
                logger.d { "SM init block finally" }
                isShutdown.value = true
            }
        }
    }

    private suspend fun processAction(action: GameAction) {
        logger.v { "SM received action: $action" }
        when (action) {
            is GameAction.NewGame -> handleNewGameAction(action)
            is GameAction.Surrender,
            is GameAction.Deal,
            is GameAction.Hit,
            is GameAction.Stand,
            is GameAction.DoubleDown,
            is GameAction.TakeInsurance,
            is GameAction.DeclineInsurance,
            is GameAction.Split -> handleGameFlowAction(action)
            is GameAction.UpdateRules,
            is GameAction.PlaceBet,
            is GameAction.ResetBet,
            is GameAction.ResetSeatBet,
            is GameAction.SelectHandCount,
            is GameAction.PlaceSideBet,
            is GameAction.ResetSideBets -> handleBettingAction(action)
        }
        logger.v { "SM action loop finished current item: $action" }
    }

    private suspend fun handleNewGameAction(action: GameAction.NewGame) {
        handleNewGame(
            action.initialBalance,
            action.rules,
            action.handCount,
            action.previousBets,
            action.lastSideBets
        )
    }

    private suspend fun handleGameFlowAction(action: GameAction) {
        when (action) {
            is GameAction.Surrender -> handleSurrender()
            is GameAction.Deal -> handleDeal()
            is GameAction.Hit -> handleHit()
            is GameAction.Stand -> handleStand()
            is GameAction.DoubleDown -> handleDoubleDown()
            is GameAction.TakeInsurance -> handleInsurance(true)
            is GameAction.DeclineInsurance -> handleInsurance(false)
            is GameAction.Split -> handleSplit()
            else -> {}
        }
    }

    private fun handleBettingAction(action: GameAction) {
        when (action) {
            is GameAction.UpdateRules -> handleUpdateRules(action.rules)
            is GameAction.PlaceBet -> handlePlaceBet(action.amount, action.seatIndex)
            is GameAction.ResetBet -> handleResetBet(null)
            is GameAction.ResetSeatBet -> handleResetBet(action.seatIndex)
            is GameAction.SelectHandCount -> handleSelectHandCount(action.count)
            is GameAction.PlaceSideBet -> handlePlaceSideBet(action.type, action.amount)
            is GameAction.ResetSideBets -> handlePlaceSideBet(null, 0)
            else -> {}
        }
    }

    fun dispatch(action: GameAction) {
        actionChannel.trySend(action).getOrThrow()
    }

    fun shutdown() {
        actionChannel.close()
    }

    private fun handlePlaceBet(
        amount: Int,
        seatIndex: Int,
    ) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        if (amount <= 0 || seatIndex !in 0 until current.handCount) {
            emitEffect(GameEffect.Vibrate)
            return
        }
        if (amount > current.balance) {
            emitEffect(GameEffect.Vibrate)
            return
        }
        val currentHand = current.playerHands[seatIndex]
        _state.value =
            current.copy(
                balance = current.balance - amount,
                playerHands = current.playerHands.set(seatIndex, currentHand.copy(bet = currentHand.bet + amount)),
            )
    }

    private fun handleResetBet(seatIndex: Int?) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        if (seatIndex == null) {
            val refund = current.playerHands.sumOf { it.bet }
            _state.value =
                current.copy(
                    balance = current.balance + refund,
                    playerHands = current.playerHands.map { it.copy(bet = 0) }.toPersistentList(),
                )
        } else {
            if (seatIndex !in 0 until current.handCount) return
            val currentHand = current.playerHands[seatIndex]
            val refund = currentHand.bet
            _state.value =
                current.copy(
                    balance = current.balance + refund,
                    playerHands = current.playerHands.set(seatIndex, currentHand.copy(bet = 0)),
                )
        }
    }

    private fun handleSelectHandCount(count: Int) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        if (count !in 1..3) {
            emitEffect(GameEffect.Vibrate)
            return
        }
        val delta = count - current.handCount
        if (delta == 0) return
        val newHands: kotlinx.collections.immutable.PersistentList<Hand>
        val balanceDelta: Int
        if (delta > 0) {
            newHands = current.playerHands.addAll(List(delta) { Hand() })
            balanceDelta = 0
        } else {
            val refund = (count until current.handCount).sumOf { i -> current.playerHands.getOrNull(i)?.bet ?: 0 }
            newHands = current.playerHands.subList(0, count).toPersistentList()
            balanceDelta = refund
        }
        _state.value =
            current.copy(
                handCount = count,
                playerHands = newHands,
                balance = current.balance + balanceDelta,
            )
    }

    private fun handleUpdateRules(rules: GameRules) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        _state.value = current.copy(rules = rules)
    }

    private suspend fun handleDeal() {
        val current = _state.value
        if (current.status != GameStatus.BETTING ||
            current.playerHands.size != current.handCount ||
            current.playerHands.any { it.bet <= 0 }
        ) {
            return
        }
        _state.value = current.copy(status = GameStatus.DEALING)
        val (playerHands, dealerHand) = dealCardsWithAnimation(current)
        applyInitialOutcome(current, playerHands, dealerHand)
    }

    private suspend fun dealCardsWithAnimation(
        current: GameState,
    ): Pair<kotlinx.collections.immutable.PersistentList<Hand>, Hand> {
        var deck = getDeck(current).toPersistentList()
        var playerHands = current.playerHands
        var dealerHand = Hand()
        _state.value =
            _state.value.copy(dealerHand = dealerHand, deck = deck)
        for (round in 0..1) {
            for (i in 0 until current.handCount) {
                delay(dealCardDelayMs)
                val card = deck.firstOrNull() ?: break
                deck = deck.removeAt(0)
                playerHands = playerHands.set(i, playerHands[i].copy(cards = playerHands[i].cards.add(card)))
                _state.value = _state.value.copy(playerHands = playerHands, deck = deck)
                emitEffect(GameEffect.PlayCardSound)
            }
            delay(dealCardDelayMs)
            val card = deck.firstOrNull() ?: break
            deck = deck.removeAt(0)
            val dealerCard = if (round == 1) card.copy(isFaceDown = true) else card
            dealerHand = Hand(dealerHand.cards.add(dealerCard))
            _state.value = _state.value.copy(dealerHand = dealerHand, deck = deck)
            emitEffect(GameEffect.PlayCardSound)
        }
        return Pair(playerHands, dealerHand)
    }

    private suspend fun applyInitialOutcome(
        current: GameState,
        playerHands: kotlinx.collections.immutable.PersistentList<Hand>,
        dealerHand: Hand,
    ) {
        val sideBetUpdate =
            SideBetLogic.resolveSideBets(
                sideBets = current.sideBets,
                playerHand = playerHands[0],
                dealerUpcard = dealerHand.cards[0],
            )

        val (initialStatus, finalDealerHand, balanceUpdate) =
            BlackjackRules.resolveInitialOutcomeValues(current, playerHands, dealerHand)

        if (initialStatus.isTerminal()) {
            if (finalDealerHand != dealerHand) {
                _state.value = _state.value.copy(dealerHand = finalDealerHand)
            }
            delay(getRevealDelayMs(dealerHand))
        }

        _state.value =
            _state.value.copy(
                status = initialStatus,
                dealerHand = finalDealerHand,
                balance = current.balance + balanceUpdate + sideBetUpdate.payoutTotal,
                sideBetResults = sideBetUpdate.results,
            )
        if (balanceUpdate > 0) emitEffect(GameEffect.ChipEruption(balanceUpdate))
        sideBetUpdate.results.forEach { (type, result) ->
            if (result.payoutAmount > 0) emitEffect(GameEffect.ChipEruption(result.payoutAmount, type))
        }
        if (initialStatus == GameStatus.PLAYER_WON || sideBetUpdate.payoutTotal > 0) {
            emitEffect(GameEffect.PlayWinSound)
            if (initialStatus == GameStatus.PLAYER_WON) emitEffect(GameEffect.WinPulse)
        } else if (initialStatus == GameStatus.DEALER_WON) {
            emitEffect(GameEffect.PlayLoseSound)
            emitEffect(GameEffect.ChipLoss(current.currentBet))
        }
    }

    private fun getDeck(current: GameState): List<Card> {
        val totalCards = current.rules.deckCount * BlackjackRules.CARDS_PER_DECK
        val threshold = totalCards / BlackjackRules.RESHUFFLE_THRESHOLD_DIVISOR

        // Always reshuffle if empty.
        if (current.deck.isEmpty()) {
            return BlackjackRules.createDeck(current.rules, secureRandom)
        }

        // In tests, respect custom small decks unless they are empty.
        if (isTest) return current.deck

        // Reshuffle if less than 25% of the shoe remains.
        return if (current.deck.size < threshold) {
            BlackjackRules.createDeck(current.rules, secureRandom)
        } else {
            current.deck
        }
    }

    private fun handleNewGame(
        initialBalance: Int? = null,
        rules: GameRules = GameRules(),
        handCount: Int = 1,
        previousBets: kotlinx.collections.immutable.PersistentList<Int> = persistentListOf(0),
        lastSideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    ) {
        val currentState = _state.value
        val newBalance = initialBalance ?: currentState.balance

        // Normalize lastBets list length if it doesn't match current seat selection
        // (but usually they should match if coming from round end)
        val normalizedLastBets =
            if (previousBets.size >= handCount) {
                previousBets.subList(0, handCount).toPersistentList()
            } else {
                previousBets.toMutableList().apply { repeat(handCount - previousBets.size) { add(0) } }.toPersistentList()
            }

        val totalLastBet = normalizedLastBets.sumOf { it }
        val allAffordable = totalLastBet <= newBalance

        val finalBets: kotlinx.collections.immutable.PersistentList<Int>
        val afterMainBetBalance: Int

        if (allAffordable) {
            finalBets = normalizedLastBets
            afterMainBetBalance = newBalance - totalLastBet
        } else {
            // If they can't afford exactly the previous bets, they start at zero to be safe
            finalBets = List(handCount) { 0 }.toPersistentList()
            afterMainBetBalance = newBalance
        }

        var totalSideBetCost = 0
        for ((_, betAmount) in lastSideBets) {
            totalSideBetCost += betAmount
        }

        val finalSideBets: PersistentMap<SideBetType, Int>
        val postSideBetBalance: Int

        if (totalSideBetCost <= afterMainBetBalance) {
            finalSideBets = lastSideBets
            postSideBetBalance = afterMainBetBalance - totalSideBetCost
        } else {
            finalSideBets = persistentMapOf()
            postSideBetBalance = afterMainBetBalance
        }

        _state.value =
            GameState(
                status = GameStatus.BETTING,
                balance = postSideBetBalance,
                sideBets = finalSideBets,
                lastSideBets = lastSideBets,
                playerHands = List(handCount) { i ->
                    Hand(bet = finalBets[i], lastBet = normalizedLastBets[i])
                }.toPersistentList(),
                activeHandIndex = 0,
                handCount = handCount,
                rules = rules,
            )
    }

    private suspend fun handleSurrender() {
        val state = _state.value
        if (state.status != GameStatus.PLAYING ||
            state.activeHand.cards.size != 2 ||
            !state.rules.allowSurrender
        ) {
            return
        }

        val refund = state.activeBet / 2
        val surrenderedHand = state.activeHand.copy(isSurrendered = true)
        val updatedHands = state.playerHands.set(state.activeHandIndex, surrenderedHand)
        _state.value =
            state.copy(
                balance = state.balance + refund,
                playerHands = updatedHands,
            )
        emitEffect(GameEffect.PlayLoseSound)
        emitEffect(GameEffect.ChipLoss(state.activeBet - refund))
        advanceOrEndTurn(_state.value)
    }

    private suspend fun handleHit() {
        val outcome = PlayerActionLogic.hit(_state.value)
        if (outcome == PlayerActionOutcome.noop(_state.value)) return
        _state.value = outcome.state
        outcome.effects.forEach(::emitEffect)
        if (outcome.shouldAdvanceTurn) advanceOrEndTurn(outcome.state)
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
        if (outcome.shouldAdvanceTurn) advanceOrEndTurn(outcome.state)
    }

    private suspend fun handleSplit() {
        val outcome = PlayerActionLogic.split(_state.value)
        if (outcome == PlayerActionOutcome.noop(_state.value)) return
        _state.value = outcome.state
        outcome.effects.forEach(::emitEffect)
        if (outcome.shouldAdvanceTurn) advanceOrEndTurn(outcome.state)
    }

    private suspend fun handleDoubleDown() {
        val outcome = PlayerActionLogic.doubleDown(_state.value)
        if (outcome == PlayerActionOutcome.noop(_state.value)) return
        _state.value = outcome.state
        outcome.effects.forEach(::emitEffect)
        if (outcome.shouldAdvanceTurn) advanceOrEndTurn(outcome.state)
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
        delay(getRevealDelayMs(_state.value.dealerHand))

        handleInsurancePayout()

        var currentDealerHand = _state.value.dealerHand
        var currentDeck = _state.value.deck
        val rules = _state.value.rules

        while (BlackjackRules.shouldDealerDraw(currentDealerHand, rules)) {
            val isCritical =
                currentDealerHand.score in
                    BlackjackRules.DEALER_STIFF_MIN until BlackjackRules.DEALER_STAND_THRESHOLD &&
                    !currentDealerHand.isSoft

            if (isCritical) {
                _state.value = _state.value.copy(dealerDrawIsCritical = true)
                emitEffect(GameEffect.DealerCriticalDraw)
                delay(dealerCriticalPreDelayMs)
            }

            val newCard = currentDeck.firstOrNull() ?: break
            currentDealerHand = currentDealerHand.copy(cards = currentDealerHand.cards.add(newCard))
            currentDeck = currentDeck.drop(1).toPersistentList()

            _state.value =
                _state.value.copy(
                    deck = currentDeck,
                    dealerHand = currentDealerHand,
                    dealerDrawIsCritical = isCritical,
                )
            emitEffect(GameEffect.PlayCardSound)
            delay(dealerTurnDelayMs)

            if (isCritical) {
                _state.value = _state.value.copy(dealerDrawIsCritical = false)
            }
        }
        finalizeGame()
    }

    private fun finalizeGame() {
        val state = _state.value
        val dealerScore = state.dealerHand.score
        val dealerBust = state.dealerHand.isBust
        val results = BlackjackRules.calculateHandResults(state, dealerScore, dealerBust)
        val totalPayout = results.totalPayout
        val anyWin = results.anyWin
        val allPush = results.allPush

        val finalStatus =
            when {
                anyWin -> GameStatus.PLAYER_WON
                allPush -> GameStatus.PUSH
                else -> GameStatus.DEALER_WON
            }

        _state.value = state.copy(status = finalStatus, balance = state.balance + totalPayout)

        if (totalPayout > 0) emitEffect(GameEffect.ChipEruption(totalPayout))
        val totalBet = state.playerHands.fold(0) { acc, h -> acc + h.bet }
        if (totalPayout < totalBet) emitEffect(GameEffect.ChipLoss(totalBet - totalPayout))

        when (finalStatus) {
            GameStatus.PLAYER_WON -> {
                emitEffect(GameEffect.PlayWinSound)
                emitEffect(GameEffect.WinPulse)
            }
            GameStatus.DEALER_WON -> {
                emitEffect(GameEffect.PlayLoseSound)
                if (state.playerHands.none { it.isBust }) emitEffect(GameEffect.Vibrate)
            }
            else -> {}
        }
    }

    private fun handleInsurancePayout() {
        val current = _state.value
        val dealerHasNaturalBJ =
            current.dealerHand.score == BlackjackRules.BLACKJACK_SCORE &&
                current.dealerHand.cards.size == 2
        if (current.insuranceBet > 0 && dealerHasNaturalBJ) {
            _state.value = current.copy(balance = current.balance + current.insuranceBet * 3)
        }
    }

    private suspend fun handleInsurance(take: Boolean) {
        val state = _state.value
        if (state.status != GameStatus.INSURANCE_OFFERED) return

        if (take) {
            val insuranceBet = state.currentBet / 2
            if (insuranceBet > state.balance) return
            _state.value =
                state.copy(
                    balance = state.balance - insuranceBet,
                    insuranceBet = insuranceBet,
                    status = GameStatus.PLAYING,
                )
        } else {
            _state.value = state.copy(insuranceBet = 0, status = GameStatus.PLAYING)
        }

        if (_state.value.dealerHand.score == BlackjackRules.BLACKJACK_SCORE) {
            runDealerTurn()
        }
    }

    private fun handlePlaceSideBet(
        type: SideBetType?,
        amount: Int
    ) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        if (type == null) {
            var totalRefund = 0
            for ((_, betAmount) in current.sideBets) {
                totalRefund += betAmount
            }
            _state.value =
                current.copy(
                    balance = current.balance + totalRefund,
                    sideBets = persistentMapOf(),
                )
        } else if (amount > 0 && amount <= current.balance) {
            val newSideBets = current.sideBets.put(type, (current.sideBets[type] ?: 0) + amount)
            _state.value =
                current.copy(
                    balance = current.balance - amount,
                    sideBets = newSideBets,
                )
        }
    }

    private fun emitEffect(effect: GameEffect) {
        val emitted = _effects.tryEmit(effect)
        if (!emitted) logger.w { "Effect dropped (buffer full): $effect" }
    }

    private fun getRevealDelayMs(hand: Hand): Long {
        if (isTest) return 0L
        val isSlowRoll =
            hand.cards.size >= 2 &&
                hand.score == 21 &&
                (hand.cards[0].rank == Rank.ACE || hand.cards[0].rank.value == 10)
        return if (isSlowRoll) SLOW_ROLL_DELAY_MS else REVEAL_DELAY_MS
    }
}
