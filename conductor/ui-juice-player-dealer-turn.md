# Plan: UI Juice - Betting Phase & Turn Animations

## Objective
Enhance the "Juice" and physical realism of the game UI.
**Part 1: Betting Phase Integration (NEW)**: Upgrade the betting experience to feel like an integrated part of the physical casino table, rather than a floating modal menu.
**Part 2: Player/Dealer Turns (PREVIOUS)**: Enhance the turn banners, hand badges, and card dealing animations (3D Casino Toss).

## Key Files & Context
- `sharedUI/src/ui/screens/BettingPhaseScreen.kt` (Betting layout, modal dimming)
- `sharedUI/src/ui/components/BettingSlot.kt` (Betting spots on the table)
- `sharedUI/src/ui/components/GameStatusMessage.kt` (Turn banners)
- `sharedUI/src/ui/screens/BlackjackScreen.kt` (Transitions, table drawing)
- `sharedUI/src/ui/components/PlayingCard.kt` (Card deal animations)

## Changes

### Phase 1: Integrated Betting Experience
1. **Remove Modal Dimming**:
   - In `BettingPhaseScreen.kt`, remove `.background(Color.Black.copy(alpha = 0.5f))` from the root `Box`.
   - The table felt and drawn arcs should remain vibrant and fully visible during betting.

2. **Remove Menu Header**:
   - In `BettingPhaseScreen.kt`, remove the floating `Text` displaying the uppercase "BETTING" status. The visual context (chips, spots) is sufficient.

3. **Ground the Controls**:
   - In `BettingPhaseScreen.kt`, modify the bottom control panel (containing `ChipSelector` and `BettingActions`).
   - Remove the `GlassDark` background, the rounded border, and clipping.
   - Adjust spacing and let the chips/buttons sit directly on the dark vignette at the bottom of the screen, acting as the table rail.

4. **Refine Betting Spots (Table Markings)**:
   - In `BettingSlot.kt`, modify the stroke drawing so the spots look more like paint on felt rather than glowing UI rings when inactive. Ensure they look physically present on the table.
   - Lower the vertical offset of the central slots in `BettingPhaseScreen.kt` slightly if needed to align perfectly with the painted table arc in `BlackjackScreen.kt`.

### Phase 2: Player/Dealer Turn & Card Dealing (Previous Scope)
1. **Center Text in GameStatusMessage**:
   - Add `contentAlignment = Alignment.Center` to the root `Box` in `GameStatusMessage`.
   - Add `textAlign = TextAlign.Center` to the `Text` inside the message.

2. **Clean Up and Round Corners on Animation**:
   - In `GameStatusMessage.kt`, use `drawRoundRect(brush = glowBrush, cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()))`.

3. **Make the Banner Animation Juicier**:
   - Update `GameStatusMessage`'s `pulseScale` animation to be faster.
   - Update `BlackjackScreen.kt` to use a bouncy `spring` for `scaleIn` when the `GameStatusMessage` appears.

4. **Juice Up the Deal Animation (3D Casino Toss)**:
   - In `PlayingCard.kt`, add `offsetX`, `offsetY`, `dealScale`, and `dealRotationZ` animatables.
   - Deal from top-right (`offsetX = 300f`, `offsetY = -400f`).
   - Use `spring` for offset and rotation. Scale up to `1.2f` mid-air, then down to `1.0f`.

## Verification
- Build the JVM target (`./amper build -p jvm`).
- Launch the app and verify the betting phase feels like interacting with the physical table (no dimming, integrated rail controls).
- Verify the cards deal with the new 3D toss animation and turn banners are bouncy and centered.