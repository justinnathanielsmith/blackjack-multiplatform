# Plan: Improve Player Turn and Dealer Turn UI Elements

## Objective
Enhance the Player Turn ("Playing") and Dealer Turn UI elements. Specifically:
1. Center the text within the turn banners.
2. Clean up the animation and ensure rounded corners on the background glow animations.
3. Make the appearance and idle animations "juicier" (more bouncy and springy).

## Key Files & Context
- `sharedUI/src/ui/components/GameStatusMessage.kt` (Handles the main "Playing" and "Dealer Turn" banners, pulse, and shimmer animations)
- `sharedUI/src/ui/screens/BlackjackScreen.kt` (Handles the AnimatedVisibility entrance/exit transitions for the banner)
- `sharedUI/src/ui/components/BlackjackHandContainer.kt` (Handles the active turn indicator glow and `TitleBadge`/`StatusBadge`)

## Changes

1. **Center Text in GameStatusMessage**:
   - Add `contentAlignment = Alignment.Center` to the root `Box` in `GameStatusMessage`.
   - Add `textAlign = TextAlign.Center` to the `Text` inside the message.

2. **Clean Up and Round Corners on Animation**:
   - In `GameStatusMessage.kt`, the glow is currently drawn behind the clipped bounds using `drawRect(brush = glowBrush)`. This causes sharp corners on the animation if it overflows. We will change this to `drawRoundRect(brush = glowBrush, cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()))` to cleanly round the edges of the glow.

3. **Make the Animation Juicier**:
   - Update `GameStatusMessage`'s `pulseScale` animation to be slightly faster and more responsive.
   - Update `BlackjackScreen.kt` to use a bouncy `spring` animation for `scaleIn` when the `GameStatusMessage` appears, instead of a plain linear scale.

4. **Enhance Hand Badges (Optional Context)**:
   - Ensure the `TitleBadge` and `StatusBadge` in `BlackjackHandContainer.kt` also have their text centered via `Alignment.Center` and `TextAlign.Center` to be thorough across all turn-related UI elements.

## Verification
- Once approved, we will build the JVM target (`./amper build -p jvm`) to verify the syntax.
- Visual check (by the developer) will show the banner now has a bouncy entrance, a cleaner rounded glow without sharp edge bleeding, and perfectly centered text.