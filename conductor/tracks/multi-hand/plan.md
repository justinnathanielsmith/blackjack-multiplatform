# Implementation Plan - Multi-Hand Play

Implement in order: domain refactor first (all other steps depend on it), then state machine logic, then tests, then UI. The domain refactor is a breaking change — all existing tests will need updating before new tests can be added.

---

## Step 1: Domain Refactor (`shared/core/src/GameLogic.kt`)

### 1a: Remove deprecated fields from `GameState`

```kotlin
// Remove:
val playerHand: Hand
val splitHand: Hand?
val splitBet: Int
val isPlayingSplitHand: Boolean

// Remove from canSplit() / canDoubleDown():
//   splitHand == null check
//   splitHand parameter references
```

### 1b: Add new fields to `GameState`

```kotlin
data class GameState(
    val deck: List<Card> = emptyList(),
    val playerHands: List<Hand> = listOf(Hand()),   // non-empty; index 0 = primary
    val playerBets: List<Int> = listOf(0),           // parallel to playerHands
    val activeHandIndex: Int = 0,
    val handCount: Int = 1,                          // selected in betting phase
    val dealerHand: Hand = Hand(),
    val status: GameStatus = GameStatus.IDLE,
    val balance: Int = 1000,
    val currentBet: Int = 0,                         // per-hand base bet (betting phase only)
    val insuranceBet: Int = 0,
) {
    companion object {
        const val MAX_HANDS = 4
    }

    val activeHand: Hand get() = playerHands[activeHandIndex]
    val activeBet: Int get() = playerBets[activeHandIndex]

    fun canDoubleDown(): Boolean =
        activeHand.cards.size == 2 && balance >= activeBet

    fun canSplit(): Boolean =
        playerHands.size < MAX_HANDS &&
        activeHand.cards.size == 2 &&
        activeHand.cards[0].rank == activeHand.cards[1].rank &&
        balance >= currentBet
}
```

> **NOTE:** `currentBet` is the per-hand base bet used during the betting phase. Once hands are dealt, `playerBets` is the authoritative per-hand bet. `currentBet` is still needed for: `canSplit()` (cost of a new hand), insurance calculation, and `handlePlaceBet` / `handleResetBet`.

### 1c: Add `SelectHandCount` action

```kotlin
sealed class GameAction {
    // ... existing actions ...
    data class SelectHandCount(val count: Int) : GameAction()
}
```

### 1d: Update `handleNewGame()` call sites in tests

After updating `GameState`, all existing tests that construct `GameState(playerHand = ..., splitHand = ...)` need to be updated to use `playerHands = listOf(...)`. Do this before adding new tests.

---

## Step 2: State Machine — `handleNewGame` & `handleSelectHandCount`

### 2a: `handleNewGame`

```kotlin
private fun handleNewGame(initialBalance: Int? = null) {
    _state.value = GameState(
        status = GameStatus.BETTING,
        balance = initialBalance ?: _state.value.balance,
        currentBet = 0,
        playerHands = listOf(Hand()),
        playerBets = listOf(0),
        activeHandIndex = 0,
        handCount = 1,
    )
}
```

### 2b: `handleSelectHandCount`

```kotlin
private fun handleSelectHandCount(count: Int) {
    if (_state.value.status != GameStatus.BETTING) return
    if (count !in 1..3) return
    _state.value = _state.value.copy(handCount = count)
}
```

Wire into `dispatch`:
```kotlin
is GameAction.SelectHandCount -> handleSelectHandCount(action.count)
```

---

## Step 3: State Machine — `handleDeal`

Replace current `handleDeal()` with a multi-hand aware version.

