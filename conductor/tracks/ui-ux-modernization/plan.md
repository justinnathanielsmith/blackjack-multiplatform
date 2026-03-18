# Implementation Plan - UI/UX Modernization

## Phase 1: Structural Boundaries (`BlackjackHandContainer.kt`, `ScoreBadge.kt`)
**Goal**: Move badges and labels to standardized, contained positions.

### Tasks
- [x] Update `BlackjackHandContainer.kt` to position the state label at `Alignment.TopCenter`.
- [x] Update `ScoreBadge` placement in `BlackjackHandContainer.kt` to perfectly straddle the container's border (Alignment.TopCenter for Dealer, Alignment.BottomCenter for Player).
- [x] Update `PlayerHand.kt` or `HandRow.kt` if they handle internal badge logic.

---

## Phase 2: Betting Polish (`PlayerHand.kt`, `BetChip.kt`)
**Goal**: Sub-set chips within the hand container.

### Tasks
- [x] Adjust `Box` nesting in `PlayerHand.kt` to anchor the `BetChip` relative to the bottom-center of the local hand area.
- [x] Remove any global offsets that push chips across container boundaries.

---

## Phase 3: Action Bar Modernization (`GameActions.kt`, `GameActionButton.kt`)
**Goal**: Semantic colors and text labels.

### Tasks
- [x] Update `GameAction` UI model or `GameActions.kt` to pass semantic colors to `GameActionButton`.
- [x] Modify `GameActionButton.kt` to include a `Text` element below the icon.
- [x] Scale icons slightly down to accommodate the label within the circular bounds.

---

## Phase 4: Aesthetic Refinements (`Header.kt`, `PlayingCard.kt`)
**Goal**: Premium asset finish.

### Tasks
- [x] Align `BalanceDisplay` and utility icons vertically in `Header.kt`.
- [x] Update `CardBack` in `PlayingCard.kt` to use a sophisticated gradient or subtle pattern (e.g., `Brush.radialGradient`).
- [x] Tweak Lightning icon alpha or background in the header.

---

## Verification Plan
- **Visual Diff**: Compare screenshots with the initial "floating" state to ensure clean boundaries.
- **Multi-platform Test**: Verify on JVM (Desktop) and simulate Mobile Portrait.
- **Regression Check**: Ensure split-hand scaling still works with the new badge/chip positioning.
