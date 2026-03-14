# Specification - UI Polish & "Juicy" Animations

## Goal

Transform the functional Blackjack UI into a delightful, casino-feel experience through targeted motion design. Every player action should have a corresponding visual and/or haptic response that reinforces feedback and communicates game state clearly.

---

## Functional Requirements

### FR-JUICE-1: Hole Card Flip Animation

When the dealer's hole card is revealed (transition from `DEALER_TURN` state), the face-down card must animate with a 3D flip (Y-axis rotation) from its back face to its front face.

- **Trigger**: `Card.isFaceDown` changes from `true` to `false`.
- **Duration**: ~400ms.
- **Easing**: `FastOutSlowInEasing` (starts fast, settles gently).
- **Mid-point swap**: The card face texture (back → front) swaps at the 90° midpoint of the rotation, matching physical card flip behavior.
- **Current state**: `PlayingCard` already has a `rotationY` entrance animation but does not animate the `isFaceDown` → `false` state change reactively.

### FR-JUICE-2: Card Deal Slide-In Animation

Each card slides in from off-screen (or from a deck position) when it first enters composition.

- **Trigger**: Card composable enters composition (new card dealt).
- **Direction**: Cards dealt to the dealer slide in from the top; cards dealt to the player slide in from the bottom.
- **Duration**: ~300ms per card with a ~100ms stagger between cards in the same deal action.
- **Easing**: `DecelerateEasing` (fast start, smooth stop).
- **Current state**: Cards appear instantly without entrance motion.

### FR-JUICE-3: Chip Toss / Bet Placement Feedback

When the player taps a chip button during the betting phase, a ghost chip animates from the chip button toward the bet display area before disappearing.

- **Trigger**: `ChipSelector` button tap.
- **Duration**: ~350ms arc animation.
- **Easing**: Parabolic arc (custom `keyframes` spec with Y offset).
- **End state**: Ghost chip fades out as it reaches the bet display.
- **Fallback**: If chip position cannot be determined (e.g., windowing constraints), skip animation; bet value still updates immediately.

### FR-JUICE-4: Balance Counter Roll Animation

When `GameState.balance` changes, the balance display animates from the old value to the new value using a rolling number effect.

- **Trigger**: `balance` value change in `GameState`.
- **Duration**: ~600ms for large changes (>$200), ~300ms for small changes.
- **Interpolation**: `IntAnimatable` counting from old value to new value.
- **Format**: Dollar sign prefix, same format as current static display (`$1,000`).
- **Current state**: Balance updates instantly on state change.

### FR-JUICE-5: Per-Hand Outcome Badges (Split Hands)

When a split hand game resolves, each hand (`playerHand` and `splitHand`) displays an individual outcome badge: **WIN**, **LOSS**, or **PUSH**.

- **Trigger**: Terminal `GameStatus` (`PLAYER_WON`, `DEALER_WON`, `PUSH`) AND `splitHand != null`.
- **Badge content**: WIN (gold), LOSS (red), PUSH (gray).
- **Determination logic**:
  - Hand wins if `!hand.isBust && (dealerBust || hand.score > dealerScore)`.
  - Hand pushes if `!hand.isBust && hand.score == dealerScore`.
  - Hand loses otherwise.
  - Expose `HandResult` (WIN/LOSS/PUSH) as a computed property or passed explicitly from the terminal `GameState`.
- **Animation**: Badge scales in (`scaleIn`) with a spring effect when the terminal state is reached.
- **Single-hand games**: No per-hand badge needed; existing `GameStatusMessage` composable covers this.

### FR-JUICE-6: Button Press Scale Feedback

All interactive buttons (`CasinoButton`, `ActionIcon`, chip buttons) briefly scale down on press and spring back on release.

- **Scale down**: 0.93× on press, ~80ms.
- **Spring back**: `spring(dampingRatio = 0.4f, stiffness = 400f)` on release.
- **Implementation**: Wrap button content in a `Modifier.pointerInput` or use `Indication`-based approach; keep it reusable in `CasinoButton`.

### FR-JUICE-7: Enhanced Win Celebration

On `PLAYER_WON` (non-split, or at least one split hand win), trigger a more impactful celebration:

- **Confetti burst**: Existing `ConfettiEffect` composable (already wired on win); increase particle count and duration.
- **Screen flash**: Brief white/gold overlay at ~10% alpha that fades out over 300ms.
- **Sound**: Existing `GameEffect.PlayWinSound` already plays; no change needed.
- **Blackjack natural (3:2)**: Use a distinct higher-intensity confetti burst vs. a regular win.

---

## Out of Scope

| Feature | Reason |
| :--- | :--- |
| Card shuffle animation | No persistent deck visualization; deck is an in-memory list. |
| Dealer "thinking" delay animation | Would require artificial delays in state machine; keeps domain pure. |
| 3D perspective table tilt | Too complex for current layout; out of scope for this pass. |
| Custom font animations for status text | Over-engineering; `GameStatusMessage` styling is sufficient. |
| Re-splitting / multi-level hand animation | Deferred to `multi-hand` track. |

---

## Non-Functional Requirements

- All animations must be **purely presentational**: zero changes to `shared/core` domain logic or `BlackjackStateMachine`.
- Animations must be **interruptible**: if a new game starts mid-animation, state transitions cleanly without stuck frames.
- On **low-end devices** or when `LocalInspectionMode` is active, animations should complete instantly (use `if (LocalInspectionMode.current) 0 else durationMs`).
- Total added **compose recomposition scope** should remain contained; avoid triggering full-screen recompositions for per-card animations.
