# Implementation Plan - Premium Visual Refinement

This plan focuses on enhancing the visual fidelity of the Blackjack table and cards in `sharedUI`. All changes are UI-only and do not affect the domain logic.

---

## Phase 1: Table Materiality Refinement (`BlackjackScreen.kt`)

### Goal
Replace the flat table background with a rich, textured material including a "felt" feel and a 3D-looking wood rail.

### Implementation
1. **Felt Texture**: In `BlackjackScreen.kt`, update the `drawWithCache` block of the table background.
   *   Create a noise brush using a loop of low-opacity points or a tiny tiling bitmap.
   *   Draw the noise layer on top of the existing `FeltGreen` gradient using `DrawScope.drawRect`.
2. **Table Rail**: Draw a deep wood-colored linear gradient at the top (under the shoe) and the bottom of the table area.
   *   Use `OakMedium` as the core color.
   *   Add a 1dp light highlight at the top edge (`Color.White.copy(alpha = 0.2f)`) and a 2dp dark shadow at the inner edge (`Color.Black.copy(alpha = 0.3f)`).
3. **Embossed Markings**: Update the Arc and Insurance Line drawing.
   *   Draw each line twice: once with a 1px offset in `Color.Black.copy(alpha = 0.2f)` (the "emboss shadow") and then the actual `PrimaryGold` line on top.

### Verification
- Visual inspection: The table should look "fuzzy" like felt rather than smooth.
- Visual inspection: The arc and markings should look "recessed" into the felt.
- Visual inspection: The top/bottom edges of the table should have a 3D-looking wood rail.

---

## Phase 2: Card Depth & Realism (`PlayingCard.kt` & `OverlayCardTable.kt`)

### Goal
Enhance the card presence on the table with dynamic shadows and a more realistic flip.

### Implementation
1. **Dynamic Shadows**: In `OverlayCardTable.kt`, within `PositionedCardItem`, add a shadow `Modifier.graphicsLayer` that adjusts based on `isFlying`.
   *   If flying: `shadowElevation = 16.dp`, `alpha = 0.6f`.
   *   If landed: `shadowElevation = 6.dp`, `alpha = 0.9f`.
   *   Animate between states using `animateFloatAsState`.
2. **Flip Lift**: In `PlayingCard.kt`, update the `updateTransition` for `isFaceUp`.
   *   Add an `animateFloat` for `liftScale` that goes from `1.0f` to `1.08f` and back to `1.0f` as `rotationY` goes from `0` to `180`.
   *   Use `keyframes` or `Snap` to handle the mid-point peak.
3. **Refined Card Linen**: Enhance the crosshatch in `CardFace`'s `drawWithCache`.
   *   Instead of a simple grid, use `DrawScope.drawLine` with a very low-opacity `Color.Black` and `Color.White` to create a more subtle weave pattern.

### Verification
- Visual inspection: Cards should look "higher" as they are dealt from the shoe.
- Visual inspection: The card should slightly "pop" towards the camera when flipping.
- Visual inspection: The card face texture should be subtle but present.

---

## Phase 3: Integrated HUD Badges (`OverlayCardTable.kt`)

### Goal
Modernize the HUD badges while making them feel part of the physical table environment.

### Implementation
1. **Glassmorphism**: Update `HudTitleBadge` and `HudStatusBadge`.
   *   Change `containerColor` to a semi-transparent `GlassDark` or `Color(0x992A2A2A)`.
   *   Add a very thin, high-contrast white border (`0.5.dp`, `Color.White.copy(alpha = 0.2f)`).
   *   Add a subtle `shadow` with a larger blur but lower alpha to simulate a floating glass effect.
2. **Physical Shadows**: Add a subtle `drawBehind` to the badges that renders a small "contact shadow" on the felt directly below them.

### Verification
- Visual inspection: Badges should look like floating glass plates over the felt.
- Accessibility check: Text remains readable against the semi-transparent backgrounds.

---

## Overall Verification
- Run the full game: Deal, hit, stand, win.
- Verify on both Desktop and Mobile (Android).
- Confirm `./lint.sh` passes.
