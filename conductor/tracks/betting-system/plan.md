# Implementation Plan - Betting System & Chip Management

## Step 1: Update Domain Models
- Modify `GameState` in `GameLogic.kt` to include `balance` and `currentBet`.
- Add `BETTING` to `GameStatus`.
- Add `PlaceBet`, `ResetBet`, and `Deal` to `GameAction`.

## Step 2: Update State Machine
- Handle `PlaceBet`: Validate balance, update `currentBet`.
- Handle `ResetBet`: Return `currentBet` to `balance`.
- Handle `Deal`: Ensure `currentBet > 0`, then trigger card dealing.
- Update win/loss/push handlers to update `balance` based on `currentBet`.
- Implement 3:2 payout for natural Blackjack (Ace + 10-value card on initial deal).

## Step 3: UI Implementation (SharedUI)
- Create `ChipSelector` component.
- Create `BettingPhaseContent` or update `BlackjackContent` to handle the `BETTING` state.
- Update `GameActions` to show betting controls when in `BETTING` state.
- Add animations for chips moving to the table.

## Step 4: Persistence (Optional/Future)
- Save balance to platform-specific storage (DataStore or similar).

## Verification Plan
- **Unit Tests**: Update `BlackjackStateMachineTest.kt` to verify betting logic and payouts.
- **UI Testing**: Manual verification of betting flow on Android and Desktop.
