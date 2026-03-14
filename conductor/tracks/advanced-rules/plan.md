# Implementation Plan - Advanced Rules: Double Down, Splitting & Insurance

Implement in order: Double Down first (simplest, isolated), Insurance second (new status + state field), Splitting last (most complex state changes + UI). Each step has its own test verification before moving on.

---

## Step 1: Update Domain Models (`shared/core/src/GameLogic.kt`)

### 1a: Add `DoubleDown` action
```kotlin
sealed class GameAction {
    // ... existing actions ...
    data object DoubleDown : GameAction()
    data object TakeInsurance : GameAction()
    data object DeclineInsurance : GameAction()
    data object Split : GameAction()
}
```

### 1b: Add `INSURANCE_OFFERED` to `GameStatus`
```kotlin
enum class GameStatus {
    BETTING, IDLE, PLAYING, INSURANCE_OFFERED, DEALER_TURN,
    PLAYER_WON, DEALER_WON, PUSH
}
```

### 1c: Add new fields to `GameState`
```kotlin
data class GameState(
    // ... existing fields ...
    val insuranceBet: Int = 0,
    val splitHand: Hand? = null,
    val splitBet: Int = 0,
    val isPlayingSplitHand: Boolean = false,
)
```
All new fields default to non-breaking values — existing serialized states remain valid.

---

## Step 2: Implement Double Down (`BlackjackStateMachine`)

Add `handleDoubleDown()` and wire it into the `dispatch()` function.

```kotlin
private suspend fun handleDoubleDown() {
    val state = _state.value
    if (state.status != GameStatus.PLAYING) return
    if (state.playerHand.cards.size != 2) return
    if (state.balance < state.currentBet) return

    val newBalance = state.balance - state.currentBet
    val newBet = state.currentBet * 2
    val (drawnCard, remainingDeck) = state.deck.first() to state.deck.drop(1)
    val newPlayerHand = Hand(state.playerHand.cards + drawnCard)

    val busted = newPlayerHand.isBust
    _state.value = state.copy(
        deck = remainingDeck,
        playerHand = newPlayerHand,
        balance = newBalance,
        currentBet = newBet,
        status = if (busted) GameStatus.DEALER_WON else GameStatus.DEALER_TURN,
    )
    _effects.emit(GameEffect.PlayCardSound)
    if (busted) {
        _effects.emit(GameEffect.PlayLoseSound)
        _effects.emit(GameEffect.Vibrate)
    } else {
        // Re-use handleStand() logic to run the dealer turn
        handleStand()
    }
}
```

> **NOTE:** `handleDoubleDown()` reuses `handleStand()` for dealer draw logic. To avoid duplication, extract the dealer-draw loop from `handleStand()` into a private `runDealerTurn()` helper that both `handleStand()` and `handleDoubleDown()` can call. This is the right time to do that refactor since both callers need it.

### Wire into dispatch:
```kotlin
is GameAction.DoubleDown -> handleDoubleDown()
```

---

## Step 3: Double Down Tests

In `BlackjackStateMachineTest.kt`, add a nested group or comment block `// --- Double Down ---`:

| Test | Given | Action | Expected |
| :--- | :--- | :--- | :--- |
| `doubleDown_doublesbet_and_deals_one_card` | PLAYING, 2-card hand, sufficient balance | DoubleDown | currentBet doubled, deck reduced by 1, playerHand has 3 cards |
| `doubleDown_transitions_to_dealer_turn` | PLAYING, 2-card hand | DoubleDown | status == DEALER_TURN (or terminal if dealer resolves) |
| `doubleDown_busted_is_DEALER_WON` | PLAYING, hand scoring 15, next deck card is a 10 | DoubleDown | status == DEALER_WON |
| `doubleDown_invalid_after_hit` | PLAYING, 3-card hand | DoubleDown | state unchanged |
| `doubleDown_invalid_if_insufficient_balance` | PLAYING, balance == currentBet - 1 | DoubleDown | state unchanged |
| `doubleDown_invalid_in_wrong_status` | BETTING | DoubleDown | state unchanged |

