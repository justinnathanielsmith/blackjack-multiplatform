# Bolt Performance Optimization: Prevent Unnecessary Card Recomposition on Hole Card Reveal

## Objective
Reduce unnecessary Compose recompositions and memory allocations when the dealer's hole card is revealed.

## Key Files & Context
- `shared/core/src/GameLogic.kt`
- `shared/core/src/BlackjackStateMachine.kt`

## Implementation Steps
Currently, when the dealer's hole card is revealed, the state machine and game logic allocate new `Card` instances for *all* cards in the dealer's hand by calling `it.copy(isFaceDown = false)` unconditionally.
Since Kotlin's `data class copy` always allocates a new instance even if the values are identical, this breaks reference equality for the dealer's face-up cards (index 0). In Compose, this causes the `PlayingCard` composable for the first card to recompose unnecessarily.

1. **Update `revealDealerHoleCard` in `BlackjackStateMachine.kt`**:
   Change the `map` logic to only copy the card if it is currently face-down:
   ```kotlin
   .map { if (it.isFaceDown) it.copy(isFaceDown = false) else it }
   ```

2. **Update `resolveInitialOutcomeValues` in `GameLogic.kt`**:
   Apply the same conditional copy optimization:
   ```kotlin
   val dealerHandRevealed = Hand(dealerHand.cards.map { if (it.isFaceDown) it.copy(isFaceDown = false) else it }.toPersistentList())
   ```

## Verification & Testing
- Run `./amper test -p jvm` to ensure core game logic and state machine tests pass.
- Run `./amper build -p jvm` to verify the build succeeds.
- Launch the application and observe the Layout Inspector during a dealer turn to ensure the first dealer card does not recompose when the hole card flips.