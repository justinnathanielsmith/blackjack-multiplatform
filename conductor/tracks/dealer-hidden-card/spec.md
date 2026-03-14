# Specification - Dealer Hidden Card (Hole Card)

## Overview

In standard Blackjack, the dealer receives two cards on the initial deal: one face-up (the **up card**) and one face-down (the **hole card**). The hole card is not revealed until the dealer's turn begins. This is a rules-correctness requirement and a prerequisite for the `advanced-rules` track (insurance bets require checking the dealer's up card only).

Currently, both dealer cards are fully exposed in `GameState`, which violates Blackjack rules and leaks information to the player.

---

## Requirements

### FR-1: Initial Deal
- The dealer's first card (index 0) is dealt **face-up**.
- The dealer's second card (index 1) is dealt **face-down** (the hole card).
- This applies only during `BETTING → PLAYING` transitions (i.e., `handleDeal()`).

### FR-2: Natural Blackjack on Deal
- If the player has a natural Blackjack (score == 21) and the dealer does **not**, the game ends immediately as `PLAYER_WON` — the hole card is **not** revealed.
- If both have natural Blackjack (score == 21), the game ends as `PUSH` — the hole card **is** revealed.
- If only the dealer has natural Blackjack (score == 21), the game ends as `DEALER_WON` — the hole card **is** revealed.

### FR-3: Hole Card Reveal
- When `handleStand()` transitions the game to `DEALER_TURN`, the hole card is **flipped face-up** before the dealer begins drawing.
- The UI must show a brief reveal before dealer draws begin.

### FR-4: UI Display During PLAYING
- The dealer's hole card is rendered as the **back of a card** (no rank/suit visible).
- The dealer's displayed score shows only the score of face-up cards (e.g., "8" not "18" when holding 8 + hole card).

### FR-5: UI Display During DEALER_TURN / Terminal States
- Once the hole card is revealed, all cards show normally and the full score is displayed.

### FR-6: Serialization Safety
- `Card.isFaceDown` must be serializable so `GameState` remains `@Serializable`.

---

## State Model Changes

### `Card`
```kotlin
// Add field:
val isFaceDown: Boolean = false
```

### `Hand`
```kotlin
// Add computed property for UI display:
val visibleScore: Int  // score counting only face-up cards
```

### `GameState`
No structural changes required — hidden card state is carried per `Card`.

---

## Domain Rules

| Condition | Hole Card Revealed? |
| :--- | :--- |
| Player natural BJ, dealer does not | No |
| Both natural BJ (push) | Yes |
| Dealer natural BJ, player does not | Yes |
| Player stands → `DEALER_TURN` | Yes (on transition) |
| Player busts (`DEALER_WON` via hit) | No — dealer never turns |

---

## Out of Scope
- Peek for Ace/10 (European vs. American rules) — deferred to `advanced-rules`.
- Insurance side bet — deferred to `advanced-rules`.
- Animated card flip — deferred to `ui-juice` (placeholder back-of-card is sufficient here).
