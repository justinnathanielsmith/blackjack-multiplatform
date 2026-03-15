# Implementation Plan - UI/UX Modernization

## Phase 1: Structural Boundaries (`BlackjackHandContainer.kt`, `ScoreBadge.kt`)
**Goal**: Move badges and labels to standardized, contained positions.

### Tasks
- [ ] Update `BlackjackHandContainer.kt` to position the state label at `Alignment.TopCenter`.
- [ ] Move `ScoreBadge` placement in `BlackjackHandContainer.kt` to the top-right corner *inside* the box.
- [ ] Update `PlayerHand.kt` or `HandRow.kt` if they handle internal badge logic.

---

## Phase 2: Betting Polish (`PlayerHand.kt`, `BetChip.kt`)
**Goal**: Sub-set chips within the hand container.

### Tasks
- [ ] Adjust `Box` nesting in `PlayerHand.kt` to anchor the `BetChip` relative to the bottom-center of the local hand area.
- [ ] Remove any global offsets that push chips across container boundaries.

---

## Phase 3: Action Bar Modernization (`GameActions.kt`, `GameActionButton.kt`)
**Goal**: Semantic colors and text labels.

### Tasks
- [ ] Update `GameAction` UI model or `GameActions.kt` to pass semantic colors to `GameActionButton`.
- [ ] Modify `GameActionButton.kt` to include a `Text` element below the icon.
- [ ] Scale icons slightly down to accommodate the label within the circular bounds.

---

## Phase 4: Aesthetic Refinements (`Header.kt`, `PlayingCard.kt`)
**Goal**: Premium asset finish.

### Tasks
- [ ] Align `BalanceDisplay` and utility icons vertically in `Header.kt`.
- [ ] Update `CardBack` in `PlayingCard.kt` to use a sophisticated gradient or subtle pattern (e.g., `Brush.radialGradient`).
- [ ] Tweak Lightning icon alpha or background in the header.

---

## Verification Plan
- **Visual Diff**: Compare screenshots with the initial "floating" state to ensure clean boundaries.
- **Multi-platform Test**: Verify on JVM (Desktop) and simulate Mobile Portrait.
- **Regression Check**: Ensure split-hand scaling still works with the new badge/chip positioning.