```kotlin
@Suppress("CyclomaticComplexity")
private fun handleDeal() {
    val current = _state.value
    if (current.status != GameStatus.BETTING || current.currentBet <= 0) return

    val extraCost = current.currentBet * (current.handCount - 1)
    if (current.balance < extraCost) return  // can't afford extra hands

    val fullDeck = /* shuffle as before */ ...

    // Deal round-robin into handCount hands
    // Hand 0: cards[0], cards[handCount]
    // Hand 1: cards[1], cards[handCount+1]
    // etc.
    val hands = List(current.handCount) { i ->
        Hand(listOf(fullDeck[i], fullDeck[i + current.handCount]))
    }
    val deckOffset = current.handCount * 2
    val dealerCards = fullDeck.drop(deckOffset).take(2)
    val dealerHand = Hand(listOf(dealerCards[0], dealerCards[1].copy(isFaceDown = true)))
    val remainingDeck = fullDeck.drop(deckOffset + 2)

    val newBalance = current.balance - extraCost  // first hand's bet already deducted
    val bets = List(current.handCount) { current.currentBet }

    // Single-hand BJ detection (unchanged behavior)
    val initialStatus = if (current.handCount == 1) {
        when {
            hands[0].score == 21 && dealerHand.score == 21 -> GameStatus.PUSH
            hands[0].score == 21 -> GameStatus.PLAYER_WON
            dealerHand.score == 21 -> GameStatus.DEALER_WON
            else -> GameStatus.PLAYING
        }
    } else {
        GameStatus.PLAYING  // multi-hand: no immediate BJ check
    }

    val finalDealerHand = when (initialStatus) {
        GameStatus.PUSH, GameStatus.DEALER_WON ->
            Hand(dealerHand.cards.map { it.copy(isFaceDown = false) })
        else -> dealerHand
    }

    val balanceUpdate = when (initialStatus) {
        GameStatus.PLAYER_WON -> current.currentBet * 2
        GameStatus.PUSH -> current.currentBet
        else -> 0
    }

    _state.value = GameState(
        deck = remainingDeck,
        playerHands = hands,
        playerBets = bets,
        activeHandIndex = 0,
        handCount = current.handCount,
        dealerHand = finalDealerHand,
        status = initialStatus,
        balance = newBalance + balanceUpdate,
        currentBet = current.currentBet,
    )
    emitEffect(GameEffect.PlayCardSound)
    if (initialStatus == GameStatus.PLAYER_WON) emitEffect(GameEffect.PlayWinSound)
    if (initialStatus == GameStatus.PLAYING && dealerCards[0].rank == Rank.ACE) {
        _state.value = _state.value.copy(status = GameStatus.INSURANCE_OFFERED)
    }
}
```

> **Deal order note:** Round-robin dealing (`H0-H1-H2-H0-H1-H2`) mimics a real table. The index math: hand `i` gets `deck[i]` and `deck[i + handCount]`. Works for 1, 2, or 3 hands.

---

## Step 4: State Machine — `handleHit`

Replace routing logic with list-based active hand.

```kotlin
private fun handleHit() {
    val state = _state.value
    if (state.status != GameStatus.PLAYING) return

    // Block hits on split aces
    val isAceSplit = state.playerHands.size > 1 &&
        state.activeHand.cards.firstOrNull()?.rank == Rank.ACE
    if (isAceSplit && state.activeHand.cards.size >= 2) return

    val newCard = state.deck.first()
    val remainingDeck = state.deck.drop(1)
    val newHand = state.activeHand.copy(cards = state.activeHand.cards + newCard)
    val updatedHands = state.playerHands.toMutableList().also { it[state.activeHandIndex] = newHand }

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
```

### Helper: `advanceOrEndTurn`

```kotlin
private fun advanceOrEndTurn(state: GameState) {
    if (state.activeHandIndex < state.playerHands.size - 1) {
        // More hands to play
        _state.value = state.copy(activeHandIndex = state.activeHandIndex + 1)
    } else {
        // All hands done → dealer turn
        scope.launch {
            mutex.withLock {
                _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)
                runDealerTurn()
            }
        }
    }
}
```

---

## Step 5: State Machine — `handleStand`

```kotlin
private fun handleStand() {
    val state = _state.value
    if (state.status != GameStatus.PLAYING) return
    advanceOrEndTurn(state)
}
```

