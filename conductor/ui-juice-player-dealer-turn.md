# Plan: Improve Player Turn, Dealer Turn UI, and Auto Deal Animations

## Objective
Enhance the Player Turn ("Playing") and Dealer Turn UI elements, as well as the Auto-Deal card animations. Specifically:
1. Center the text within the turn banners.
2. Clean up the animation and ensure rounded corners on the background glow animations.
3. Make the banner appearance and idle animations "juicier" (more bouncy and springy).
4. Upgrade the card dealing animation to a "3D Casino Toss" (cards fly from top-right, scale up/down for depth, spin on Z-axis, and land with a spring bounce).

## Key Files & Context
- `sharedUI/src/ui/components/GameStatusMessage.kt` (Handles the main "Playing" and "Dealer Turn" banners, pulse, and shimmer animations)
- `sharedUI/src/ui/screens/BlackjackScreen.kt` (Handles the AnimatedVisibility entrance/exit transitions for the banner)
- `sharedUI/src/ui/components/BlackjackHandContainer.kt` (Handles the active turn indicator glow and `TitleBadge`/`StatusBadge`)
- `sharedUI/src/ui/components/PlayingCard.kt` (Handles the card deal, flip, and near-miss animations)

## Changes

1. **Center Text in GameStatusMessage**:
   - Add `contentAlignment = Alignment.Center` to the root `Box` in `GameStatusMessage`.
   - Add `textAlign = TextAlign.Center` to the `Text` inside the message.

2. **Clean Up and Round Corners on Animation**:
   - In `GameStatusMessage.kt`, the glow is currently drawn behind the clipped bounds using `drawRect(brush = glowBrush)`. This causes sharp corners on the animation if it overflows. We will change this to `drawRoundRect(brush = glowBrush, cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()))` to cleanly round the edges of the glow.

3. **Make the Banner Animation Juicier**:
   - Update `GameStatusMessage`'s `pulseScale` animation to be slightly faster and more responsive.
   - Update `BlackjackScreen.kt` to use a bouncy `spring` animation for `scaleIn` when the `GameStatusMessage` appears, instead of a plain linear scale.

4. **Enhance Hand Badges (Optional Context)**:
   - Ensure the `TitleBadge` and `StatusBadge` in `BlackjackHandContainer.kt` also have their text centered via `Alignment.Center` and `TextAlign.Center` to be thorough across all turn-related UI elements.

5. **Juice Up the Deal Animation (3D Casino Toss)**:
   - In `PlayingCard.kt`, add an `offsetX` animatable starting from `300f` (representing the shoe on the right), and update `offsetY` to start from `-400f` (top of screen) regardless of dealer/player so all cards originate from the top-right shoe.
   - Add a `dealScale` animatable that starts at `0.5f`.
   - Add a `dealRotationZ` animatable starting at `45f` or `-45f` depending on the target.
   - Update the dealing `LaunchedEffect(card)` to use `async` to launch concurrent `animateTo` calls on `offsetX`, `offsetY`, `dealScale`, and `dealRotationZ`.
   - The offset and rotation will use a `spring` spec for a bouncy landing. The scale will use `keyframes` or sequence: animate to `1.2f` (mid-air) then `spring` down to `1.0f`.
   - Apply these values to the `graphicsLayer` of the card (`translationX`, `translationY`, `scaleX`, `scaleY`, `rotationZ`).

## Verification
- Once approved, we will build the JVM target (`./amper build -p jvm`) to verify the syntax.
- Visual check (by the developer) will show the banner now has a bouncy entrance, a cleaner rounded glow without sharp edge bleeding, and perfectly centered text.
- Visual check will show cards dealing in a satisfying arc from the top-right of the screen with a spin and springy snap into place.