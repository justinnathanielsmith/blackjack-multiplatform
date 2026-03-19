# Fix Dealer Hole Card Flip on Natural 21

## Objective
Ensure the dealer's hole card flips and is visible *before* the outcome animation plays when the dealer is dealt a natural 21 (Blackjack).

## Key Files & Context
- `shared/core/src/BlackjackStateMachine.kt`: Manages game state and transitions. Specifically, the `applyInitialOutcome` function determines the initial state after dealing cards.
- `shared/core/src/GameLogic.kt`: `resolveInitialOutcomeValues` determines the `initialStatus` and the `finalDealerHand` (which has the hole card face up if the dealer has Blackjack).

Currently, when the initial deal results in a terminal state (like `DEALER_WON` or `PUSH` due to a dealer Blackjack), `applyInitialOutcome` delays *before* updating the state with both the revealed hole card and the terminal status simultaneously. This causes the outcome animation (triggered by the terminal status) to overlap or preempt the visual flip of the hole card.

## Implementation Steps
1. Modify `applyInitialOutcome` in `shared/core/src/BlackjackStateMachine.kt`.
2. Inside the `if (initialStatus.isTerminal())` block, check if `finalDealerHand` is different from `dealerHand` (meaning the hole card was revealed).
3. If they are different, update the state's `dealerHand` immediately with `finalDealerHand` while keeping the current non-terminal status (e.g., `DEALING`).
4. This allows the UI to render the card flip *before* the subsequent `delay(getRevealDelayMs(dealerHand))`.
5. After the delay, the existing code will correctly apply the `initialStatus` (triggering the outcome animation) along with the rest of the updates.

**Changes in `applyInitialOutcome`:**
```kotlin
        if (initialStatus.isTerminal()) {
            // Update the dealer hand immediately so the card flips
            if (finalDealerHand != dealerHand) {
                _state.value = _state.value.copy(dealerHand = finalDealerHand)
            }
            delay(getRevealDelayMs(dealerHand))
        }
```

## Verification & Testing
1. Play the game and wait for the dealer to be dealt a natural 21.
2. Observe the sequence of animations.
3. Verify that the hole card flips and is fully visible *before* the "Dealer Wins" or "Push" outcome animation announces the result.
4. Run the JVM tests (`./amper test -p jvm`) to ensure no state machine logic is broken.