> This replaces the old `isPlayingSplitHand` branch. `advanceOrEndTurn` handles both multi-hand and split uniformly.

---

## Step 6: State Machine — `handleDoubleDown`

```kotlin
@Suppress("ReturnCount")
private fun handleDoubleDown() {
    val state = _state.value
    if (state.status != GameStatus.PLAYING) return
    if (!state.canDoubleDown()) return  // guards size == 2 && balance >= activeBet

    val drawnCard = state.deck.first()
    val remainingDeck = state.deck.drop(1)
    val newHand = state.activeHand.copy(cards = state.activeHand.cards + drawnCard)
    val updatedHands = state.playerHands.toMutableList().also { it[state.activeHandIndex] = newHand }

    // Double the bet for this hand
    val newBets = state.playerBets.toMutableList().also { it[state.activeHandIndex] *= 2 }
    val newBalance = state.balance - state.activeBet  // deduct the extra (original activeBet)

    val busted = newHand.isBust
    val newState = state.copy(
        deck = remainingDeck,
        playerHands = updatedHands,
        playerBets = newBets,
        balance = newBalance,
    )
    _state.value = newState
    emitEffect(GameEffect.PlayCardSound)

    if (busted) {
        emitEffect(GameEffect.PlayLoseSound)
        emitEffect(GameEffect.Vibrate)
        advanceOrEndTurn(newState)
    } else {
        advanceOrEndTurn(newState)
    }
}
```

> **NOTE:** `activeBet` at the time of calling is the pre-doubled amount. We deduct `activeBet` from `balance` (the cost of doubling), then update `playerBets[i]` to `activeBet * 2`. The net effect: balance drops by the original bet; payout at resolution uses the doubled bet.

---

## Step 7: State Machine — `handleSplit`

```kotlin
private fun handleSplit() {
    val state = _state.value
    if (state.status != GameStatus.PLAYING || !state.canSplit()) return
    if (state.deck.size < 2) return

    val card1 = state.activeHand.cards[0]
    val card2 = state.activeHand.cards[1]
    val newPrimaryHand = Hand(listOf(card1, state.deck[0]))
    val newSplitHand = Hand(listOf(card2, state.deck[1]))
    val isAceSplit = card1.rank == Rank.ACE

    val updatedHands = state.playerHands.toMutableList().apply {
        set(state.activeHandIndex, newPrimaryHand)
        add(state.activeHandIndex + 1, newSplitHand)
    }
    val updatedBets = state.playerBets.toMutableList().apply {
        add(state.activeHandIndex + 1, state.currentBet)
    }

    val newState = state.copy(
        deck = state.deck.drop(2),
        playerHands = updatedHands,
        playerBets = updatedBets,
        balance = state.balance - state.currentBet,
    )
    _state.value = newState
    emitEffect(GameEffect.PlayCardSound)
    emitEffect(GameEffect.PlayCardSound)

    if (isAceSplit) {
        // Ace split: auto-advance through both ace hands to dealer turn
        scope.launch {
            mutex.withLock {
                _state.value = _state.value.copy(status = GameStatus.DEALER_TURN)
                runDealerTurn()
            }
        }
    }
}
```

> Split on Aces: we immediately jump to DEALER_TURN since Aces can't be hit. Both ace hands are included in `playerHands` for resolution.

---

## Step 8: State Machine — `runDealerTurn` (outcome resolution)

Update to iterate over `playerHands` / `playerBets`:

