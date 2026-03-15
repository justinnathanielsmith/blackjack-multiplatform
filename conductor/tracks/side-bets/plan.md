# Plan - Side Bets Implementation

Implement side bets (21+3, Perfect Pairs) into the Blackjack core and UI.

## Phase 1: Core Domain & Logic
- [x] Define `SideBetType` and related payout structures in `GameLogic.kt`.
- [x] Add `sideBets: PersistentMap<SideBetType, Int>` to `GameState`.
- [x] Implement `checkTwentyOnePlusThree(playerHand: Hand, dealerUpcard: Card)` logic.
- [x] Implement `checkPerfectPairs(playerHand: Hand)` logic.
- [x] Update `GameAction` with `PlaceSideBet` and `ResetSideBets`.

## Phase 2: State Machine Integration
- [x] Update `BlackjackStateMachine.handlePlaceBet` to support side bets (or add dedicated handler).
- [x] Update `BlackjackStateMachine.handleDeal` to:
    - [x] Evaluate side bets immediately after deal.
    - [x] Calculate total side bet payout.
    - [x] Update balance and emit effects for wins.
- [x] Integrate Insurance into this unified side bet resolution flow.

## Phase 3: UI Implementation
- [x] Update `BettingPhaseScreen` to include side bet placement areas.
- [x] Add visual indicators for placed side bets.
- [x] Implement animations/effects for side bet wins during the deal phase.

## Phase 4: Verification & Polish
- [x] Unit tests for 21+3 and Perfect Pairs logic.
- [x] Integration tests for side bet settlement in the State Machine.
- [x] UI verification for responsive layouts and "juicy" feel.
