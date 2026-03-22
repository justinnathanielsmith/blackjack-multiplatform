# Migrate Hand-Specific Bet Logic to Hand Model

## Background & Motivation
Currently, bets are stored in `GameState` as separate parallel arrays (`currentBets` and `lastBets`). However, the `Hand` class already has a `bet` property, which creates redundancy and forces parallel array synchronization. By moving hand-specific data like `lastBet` directly into the `Hand` model and using the existing `Hand` instances during the betting phase, we can simplify `GameState`, eliminate the parallel arrays, and make the logic more object-oriented.

## Scope & Impact
- Modify `Hand` and `GameState` models in `GameLogic.kt`.
- Refactor betting phase mutations in `BlackjackStateMachine.kt`.
- Update Compose UI elements relying on `state.currentBets`.

## Proposed Solution

### 1. Update Domain Models (`shared/core/src/GameLogic.kt`)
- Add `val lastBet: Int = 0` to the `Hand` data class.
- Remove `val currentBets: PersistentList<Int>` and `val lastBets: PersistentList<Int>` from `GameState`.
- Update `GameState.currentBet` to read from the first hand: `val currentBet: Int get() = playerHands.getOrNull(0)?.bet ?: 0`.
- Simplify `GameState.totalBet` to always read from `playerHands` regardless of the `status` (since hands will now hold the bets during the betting phase):
  ```kotlin
  val mainBetsTotal = playerHands.fold(0) { acc, h -> acc + h.bet }
  ```

### 2. Update State Machine (`shared/core/src/BlackjackStateMachine.kt`)
- **`handleNewGame`**:
  Instead of saving bets to `GameState`, initialize the `playerHands` list directly with new `Hand` instances containing the resolved bets:
  ```kotlin
  playerHands = List(handCount) { i ->
      Hand(bet = finalBets[i], lastBet = normalizedLastBets[i])
  }.toPersistentList()
  ```
- **`handlePlaceBet` & `handleResetBet`**:
  Modify these functions to update the `bet` amount on the specific `Hand` inside the `playerHands` list, rather than updating a `currentBets` array.
- **`handleSelectHandCount`**:
  When expanding or shrinking the number of hands, mutate `playerHands` directly (adding empty `Hand` instances or removing/refunding existing ones).
- **`handleDeal`**:
  Remove the line that initializes new `Hand` objects based on `currentBets`. Instead, simply use the existing `playerHands` (which already contain the correct bets) and deal cards to them.
- **Validation**:
  Change checks like `current.currentBets.any { it <= 0 }` to `current.playerHands.any { it.bet <= 0 }`.

### 3. Update UI Layer (`sharedUI/src/`)
Replace all usages of `state.currentBets` with `state.playerHands.map { it.bet }` or direct property access:
- **`ui/screens/BlackjackScreen.kt`**:
  - Update the `GameAction.NewGame` payload to map previous bets: `lastBets = state.playerHands.map { it.bet }.toPersistentList()`.
  - Update auto-deal conditions: `state.playerHands.all { it.bet > 0 }`.
- **`ui/screens/BettingPhaseScreen.kt`**:
  - Update seat amount display: `amount = state.playerHands.getOrNull(seatIndex)?.bet ?: 0`.
- **`ui/components/ControlCenter.kt`**:
  - Update `canDeal` check: `canDeal = state.playerHands.isNotEmpty() && state.playerHands.all { it.bet > 0 }`.

## Verification & Testing
1. **Compilation**: Run `./amper build -p jvm` to ensure all references to `currentBets` and `lastBets` are resolved.
2. **Gameplay Verification**:
   - Verify placing, resetting, and modifying bets works for 1 to 3 hands.
   - Verify that when dealing, the placed bets correctly transfer to the active hands.
   - Verify that upon game completion, the previous bets are correctly remembered and auto-placed in the next round (if affordable).
3. **Tests**: Run `./amper test -p jvm` to ensure existing betting logic tests pass with the new structural flow.