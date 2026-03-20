# Dealer HUD Redesign

## Objective
Fix the overlapping and cluttered appearance of the dealer HUD in the main game interface.

## Key Files & Context
- `sharedUI/src/ui/components/OverlayCardTable.kt`: This file is responsible for rendering the overlays for each hand zone (both player and dealer).
- Specifically, within `HandZoneOverlay`, the `isDealer` branch renders a `HudTitleBadge` and a `ScoreBadge` using absolute alignments (`TopStart` with a negative offset and `TopCenter` with a negative offset).

## Problem
Because the `HudTitleBadge` is aligned to `TopStart` and the `ScoreBadge` is aligned to `TopCenter` within the same box constraints, they often overlap when the dealer hand zone width is constrained, causing a messy and unreadable UI.

## Proposed Solution
Refactor the `isDealer` layout block in `OverlayCardTable.kt` to group the `HudTitleBadge` and `ScoreBadge` into a unified `Row`.

1. Remove the absolute alignments from `HudTitleBadge` and `ScoreBadge`.
2. Wrap both components in a `Row`.
3. Align the `Row` to `TopCenter` with a single negative Y offset (`-18.dp`).
4. Apply horizontal spacing (`Arrangement.spacedBy(8.dp)`) between the badges to ensure they never overlap and are clearly presented as a single cohesive unit.
5. Set `verticalAlignment = Alignment.CenterVertically` on the `Row`.

## Implementation Steps
1. Open `sharedUI/src/ui/components/OverlayCardTable.kt`.
2. Locate `HandZoneOverlay` and the `if (isDealer)` block.
3. Replace the standalone `HudTitleBadge` and `ScoreBadge` calls with a `Row` layout containing both, as proposed above.
4. Verify the UI via a local UI test or Amper run to ensure the visual clutter is resolved.

## Verification & Testing
- Build and run the JVM target (`./amper run -p jvm`) to verify the dealer HUD visually.
- The "DEALER" badge and the score pill should sit neatly side-by-side above the dealer's cards without overlapping.