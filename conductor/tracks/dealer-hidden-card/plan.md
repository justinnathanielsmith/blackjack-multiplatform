# Implementation Plan - Dealer Hidden Card

## Step 1: Update `Card` Domain Model
- Add `val isFaceDown: Boolean = false` to `Card` in `GameLogic.kt`.
- Default is `false` so all existing card creation is unaffected.

## Step 2: Add `visibleScore` to `Hand`
- Add `val visibleScore: Int` computed property to `Hand` in `GameLogic.kt`.
- Counts only cards where `isFaceDown == false` using the same Ace-softening logic as `score`.
- `score` remains unchanged (used internally by the state machine for all game logic).

## Step 3: Update `handleDeal()` in `BlackjackStateMachine`
- After shuffling and splitting cards, mark dealer's second card face-down:
  ```kotlin
  val dealerCards = fullDeck.drop(2).take(2)
  val dealerHand = Hand(listOf(dealerCards[0], dealerCards[1].copy(isFaceDown = true)))
  ```
- Natural Blackjack terminal cases (`PUSH`, `DEALER_WON`) must reveal the hole card before setting state:
  ```kotlin
  fun Hand.revealed() = copy(cards = cards.map { it.copy(isFaceDown = false) })
  ```
- `PLAYER_WON` natural BJ: hole card stays hidden (dealer never reveals).

## Step 4: Update `handleStand()` in `BlackjackStateMachine`
- At the start of `DEALER_TURN`, before the dealer draw loop, flip all face-down dealer cards:
  ```kotlin
  _state.value = _state.value.copy(
      dealerHand = _state.value.dealerHand.copy(
          cards = _state.value.dealerHand.cards.map { it.copy(isFaceDown = false) }
      )
  )
  ```
- Add a short delay after reveal and before dealer draws for visual pacing.

## Step 5: Update `PlayingCard` UI Component (`sharedUI`)
- Add a branch for `card.isFaceDown == true`: render a card back (solid color with a pattern, no rank/suit text).
- Use `ModernGold` or a dark felt texture to distinguish the card back visually.

## Step 6: Update `BlackjackContent` Dealer Score Display
- During `GameStatus.PLAYING`, display `dealerHand.visibleScore` instead of `dealerHand.score`.
- During all other statuses, display `dealerHand.score` as normal.

## Step 7: Write / Update Tests
- **`BlackjackStateMachineTest`**: Add cases verifying:
  - After `Deal`, dealer hand has exactly one `isFaceDown = true` card (index 1).
  - After `Stand`, all dealer cards have `isFaceDown = false` before dealer draws.
  - Natural BJ `PLAYER_WON`: hole card remains face-down.
  - Natural BJ `PUSH` / `DEALER_WON`: hole card is revealed.
- **`HandTest`** (new or existing): Verify `visibleScore` returns score of face-up cards only.

## Verification Plan
- **Unit Tests**: All cases above pass via `./amper test -m core -p jvm`.
- **Manual UI**: On deal, dealer shows one card + one card back. On stand, card back flips before dealer draws. Score label updates correctly.
