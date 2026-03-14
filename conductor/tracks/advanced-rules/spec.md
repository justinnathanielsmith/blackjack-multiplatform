# Specification - Advanced Rules: Double Down, Splitting & Insurance

## Overview

This track adds three standard Blackjack rules that require player decisions beyond Hit/Stand. Each rule introduces new player agency, new betting interactions, and non-trivial state machine complexity. They are implemented in order of increasing complexity: **Double Down** → **Insurance** → **Splitting**.

---

## Feature 1: Double Down

### Overview

After receiving the initial two cards, the player may double their bet in exchange for receiving exactly one more card and then being forced to stand.

### Requirements

#### FR-DD-1: Availability
- The `DoubleDown` action is only valid when:
  - `status == GameStatus.PLAYING`
  - `playerHand.cards.size == 2` (initial two cards only — not after a hit)
  - `balance >= currentBet` (must be able to afford the doubled bet)

#### FR-DD-2: Bet Doubling
- `currentBet` doubles: the additional `currentBet` amount is deducted from `balance`.
- Example: balance=900, currentBet=100 → balance=800, currentBet=200.

#### FR-DD-3: Card Deal & Auto-Stand
- Exactly one card is dealt to the player's hand.
- The action immediately transitions to `DEALER_TURN` (equivalent to Stand) regardless of the card received.
- If the drawn card causes the player's score to exceed 21, the outcome is `DEALER_WON` (bust still counts).

#### FR-DD-4: Payout
- Win payout uses the same `currentBet * 2` formula. Since `currentBet` was doubled in FR-DD-2, the effective payout is 4× the original bet — which is correct.
- Push returns `currentBet` (the doubled amount).
- Loss forfeits `currentBet` (already deducted during the double-down action).

### State Model Changes

No new fields required. `DoubleDown` relies on existing `currentBet` and `balance` fields.

### New Action

```kotlin
data object DoubleDown : GameAction()
```

### New Effect

```kotlin
data object PlayCardSound  // already exists — reused on the dealt card
```

---

## Feature 2: Insurance

### Overview

When the dealer's face-up card is an Ace, the player is offered an **insurance** side bet before the game continues. Insurance pays 2:1 if the dealer has a natural Blackjack.

### Requirements

#### FR-INS-1: Offer Condition
- After `handleDeal()`, if `dealerHand.cards[0].rank == Rank.ACE` (the face-up card is an Ace), the game transitions to `GameStatus.INSURANCE_OFFERED` instead of `GameStatus.PLAYING`.
- If the face-up card is not an Ace, the game proceeds to `PLAYING` as normal.

#### FR-INS-2: Insurance Bet
- The maximum insurance bet is `currentBet / 2` (integer division, rounded down).
- `TakeInsurance` deducts `currentBet / 2` from `balance` and stores it in `insuranceBet`.
- `DeclineInsurance` sets `insuranceBet = 0` and does not touch `balance`.
- Both actions transition to `GameStatus.PLAYING`.

#### FR-INS-3: Dealer Blackjack Resolution
- After the player takes insurance and stands (triggering `DEALER_TURN`), the dealer's hole card is checked.
- If the dealer has a natural Blackjack (2-card score == 21):
  - Insurance pays 2:1: `balance += insuranceBet * 3` (return stake + 2× profit).
  - Original bet is lost → outcome is `DEALER_WON`.
- If the dealer does not have a natural Blackjack:
  - `insuranceBet` is forfeited (already deducted, nothing added back).
  - Game continues with dealer draw logic as normal.

#### FR-INS-4: Insurance Ignored on Player Bust
- If the player busts (hits to >21), insurance payout resolution is **skipped** — the dealer is not checked for Blackjack and the insuranceBet is forfeited.
- This mirrors casino rules where a bust ends the hand immediately.

#### FR-INS-5: Reset
- `insuranceBet` is reset to `0` on every `NewGame` action.

### State Model Changes

#### `GameStatus`
```kotlin
// Add:
INSURANCE_OFFERED
```

#### `GameState`
```kotlin
// Add field:
val insuranceBet: Int = 0
```

### New Actions

```kotlin
data object TakeInsurance : GameAction()
data object DeclineInsurance : GameAction()
```

### Domain Rules

| Dealer Up Card | Insurance Offered? |
| :--- | :--- |
| Ace | Yes → `INSURANCE_OFFERED` |
| Any other rank | No → `PLAYING` |

