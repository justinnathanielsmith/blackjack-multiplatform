# Fix Action Buttons Clipping and Gap

## Objective
Fix the UI bug where action buttons clip into the hand cards during gameplay and there is a gap below the buttons.

## Key Files & Context
- `sharedUI/src/ui/components/ControlCenter.kt`: Contains the layout for the bottom control area, including `GameActions`, `BettingActions`, and `ChipRack`.

## Root Cause Analysis
The `ControlCenter` uses a `Column` to stack `GameActions`, `BettingActions`, `ChipRack`, and the footer.
During the playing phase (when action buttons like Hit, Stand are shown):
- `BettingActions` is removed from the layout via `AnimatedVisibility`.
- However, `ChipRack` is conditionally translated out of view using `.graphicsLayer { translationY = ... }`.
- Because `graphicsLayer` does not affect layout bounds, the `ChipRack` continues to take up empty space in the `Column`.
- This creates an invisible gap below the action buttons, pushing the action buttons up.
- Because the action buttons are pushed up, they encroach on the available vertical space for the game area (`BlackjackLayout`), causing the hand cards to clip into the action buttons.

## Implementation Steps
1. **Modify `ControlCenter.kt`**:
   - Refactor the conditionally displayed `ChipRack` so it is wrapped in an `AnimatedVisibility` block, similar to `BettingActions` (or combined with it).
   - Alternatively, change the animation strategy to use `expandVertically` and `shrinkVertically` alongside standard slide transitions so the layout height updates appropriately as it animates in/out.
   - Remove the `animateDpAsState` for `chipRackTranslationY` and its `.graphicsLayer` modifier, relying on `AnimatedVisibility` to handle layout changes cleanly.

## Verification & Testing
1. Start the game and observe the betting phase. The chip rack should appear normally at the bottom.
2. Place a bet and deal cards to enter the playing phase.
3. Verify that the chip rack animates out and vacates its layout space.
4. Check that the action buttons (Hit, Stand, etc.) settle at the bottom near the footer, without an empty gap below them.
5. Confirm that the action buttons do not clip into the player's hand cards.