```kotlin
// After dealer draw loop completes:
val dealerScore = _state.value.dealerHand.score
val dealerBust = _state.value.dealerHand.isBust

var totalPayout = 0
var anyWin = false
var allPush = true  // only true if every hand pushes

for (i in _state.value.playerHands.indices) {
    val hand = _state.value.playerHands[i]
    val bet = _state.value.playerBets[i]
    val payout = resolveHand(hand, bet, dealerScore, dealerBust)
    totalPayout += payout

    val handWins = !hand.isBust && (dealerBust || hand.score > dealerScore)
    val handPushes = !hand.isBust && !dealerBust && hand.score == dealerScore
    if (handWins) anyWin = true
    if (!handPushes) allPush = false
}

val finalStatus = when {
    anyWin -> GameStatus.PLAYER_WON
    allPush -> GameStatus.PUSH
    else -> GameStatus.DEALER_WON
}

_state.value = _state.value.copy(
    status = finalStatus,
    balance = _state.value.balance + totalPayout,
)
```

> Remove the old `splitHand != null` branch; the loop replaces both the single-hand and split-hand code paths.

---

## Step 9: Update Existing Tests

Before adding new tests, update `BlackjackStateMachineTest.kt` to replace all `playerHand =` / `splitHand =` constructor calls:

```kotlin
// Before:
GameState(playerHand = Hand(listOf(card1, card2)), balance = 1000, currentBet = 100, ...)

// After:
GameState(playerHands = listOf(Hand(listOf(card1, card2))), playerBets = listOf(100), balance = 1000, currentBet = 100, ...)
```

Run: `./amper test -m core -p jvm` — all existing tests should pass before adding new ones.

---

## Step 10: New Tests

Add a `// --- Multi-Hand ---` block in `BlackjackStateMachineTest.kt`:

| Test | Given | Action | Expected |
| :--- | :--- | :--- | :--- |
| `selectHandCount_updates_handCount` | BETTING | SelectHandCount(3) | handCount == 3 |
| `selectHandCount_ignored_outside_betting` | PLAYING | SelectHandCount(2) | state unchanged |
| `selectHandCount_ignores_invalid_values` | BETTING | SelectHandCount(0) / SelectHandCount(4) | state unchanged |
| `deal_creates_two_hands_when_handCount_2` | BETTING, handCount=2, currentBet=100, balance=1000 | Deal | playerHands.size==2, playerBets==[100,100] |
| `deal_deducts_extra_bet_for_multi_hand` | BETTING, handCount=3, currentBet=100, balance=1000 | Deal | balance == 800 (100 already deducted + 200 extra) |
| `deal_rejected_if_insufficient_balance` | BETTING, handCount=3, currentBet=100, balance=150 | Deal | state unchanged (only 50 left after first hand, need 200 more) |
| `stand_advances_to_next_hand` | PLAYING, 2 hands, activeHandIndex=0 | Stand | activeHandIndex==1, status==PLAYING |
| `stand_enters_dealer_turn_on_last_hand` | PLAYING, 2 hands, activeHandIndex=1 | Stand | status==DEALER_TURN |
| `hit_bust_advances_to_next_hand` | PLAYING, 2 hands, activeHandIndex=0, deck causes bust | Hit | activeHandIndex==1, status==PLAYING |
| `multi_hand_independent_payouts` | 2 hands, hand0 wins, hand1 loses | Dealer turn | balance += hand0Bet*2 only |
| `multi_hand_player_won_if_any_hand_wins` | hand0 wins, hand1 loses | Dealer turn | status == PLAYER_WON |
| `multi_hand_dealer_won_if_all_hands_lose` | both hands lose | Dealer turn | status == DEALER_WON |
| `multi_hand_push_if_all_hands_push` | both hands push | Dealer turn | status == PUSH |
| `split_inserts_hand_at_active_index_plus_one` | PLAYING, hand0=pair of 8s, 2 initial hands | Split | playerHands.size==3, playerHands[1] is new split hand |
| `double_down_updates_active_bet_only` | PLAYING, 3 hands, activeHandIndex=1 | DoubleDown | playerBets[1] doubled, playerBets[0] and [2] unchanged |
| `new_game_resets_hand_count_to_1` | after multi-hand round | NewGame | handCount==1, playerHands.size==1 |

Run: `./amper test -m core -p jvm`

---

## Step 11: UI — Hand Count Selector (`BettingPhaseScreen.kt`)

