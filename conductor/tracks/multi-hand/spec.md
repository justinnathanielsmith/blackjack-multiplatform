# Specification - Multi-Hand Play

## Overview

Multi-hand play lets the player wager on 1, 2, or 3 simultaneous hands per round. Hands are dealt from the same shuffled deck and played sequentially (left to right). All hands compete against the same dealer draw at the end of the round.

This track also performs the planned generalization from the `advanced-rules` track: replacing the `playerHand + splitHand` dual-field model with a single `playerHands: List<Hand>`, unifying normal play, split, and multi-hand into one coherent structure.

---

## State Model Changes

### Remove

```kotlin
val playerHand: Hand            // → replaced by playerHands[0]
val splitHand: Hand?            // → replaced by playerHands[1..n]
val splitBet: Int               // → all hands share currentBet
val isPlayingSplitHand: Boolean // → replaced by activeHandIndex
```

### Add

```kotlin
val playerHands: List<Hand> = listOf(Hand())   // one hand by default
val activeHandIndex: Int = 0                    // which hand is being played
val handCount: Int = 1                          // 1, 2, or 3 (chosen in betting phase)
```

### Keep (unchanged)

- `currentBet: Int` — uniform per-hand bet; still set via `PlaceBet`
- `balance: Int` — decremented by `currentBet * (handCount - 1)` on Deal for the extra hands
- `insuranceBet: Int` — unchanged; insurance applies to the first hand only

### Updated Computed Properties

```kotlin
// Replaces canSplit() / canDoubleDown() on GameState
fun canSplit(): Boolean =
    playerHands.size < MAX_HANDS &&
    playerHands.getOrNull(activeHandIndex)?.let { hand ->
        hand.cards.size == 2 &&
        hand.cards[0].rank == hand.cards[1].rank &&
        balance >= currentBet
    } ?: false

fun canDoubleDown(): Boolean =
    playerHands.getOrNull(activeHandIndex)?.cards?.size == 2 &&
    balance >= currentBet

companion object {
    const val MAX_HANDS = 4  // initial handCount + all possible splits
}
```

### New Action

```kotlin
data class SelectHandCount(val count: Int) : GameAction()
```

---

## Feature Requirements

### FR-MH-1: Hand Count Selection

- `SelectHandCount(count)` is only valid when `status == GameStatus.BETTING`.
- Valid values: 1, 2, or 3. Other values are ignored.
- `handCount` is updated in `GameState` immediately; no other state changes.
- Default `handCount` is 1 (reset on every `NewGame`).

### FR-MH-2: Multi-Hand Deal