Run: `./amper test -m core -p jvm`

---

## Step 4: Implement Insurance (`BlackjackStateMachine`)

### 4a: Update `handleDeal()` to check for insurance offer
After the initial deal evaluation (natural BJ checks), insert:
```kotlin
// If dealer's face-up card is an Ace, offer insurance before PLAYING
if (!terminalOutcome && dealerHand.cards[0].rank == Rank.ACE) {
    _state.value = _state.value.copy(status = GameStatus.INSURANCE_OFFERED)
    return
}
```
Only transition to `INSURANCE_OFFERED` when the game would otherwise begin `PLAYING` (i.e., no immediate Blackjack terminal state).

### 4b: Add `handleTakeInsurance()`
```kotlin
private suspend fun handleTakeInsurance() {
    val state = _state.value
    if (state.status != GameStatus.INSURANCE_OFFERED) return
    val insuranceBet = state.currentBet / 2
    _state.value = state.copy(
        balance = state.balance - insuranceBet,
        insuranceBet = insuranceBet,
        status = GameStatus.PLAYING,
    )
}
```

### 4c: Add `handleDeclineInsurance()`
```kotlin
private suspend fun handleDeclineInsurance() {
    val state = _state.value
    if (state.status != GameStatus.INSURANCE_OFFERED) return
    _state.value = state.copy(
        insuranceBet = 0,
        status = GameStatus.PLAYING,
    )
}
```

### 4d: Update `handleStand()` dealer-turn resolution to check insurance payout
After the dealer draw loop completes, before setting the terminal status:
```kotlin
// Check if dealer has natural BJ and player has insurance
val dealerHasNaturalBJ = currentState.dealerHand.score == 21 &&
    currentState.dealerHand.cards.size == 2
if (currentState.insuranceBet > 0 && dealerHasNaturalBJ) {
    balanceUpdate += currentState.insuranceBet * 3  // 2:1 payout + return stake
}
// insuranceBet is always reset to 0 in NewGame; no explicit clear needed here
```

> **NOTE:** Insurance resolves at the *start* of `DEALER_TURN`, not at the end. The code should check whether the dealer's 2-card score (before drawing more) equals 21. The `handleStand()` function reveals the hole card first, which makes the dealer's true score visible — check immediately after the reveal, before the draw loop, to correctly identify a natural BJ.

### 4e: Reset `insuranceBet` in `handleNewGame()`
```kotlin
_state.value = GameState(
    // ...
    insuranceBet = 0,
)
```

### Wire into dispatch:
```kotlin
is GameAction.TakeInsurance -> handleTakeInsurance()
is GameAction.DeclineInsurance -> handleDeclineInsurance()
```

---

## Step 5: Insurance Tests

| Test | Given | Action | Expected |
| :--- | :--- | :--- | :--- |
| `insurance_offered_when_dealer_ace` | Deal completes, dealer up card is Ace | (after Deal) | status == INSURANCE_OFFERED |
| `insurance_not_offered_when_dealer_non_ace` | Deal completes, dealer up card is King | (after Deal) | status == PLAYING |
| `take_insurance_deducts_half_bet` | INSURANCE_OFFERED, bet=100, balance=900 | TakeInsurance | balance=850, insuranceBet=50, status=PLAYING |
| `decline_insurance_no_balance_change` | INSURANCE_OFFERED, bet=100, balance=900 | DeclineInsurance | balance=900, insuranceBet=0, status=PLAYING |
| `insurance_pays_on_dealer_blackjack` | Player takes insurance, dealer has BJ | Stand | balance += insuranceBet*3; DEALER_WON |
| `insurance_forfeited_on_no_dealer_blackjack` | Player takes insurance, dealer has no BJ | Stand → player wins | balance NOT += insuranceBet; regular win payout |
| `insurance_invalid_in_wrong_status` | PLAYING | TakeInsurance | state unchanged |

