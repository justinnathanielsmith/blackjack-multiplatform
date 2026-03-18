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
    initialState: GameState = GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0),
    private val isTest: Boolean = true,
    private val logger: Logger = Logger.withTag("BlackjackStateMachine")
) {
    companion object {
        private const val BLACKJACK_SCORE = 21
        private const val DEALER_STAND_THRESHOLD = 17
        private const val DEALER_STIFF_MIN = 12
        private const val CARDS_PER_DECK = 52
        private const val DEALER_TURN_DELAY_MS = 600L
        private const val DEAL_CARD_DELAY_MS = 400L
        private const val DEALER_CRITICAL_PRE_DELAY_MS = 900L
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

    private val actionChannel = Channel<GameAction>(Channel.UNLIMITED)

    init {
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            logger.d { "SM init block launched on ${Thread.currentThread().name}" }
            try {
                for (action in actionChannel) {
                    logger.v { "SM received action: $action" }
                    when (action) {
                        is GameAction.NewGame ->
                            handleNewGame(
                                action.initialBalance,
                                action.rules,
                                action.handCount,
                                action.lastBet,
                                action.lastSideBets
                            )
                        is GameAction.Surrender -> handleSurrender()
                        is GameAction.Deal -> {
                            logger.d { "SM handling Deal action" }
                            handleDeal()
                        }
                        is GameAction.Hit -> handleHit()
                        is GameAction.Stand -> handleStand()
                        is GameAction.DoubleDown -> handleDoubleDown()
                        is GameAction.TakeInsurance -> handleInsurance(true)
                        is GameAction.DeclineInsurance -> handleInsurance(false)
                        is GameAction.Split -> {
                            logger.d { "SM handling Split action" }
                            handleSplit()
                        }
                        is GameAction.UpdateRules -> handleUpdateRules(action.rules)
                        is GameAction.PlaceBet -> handlePlaceBet(action.amount)
                        is GameAction.ResetBet -> handlePlaceBet(null)
                        is GameAction.SelectHandCount -> handleSelectHandCount(action.count)
                        is GameAction.PlaceSideBet -> handlePlaceSideBet(action.type, action.amount)
                        is GameAction.ResetSideBets -> handlePlaceSideBet(null, 0)
                    }
                    logger.v { "SM action loop finished current item: $action" }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                logger.d { "SM action loop cancelled" }
                throw e
            } catch (e: Exception) {
                logger.e(e) { "SM init block caught fatal error" }
            } finally {

                logger.d { "SM init block finally" }
                isShutdown.value = true
            }
        }
    }

    fun dispatch(action: GameAction) {
        actionChannel.trySend(action).getOrThrow()
    }

    fun shutdown() {
        actionChannel.close()
    }

    private fun handlePlaceBet(amount: Int?) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        if (amount == null) {
            _state.value =
                current.copy(
                    balance = current.balance + current.currentBet * current.handCount,
                    currentBet = 0,
                )
        } else {
            val totalCost = amount * current.handCount
            if (amount <= 0 || totalCost > current.balance) return
            _state.value =
                current.copy(
                    balance = current.balance - totalCost,
                    currentBet = current.currentBet + amount,
                )
        }
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

    private fun handleUpdateRules(rules: GameRules) {
        val current = _state.value
        if (current.status != GameStatus.BETTING) return
        _state.value = current.copy(rules = rules)
    }

    private suspend fun handleDeal() {
        logger.d { "handleDeal started" }
        val current = _state.value
        if (current.status != GameStatus.BETTING || current.currentBet <= 0) {
            logger.d { "handleDeal aborted - status:${current.status}, bet:${current.currentBet}" }
            return
        }
        _state.value = current.copy(status = GameStatus.DEALING)
        val (playerHands, dealerHand) = dealCardsWithAnimation(current)
        delay(dealCardDelayMs)
        applyInitialOutcome(current, playerHands, dealerHand)
    }

    private suspend fun dealCardsWithAnimation(
        current: GameState,
    ): Pair<kotlinx.collections.immutable.PersistentList<Hand>, Hand> {
        val bets = List(current.handCount) { current.currentBet }.toPersistentList()
        var deck = getInitialDeck(current).toPersistentList()
        var playerHands = List(current.handCount) { Hand() }.toPersistentList()
        var dealerHand = Hand()
        _state.value =
            _state.value.copy(playerHands = playerHands, dealerHand = dealerHand, playerBets = bets, deck = deck)
        for (round in 0..1) {
            for (i in 0 until current.handCount) {
                delay(dealCardDelayMs)
                val card = deck[0]
                deck = deck.removeAt(0)
                playerHands = playerHands.set(i, Hand(playerHands[i].cards.add(card)))
                _state.value = _state.value.copy(playerHands = playerHands, deck = deck)
                emitEffect(GameEffect.PlayCardSound)
            }
            delay(dealCardDelayMs)
            val card = deck[0]
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
            resolveInitialOutcomeValues(current, playerHands, dealerHand)

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

    private fun resolveInitialOutcomeValues(
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

    private fun determineInitialStatus(
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

    private fun getInitialDeck(current: GameState): List<Card> {
        return current.deck.ifEmpty {
            val deckSize = current.rules.deckCount * CARDS_PER_DECK
            val newDeck = ArrayList<Card>(deckSize)
            for (i in 1..current.rules.deckCount) {
                for (suit in Suit.entries) {
                    for (rank in Rank.entries) {
                        newDeck.add(Card(rank, suit))
                    }
                }
            }
            newDeck.shuffle(secureRandom)
            newDeck
        }
    }

    private fun handleNewGame(
        initialBalance: Int? = null,
        rules: GameRules = GameRules(),
        handCount: Int = 1,
        lastBet: Int = 0,
        lastSideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    ) {
        logger.d { "handleNewGame called with lastBet=$lastBet, lastSideBets=$lastSideBets" }
        val currentState = _state.value
        val newBalance = initialBalance ?: currentState.balance
        val maxAffordableMainBet = if (handCount > 0) newBalance / handCount else newBalance
        val clampedBet = lastBet.coerceIn(0, maxAffordableMainBet)

        var remainingBalance = newBalance - clampedBet * handCount
        val totalSideBetCost = lastSideBets.values.sum()

        val finalSideBets: PersistentMap<SideBetType, Int>
        val postSideBetBalance: Int

        if (totalSideBetCost <= remainingBalance) {
            finalSideBets = lastSideBets
            postSideBetBalance = remainingBalance - totalSideBetCost
        } else {
            finalSideBets = persistentMapOf()
            postSideBetBalance = remainingBalance
        }

        _state.value =
            GameState(
                status = GameStatus.BETTING,
                balance = postSideBetBalance,
                currentBet = clampedBet,
                sideBets = finalSideBets,
                lastSideBets = lastSideBets,
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
        logger.d { "DEALER_TURN: start" }
        if (_state.value.status != GameStatus.DEALER_TURN) {
            _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)
        }
        revealDealerHoleCard()
        logger.v { "DEALER_TURN: before first delay" }
        delay(dealerTurnDelayMs) // Visual pause for hole card reveal
        logger.v { "DEALER_TURN: after first delay" }

        handleInsurancePayout()

        var currentDealerHand = _state.value.dealerHand
        var currentDeck = _state.value.deck
        val rules = _state.value.rules

        while (shouldDealerDraw(currentDealerHand, rules)) {
            val isCritical =
                currentDealerHand.score in DEALER_STIFF_MIN until DEALER_STAND_THRESHOLD && !currentDealerHand.isSoft

            if (isCritical) {
                _state.value = _state.value.copy(dealerDrawIsCritical = true)
                emitEffect(GameEffect.DealerCriticalDraw)
                logger.v { "DEALER_TURN: before critical delay" }
                delay(dealerCriticalPreDelayMs)
                logger.v { "DEALER_TURN: after critical delay" }
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
            logger.v { "DEALER_TURN: before card delay" }
            delay(dealerTurnDelayMs)
            logger.v { "DEALER_TURN: after card delay" }

            if (isCritical) {
                _state.value = _state.value.copy(dealerDrawIsCritical = false)
            }
        }

        logger.d { "DEALER_TURN: finalizing game" }
        finalizeGame()
        logger.d { "DEALER_TURN: end" }
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
        val (totalPayout, anyWin, allPush) = calculateHandResults(state, dealerScore, dealerBust)

        val finalStatus =
            when {
                anyWin -> GameStatus.PLAYER_WON
                allPush -> GameStatus.PUSH
                else -> GameStatus.DEALER_WON
            }

        _state.value = state.copy(status = finalStatus, balance = state.balance + totalPayout)

        if (totalPayout > 0) emitEffect(GameEffect.ChipEruption(totalPayout))

        val totalBet = state.playerBets.sum()
        if (totalPayout < totalBet) emitEffect(GameEffect.ChipLoss(totalBet - totalPayout))

        when (finalStatus) {
            GameStatus.PLAYER_WON -> {
                emitEffect(GameEffect.PlayWinSound)
                emitEffect(GameEffect.WinPulse)
            }
            GameStatus.DEALER_WON -> {
                emitEffect(GameEffect.PlayLoseSound)
                val anyPlayerBust = state.playerHands.any { it.isBust }
                if (!anyPlayerBust) emitEffect(GameEffect.Vibrate)
            }
            GameStatus.PUSH,
            GameStatus.BETTING,
            GameStatus.DEALING,
            GameStatus.IDLE,
            GameStatus.PLAYING,
            GameStatus.INSURANCE_OFFERED,
            GameStatus.DEALER_TURN -> {}
        }
    }

    private fun calculateHandResults(
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

    private fun handleInsurancePayout() {
        val current = _state.value
        val dealerHasNaturalBJ =
            current.dealerHand.score == BLACKJACK_SCORE && current.dealerHand.cards.size == 2
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
    ): Int =
        when (determineHandOutcome(hand, dealerScore, dealerBust)) {
            HandOutcome.NATURAL_WIN -> bet + (bet * rules.blackjackPayout.numerator) / rules.blackjackPayout.denominator
            HandOutcome.WIN -> bet * 2
            HandOutcome.PUSH -> bet
            HandOutcome.LOSS -> 0
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
            _state.value =
                state.copy(
                    insuranceBet = 0,
                    status = GameStatus.PLAYING,
                )
        }

        if (_state.value.dealerHand.score == BLACKJACK_SCORE) {
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
            val totalRefund = current.sideBets.values.sum()
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
        _effects.tryEmit(effect)
    }
}