- Deal is only valid when `status == BETTING && currentBet > 0`.
- **Extra bet deduction**: Before dealing, verify `balance >= currentBet * (handCount - 1)`. If not, Deal is ignored.
- Deduct `currentBet * (handCount - 1)` from `balance` (the first hand's bet was deducted during `PlaceBet`).
- Create `handCount` hands, each receiving 2 cards from the shuffled deck in round-robin order:
  - `playerHands[0]` gets cards 0, 2
  - `playerHands[1]` gets cards 1, 3
  - `playerHands[2]` gets cards 4, 5
  - (Standard multi-deck deal order; keeps the deck position deterministic.)
- Dealer receives 2 cards as normal (hole card face-down).
- `activeHandIndex = 0`.
- **Natural BJ handling**:
  - In **single-hand mode** (`handCount == 1`): existing immediate BJ detection is preserved (PLAYER_WON / PUSH / DEALER_WON outcomes at deal time).
  - In **multi-hand mode** (`handCount > 1`): natural BJ is not checked at deal time. All hands enter `PLAYING`; BJ hands are evaluated at dealer-turn resolution.

### FR-MH-3: Sequential Hand Play

- `Hit`, `Stand`, `DoubleDown`, and `Split` always operate on `playerHands[activeHandIndex]`.
- When the active hand is resolved (Stand, bust, or DoubleDown auto-stand):
  - If `activeHandIndex < playerHands.size - 1` → increment `activeHandIndex` and continue `PLAYING`.
  - If `activeHandIndex == playerHands.size - 1` → enter `DEALER_TURN`.
- The `activeHandIndex` is never decremented; hands are played in insertion order.

### FR-MH-4: Hit on Active Hand

- Deals one card to `playerHands[activeHandIndex]`.
- If the hand busts: advance `activeHandIndex` per FR-MH-3 (bust counts as automatic stand; hand is not discarded).
- The busted hand is evaluated at dealer-turn resolution as a loss.

### FR-MH-5: Stand on Active Hand

- Marks the active hand as done; advance `activeHandIndex` per FR-MH-3.

### FR-MH-6: Double Down on Active Hand

- Available when `canDoubleDown()` is true.
- Deducts `currentBet` from `balance`; the doubled amount is stored as... **note**: since all hands share `currentBet`, doubling changes the bet for that specific hand. To support this without a `playerBets: List<Int>`, track doubled hands separately:

  ```kotlin
  val doubledHandIndices: Set<Int> = emptySet()
  ```

  A hand in `doubledHandIndices` pays out at `currentBet * 2` instead of `currentBet` at resolution.

  Alternatively (simpler): add `playerBets: List<Int>` parallel to `playerHands`. See the **Key Decisions** section below.

- Deals exactly one card; then advances `activeHandIndex` per FR-MH-3.
- If the dealt card causes a bust, the hand is treated as a loss (normal bust resolution).

### FR-MH-7: Split on Active Hand

- Available when `canSplit()` is true.
- Deducts `currentBet` from `balance`.
- Splits `playerHands[activeHandIndex]` into two hands and inserts the new hand at `activeHandIndex + 1`:
  - `playerHands[activeHandIndex]` keeps `card[0]` + one newly dealt card.
  - New hand (inserted at `activeHandIndex + 1`) starts with `card[1]` + one newly dealt card.
- `activeHandIndex` is unchanged; the player continues playing the current hand.
- Split Aces: if the split cards are Aces, each hand receives exactly one card and `Hit` is blocked for both (same rule as before).
- Split is not allowed on a hand that already originated from a split if `playerHands.size >= MAX_HANDS`.

### FR-MH-8: Dealer Turn & Outcome Resolution

- After all hands are resolved, `DEALER_TURN` begins.
- Dealer draws per standard rules (stand on ≥17).
- Each hand in `playerHands` is evaluated independently:
  - Busted hand → loss (payout = 0).
  - Win: `balance += effectiveBet * 2` (see FR-MH-6 note on doubled bets).
  - Push: `balance += effectiveBet`.
  - Loss: nothing added.
- Terminal `GameStatus` is determined by aggregate result:
  - Any hand wins → `PLAYER_WON`.
  - All hands push → `PUSH`.
  - All hands lose → `DEALER_WON`.
  - Mixed win+loss → `PLAYER_WON` (balance reflects actual result; per-hand badges show details).

### FR-MH-9: Per-Hand Bet Tracking

To support Double Down changing an individual hand's bet, `GameState` needs per-hand bets:

```kotlin
val playerBets: List<Int> = listOf(0)  // parallel to playerHands; populated on Deal
```

- On Deal: `playerBets = List(handCount) { currentBet }`.
- On Split: insert `currentBet` at `activeHandIndex + 1` in `playerBets`.
- On Double Down on hand `i`: `playerBets[i] *= 2`; deduct the extra `currentBet` from `balance`.
- `currentBet` remains the "per-hand base bet" for the betting phase; `playerBets` takes over during play.
- `canDoubleDown()` guards: `balance >= playerBets[activeHandIndex]`.
- `canSplit()` guards: `balance >= currentBet` (split always uses the base bet).

### FR-MH-10: Insurance

- Insurance is offered only when playing a single initial hand (`handCount == 1` or if all hands happen to have the same dealer ace condition — which is always true since there's one dealer).
- Actually: insurance offer is checked after Deal regardless of `handCount`.
- Insurance resolves against the primary hand's bet (`playerBets[0]`). No change from current logic; the `insuranceBet` field and resolution remain identical.

### FR-MH-11: Reset

On `NewGame`:
```kotlin
playerHands = listOf(Hand())
playerBets = listOf(0)
activeHandIndex = 0
handCount = 1
```

All other state resets as before.

---

## UI Components

### Hand Count Selector (Betting Phase)

- A 3-option tab/chip row in `BettingPhaseScreen`: **1 Hand**, **2 Hands**, **3 Hands**.
- Selecting an option dispatches `SelectHandCount(n)`.
- The selected option is visually highlighted (active style).
- Below the chip selector, show the total commitment: `Bet per hand: X | Total: X × N`.

### Multi-Hand Play Display

When `playerHands.size > 1`:
- Render all hands in a horizontal row (portrait) or stacked column section (landscape).
- Active hand (`activeHandIndex`) has a highlighted border / "YOUR TURN" badge.
- Completed hands (index < activeHandIndex) show their result badge (WIN / LOSS / PUSH) and are dimmed.
- Pending hands (index > activeHandIndex) show cards face-down or at reduced opacity.
- Each hand displays its individual `playerBets[i]` amount.

When `playerHands.size == 1` (default): layout is identical to current single-hand display.

### Strings to Add

```xml
<string name="hand_count_label">Hands</string>
<string name="one_hand">1</string>
<string name="two_hands">2</string>
<string name="three_hands">3</string>
<string name="total_bet">Total: %1$s</string>
<string name="hand_number">Hand %1$d</string>
```

---

## Out of Scope

| Feature | Reason |
| :--- | :--- |
| Individual bet amounts per hand | Requires redesigned betting UX; uniform bet covers the common case. |
| More than 3 initial hands | Layout becomes too cramped on mobile without a scroll/pagination design. |
| Natural BJ immediate payout in multi-hand mode | Adds per-hand terminal state tracking; deferred for simplicity. |
| Persistent hand count preference | No persistence layer yet; handCount resets per round. |
| Re-splitting beyond MAX_HANDS (4) | Caps complexity; covers virtually all real-game scenarios. |
| Surrender | Previously deferred from `advanced-rules`; remains out of scope. |