| Dealer Hole Card | Player took Insurance? | Result |
| :--- | :--- | :--- |
| Natural BJ | Yes | Insurance wins 2:1; main bet lost |
| Natural BJ | No | Main bet lost; no insurance refund |
| Not BJ | Yes | Insurance forfeited; game continues |
| Not BJ | No | Game continues normally |

---

## Feature 3: Splitting

### Overview

When the player's initial two cards share the same `Rank`, they may **split** the hand into two independent hands. Each hand has its own bet and is played sequentially.

### Requirements

#### FR-SPL-1: Availability
- The `Split` action is only valid when:
  - `status == GameStatus.PLAYING`
  - `playerHand.cards.size == 2`
  - `playerHand.cards[0].rank == playerHand.cards[1].rank` (same rank)
  - `balance >= currentBet` (must afford the second bet)
  - `splitHand == null` (no existing split — re-splitting is out of scope)

#### FR-SPL-2: Hand Creation
- An additional `currentBet` amount is deducted from `balance` and stored in `splitBet`.
- `playerHand` retains the first of the two original cards plus one newly dealt card.
- `splitHand` is initialized with the second of the two original cards plus one newly dealt card.
- `isPlayingSplitHand` is set to `false` (player begins with the primary hand).

#### FR-SPL-3: Sequential Play
- The player plays `playerHand` to completion (Hit/Stand/DoubleDown/Bust).
- Once the primary hand is resolved, `isPlayingSplitHand` is set to `true` and the player plays `splitHand` to completion.
- `DEALER_TURN` is entered only after both hands are fully played.
- Both hands play against the same dealer draw sequence.

#### FR-SPL-4: Independent Outcomes
- Payout is calculated separately for each hand vs. the final dealer hand:
  - `playerHand` vs `dealerHand`: uses `currentBet`.
  - `splitHand` vs `dealerHand`: uses `splitBet`.
- Win/loss/push for one hand does not affect the other.

#### FR-SPL-5: Balance Updates
- Winning hand: `balance += handBet * 2`
- Push hand: `balance += handBet`
- Losing hand: nothing added (bet already deducted on split)

#### FR-SPL-6: Splitting Aces (Special Rule)
- If the two split cards are Aces, each hand receives exactly one card (no further Hit allowed on either Ace hand).
- This is enforced by: if `splitHand != null && playerHand.cards[0].rank == Rank.ACE`, disable Hit and DoubleDown after the initial dealt card.

#### FR-SPL-7: Reset
- `splitHand`, `splitBet`, and `isPlayingSplitHand` reset to their defaults on every `NewGame`.

### State Model Changes

#### `GameState`
```kotlin
// Add fields:
val splitHand: Hand? = null
val splitBet: Int = 0
val isPlayingSplitHand: Boolean = false
```

### New Action

```kotlin
data object Split : GameAction()
```

---

## UI Components

### Double Down
- **`DoubleDown` button**: Displayed alongside Hit/Stand when `playerHand.cards.size == 2` and `balance >= currentBet`.
- Grayed out (disabled) after first hit.

### Insurance
- **`InsurancePrompt` overlay or inline component**: Shown when `status == INSURANCE_OFFERED`.
  - Displays the dealer's Ace.
  - Shows insurance cost (`currentBet / 2`).
  - Two actions: **Take Insurance** and **No Thanks** (decline).
- This replaces the standard game action row while in `INSURANCE_OFFERED`.

### Splitting
- **`Split` button**: Displayed alongside Hit/Stand when `FR-SPL-1` conditions are met.
- **Dual hand display**: When `splitHand != null`, both hands are shown side-by-side (or stacked on narrow viewports).
  - Active hand is highlighted; resolved hand is dimmed.
  - Each hand shows its own score and bet.
- Layout adapts to `isPlayingSplitHand` to indicate which hand the player is controlling.

---

## Out of Scope

- **Re-splitting**: Splitting a split hand. Deferred due to complexity; requires full `List<Hand>` refactor.
- **Splitting Aces (unlimited hits)**: The one-card-per-Ace rule (FR-SPL-6) is in scope; subsequent re-splits of Aces are not.
- **Surrender**: A fourth advanced action — deferred to a separate track.
- **European No-Peek rule**: Insurance under EU rules differs; this spec assumes American rules.
- **Side-bet persistence**: Insurance bets are not persisted across sessions.