Run: `./amper test -m core -p jvm`

---

## Step 6: Implement Splitting (`BlackjackStateMachine`)

### 6a: Add `handleSplit()`
```kotlin
private suspend fun handleSplit() {
    val state = _state.value
    if (state.status != GameStatus.PLAYING) return
    if (state.playerHand.cards.size != 2) return
    if (state.playerHand.cards[0].rank != state.playerHand.cards[1].rank) return
    if (state.balance < state.currentBet) return
    if (state.splitHand != null) return  // no re-splitting

    val (card1, card2) = state.playerHand.cards
    val deck = state.deck.toMutableList()
    val newCard1 = deck.removeFirst()
    val newCard2 = deck.removeFirst()

    _state.value = state.copy(
        deck = deck,
        playerHand = Hand(listOf(card1, newCard1)),
        splitHand = Hand(listOf(card2, newCard2)),
        splitBet = state.currentBet,
        balance = state.balance - state.currentBet,
        isPlayingSplitHand = false,
    )
    _effects.emit(GameEffect.PlayCardSound)
    _effects.emit(GameEffect.PlayCardSound)
}
```

### 6b: Update `handleHit()` and `handleStand()` to route to active hand

Create a helper to get/set the active hand:
```kotlin
private val GameState.activeHand: Hand
    get() = if (isPlayingSplitHand) splitHand ?: playerHand else playerHand

private fun GameState.withActiveHand(hand: Hand): GameState =
    if (isPlayingSplitHand) copy(splitHand = hand) else copy(playerHand = hand)
```

Update `handleHit()` to use `state.activeHand` / `state.withActiveHand(...)`.

### 6c: Update `handleStand()` to advance to split hand or enter DEALER_TURN

At the end of the primary hand's stand resolution, check if `splitHand` exists and is not yet played:
```kotlin
if (!state.isPlayingSplitHand && state.splitHand != null) {
    // Advance to the split hand instead of entering DEALER_TURN
    _state.value = _state.value.copy(isPlayingSplitHand = true)
    return
}
// Otherwise, enter DEALER_TURN normally
```

### 6d: Update outcome resolution for split hands

After `DEALER_TURN` completes, calculate outcomes for both `playerHand` and `splitHand`:
```kotlin
fun resolveHand(hand: Hand, bet: Int, dealerScore: Int, dealerBust: Boolean): Int {
    return when {
        hand.isBust -> 0
        dealerBust || hand.score > dealerScore -> bet * 2
        hand.score == dealerScore -> bet
        else -> 0
    }
}

val primaryPayout = resolveHand(state.playerHand, state.currentBet, dealerScore, dealerBust)
val splitPayout = state.splitHand?.let {
    resolveHand(it, state.splitBet, dealerScore, dealerBust)
} ?: 0
_state.value = _state.value.copy(balance = _state.value.balance + primaryPayout + splitPayout)
```

Determine terminal status:
- `PLAYER_WON` if primary hand or split hand wins (or both).
- `DEALER_WON` if dealer beats both hands.
- `PUSH` if all hands tie.
- If outcomes differ (one win, one loss), use `PLAYER_WON` for the overall status and rely on the balance to reflect the mixed result. A future `ui-juice` track can display per-hand outcomes.

### 6e: Handle split Aces (FR-SPL-6)
In `handleHit()`, guard against hitting a split Ace hand beyond 2 cards:
```kotlin
val isAceSplit = state.splitHand != null &&
    state.playerHand.cards.firstOrNull()?.rank == Rank.ACE
if (isAceSplit && state.activeHand.cards.size >= 2) return
```

### 6f: Reset split state in `handleNewGame()`
```kotlin
splitHand = null, splitBet = 0, isPlayingSplitHand = false
```

