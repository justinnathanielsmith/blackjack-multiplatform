# Fix Small Chips Plan

## Objective
Hide the numerical value text on `BetChip` components when they are rendered at a small size (less than 32.dp wide) to prevent the text from overflowing or looking cluttered.

## Key Files & Context
- `sharedUI/src/ui/components/BetChip.kt`

## Implementation Steps
1. In `BetChip.kt`, add an import for `androidx.compose.foundation.layout.BoxWithConstraints`.
2. Change the root `Box` of the `BetChip` component to `BoxWithConstraints`.
3. Wrap the `Text` composable that renders the amount with an `if (maxWidth >= 32.dp)` check so it only renders when the chip is large enough.

## Verification & Testing
1. Check the `OverlayCardTable` and ensure chips on the table (which are scaled down) no longer display the value text.
2. Check `BettingPhaseScreen` and `BettingSlot` to verify that main chips and active side bet chips still display their text.
3. Review the `BetChipPreview` to ensure regular sized chips are unaffected.