Add a row of 3 toggle buttons above or below the chip selector:

```kotlin
// In BettingPhaseScreen composable:
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf(1, 2, 3).forEach { count ->
        CasinoButton(
            text = "$count",
            isSelected = state.handCount == count,
            onClick = { component.onAction(GameAction.SelectHandCount(count)) },
        )
    }
}

// Total bet indicator:
if (state.handCount > 1 && state.currentBet > 0) {
    Text(stringResource(Res.string.total_bet, formatCurrency(state.currentBet * state.handCount)))
}
```

Add strings to `sharedUI/composeResources/values/strings.xml`:
```xml
<string name="hand_count_label">Hands</string>
<string name="total_bet">Total: %1$s</string>
```

---

## Step 12: UI — Multi-Hand Play Display (`BlackjackScreen.kt`)

Update `BlackjackLayout` to render all hands from `playerHands`:

```kotlin
// Replace the splitHand != null branch with a loop:
val hands = state.playerHands
if (hands.size > 1) {
    // Multi-hand: show all hands
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        hands.forEachIndexed { index, hand ->
            val isActive = index == state.activeHandIndex && state.status == GameStatus.PLAYING
            val isPending = index > state.activeHandIndex && state.status == GameStatus.PLAYING
            HandContainer(
                title = stringResource(Res.string.hand_number, index + 1),
                score = hand.score,
                bet = state.playerBets.getOrNull(index),
                isActive = isActive,
                isPending = isPending,
                result = state.handResult(index),
                modifier = Modifier.weight(1f),
            ) {
                HandRow(hand)
            }
        }
    }
} else {
    // Single hand: existing layout unchanged
    HandContainer(
        title = "You",
        score = state.playerHands[0].score,
        bet = if (state.status != GameStatus.IDLE) state.currentBet else null,
        isActive = state.status == GameStatus.PLAYING,
    ) {
        HandRow(state.playerHands[0])
    }
}
```

Update `primaryHandResult()` / `splitHandResult()` in `BlackjackScreen.kt` to a single `handResult(index: Int)` extension:

```kotlin
fun GameState.handResult(index: Int): HandResult {
    if (!status.isTerminal()) return HandResult.NONE
    val hand = playerHands.getOrNull(index) ?: return HandResult.NONE
    val dealerScore = dealerHand.score
    val dealerBust = dealerHand.isBust
    return when {
        hand.isBust -> HandResult.LOSS
        dealerBust || hand.score > dealerScore -> HandResult.WIN
        hand.score == dealerScore -> HandResult.PUSH
        else -> HandResult.LOSS
    }
}
```

Add string:
```xml
<string name="hand_number">Hand %1$d</string>
```

---

## Step 13: `GameActions.kt` — Update Split/DoubleDown Conditions

Replace `state.canSplit()` / `state.canDoubleDown()` call sites (they are already methods on `GameState`; the method bodies were updated in Step 1). No call-site changes needed unless the composable directly inspected `splitHand`.

---

## Verification Plan

- **Unit Tests**: All test cases pass via `./amper test -m core -p jvm`.
- **Lint**: `./lint.sh` passes with no violations.
- **Manual — Single hand**: Full game plays identically to before; no regressions.
- **Manual — 2 hands**: Select 2 hands, place 100 bet; verify 200 deducted on deal. Play hand 1 to completion; hand 2 becomes active. Dealer resolves both; balance correct.
- **Manual — 3 hands**: Select 3 hands, place 50 bet; verify 150 deducted. Play all 3 sequentially; verify per-hand WIN/LOSS/PUSH badges.
- **Manual — Split in multi-hand**: Play 2 initial hands; split one; verify 3 hands in the list; play proceeds correctly.
- **Manual — Double Down mid-multi-hand**: Verify only the active hand's bet doubles; other hands unaffected.
- **Manual — Ace split**: Split aces in multi-hand; verify both ace hands receive one card only and auto-proceed to dealer.