### Wire into dispatch:
```kotlin
is GameAction.Split -> handleSplit()
```

---

## Step 7: Splitting Tests

| Test | Scenario | Expected |
| :--- | :--- | :--- |
| `split_creates_two_hands` | PLAYING, pair of 8s, sufficient balance | playerHand has 2 cards, splitHand has 2 cards |
| `split_deducts_balance` | balance=900, currentBet=100 | balance=800, splitBet=100 |
| `split_invalid_non_pair` | 8 + 9 | state unchanged |
| `split_invalid_insufficient_balance` | balance < currentBet | state unchanged |
| `split_invalid_after_hit` | 3-card hand | state unchanged |
| `stand_advances_to_split_hand` | splitHand present, primary hand stood | isPlayingSplitHand=true |
| `stand_enters_dealer_turn_after_split_hand` | splitHand present, both hands resolved | DEALER_TURN entered |
| `split_independent_payouts` | Primary wins, split loses | balance = primaryWin + 0 |
| `split_ace_no_extra_hit` | Split aces, try to hit | state unchanged after hit attempt |
| `split_invalid_when_split_already_active` | splitHand != null | state unchanged |

Run: `./amper test -m core -p jvm`

---

## Step 8: UI - Double Down Button (`sharedUI`)

In `BlackjackContent.kt` (or the `GameActions` composable):

- Show a `CasinoButton` labeled `stringResource(Res.string.double_down)` when:
  - `state.status == GameStatus.PLAYING`
  - `state.playerHand.cards.size == 2`
  - `state.balance >= state.currentBet`
- On click: `component.dispatch(GameAction.DoubleDown)`
- Add string to `sharedUI/composeResources/values/strings.xml`:
  ```xml
  <string name="double_down">Double Down</string>
  ```

---

## Step 9: UI - Insurance Prompt (`sharedUI`)

Create `InsurancePromptContent.kt`:
- Displayed when `state.status == GameStatus.INSURANCE_OFFERED`.
- Shows: dealer's Ace card, insurance cost (`currentBet / 2`), and two buttons.
- Replaces the standard `GameActions` row.
- Wire buttons to `TakeInsurance` and `DeclineInsurance`.
- Add strings:
  ```xml
  <string name="insurance_prompt">Dealer shows an Ace. Take insurance?</string>
  <string name="take_insurance">Take Insurance (%1$s)</string>
  <string name="decline_insurance">No Thanks</string>
  ```

In `BlackjackContent.kt`, add a branch for `INSURANCE_OFFERED`:
```kotlin
GameStatus.INSURANCE_OFFERED -> InsurancePromptContent(state, onAction)
```

---

## Step 10: UI - Split Hand Display (`sharedUI`)

When `state.splitHand != null`:
- Render two hand rows side-by-side (or stacked on compact viewports).
- The active hand (`isPlayingSplitHand` determines which) has a highlight border or label.
- The resolved hand is rendered at reduced opacity.
- Each hand row shows its own score and bet amount.
- `Split` button appears in `GameActions` when `FR-SPL-1` conditions are met.
- Add strings:
  ```xml
  <string name="split">Split</string>
  <string name="hand_one">Hand 1</string>
  <string name="hand_two">Hand 2</string>
  ```

---

## Verification Plan

- **Unit Tests**: All 20+ new test cases pass via `./amper test -m core -p jvm`.
- **Lint**: `./lint.sh` passes with no violations.
- **Manual - Double Down**: Deal a hand, double down, verify one card dealt, dealer plays, balance updates correctly.
- **Manual - Insurance**: Deal until dealer shows Ace; verify prompt appears; verify insurance pays on dealer BJ and is forfeited otherwise.
- **Manual - Splitting**: Deal a pair; split; play both hands; verify independent balance outcomes.
- **Manual - Split Aces**: Split Aces; verify exactly one card per hand; Hit button is disabled/ignored.
