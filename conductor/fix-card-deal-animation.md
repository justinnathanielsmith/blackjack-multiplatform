# Plan: Correct Deal Animation Delay and Centering

Correct the deal animation system to ensure cards "land" promptly and are correctly positioned in the hand area, especially when scaled.

## Objective
Fix the reported delay where `PlayingCard` components in the hand area don't appear until much later than expected. This is caused by over-precise animation settle thresholds and a fixed slow spring stiffness that ignores the requested animation duration. Additionally, correct a centering calculation error in the `FlyingCard` effect that causes visual jumps on landing.

## Key Files & Context
- `sharedUI/src/ui/effects/FlyingCard.kt`: Main logic for the flying card animation.
- `sharedUI/src/ui/components/PlayingCard.kt`: Manages individual card visibility and requests deals.
- `sharedUI/src/ui/screens/BlackjackScreen.kt`: Instantiates the `DealAnimationRegistry` and renders the overlay.

## Analysis of Issues
1.  **Settle Delay**: `Animatable(startValue)` defaults to a `visibilityThreshold` of `0.01f`. When animating screen pixels (e.g., 1000px), a spring animation with `StiffnessLow` takes over 1.2 seconds to decay to 0.01px, even though it is visually at rest after ~300ms. Since `PlayingCard` waits for the animation to "officially" end (`jobs.joinAll()`) before becoming visible, there is a significant "dead" window where neither the flying card nor the hand card is interactive or showing final state (like score badges).
2.  **Centering Bug**: The `translationX/Y` in `FlyingCard` subtracts `baseCardWidthPx * visualScale / 2`. However, since the box is scaled around its center, its center remains at `baseCardWidthPx / 2`. The correct translation to align centers is `targetCenter - unscaledCenter`. Multiplying by `visualScale` offsets the center by `(1 - visualScale) * width / 2`.
3.  **Non-Reactive Overlay Offset**: `DealAnimationRegistry.overlayOffset` is a regular `var`, so `FlyingCard` doesn't update if the game layout shifts (e.g., window resize).
4.  **Ignored Animation Duration**: `PlayingCard` has an `animationDurationMs` parameter that is never used; `FlyingCard` uses a fixed `StiffnessLow` regardless of whether a fast or slow (dealer hole card) deal is requested.

## Proposed Changes

### 1. Update `DealAnimationRegistry`
- Convert `overlayOffset` to a `mutableStateOf(Offset.Zero)` to ensure reactivity.

### 2. Update `FlyingCardInstance` Data Class
- Add `durationMs: Int` to carry the requested timing.

### 3. Update `FlyingCard` Effect (`FlyingCard.kt`)
- Use appropriate `visibilityThreshold` for pixel coordinates (`1f` for X/Y, `0.1f` for rotation).
- Derivation: Set `stiffness` based on `durationMs` so the spring settles visually within the requested time.
- Fix the `translationX/Y` math by using `baseCardWidthPx / 2` (unscaled center) for the offset.

### 4. Update `PlayingCard` (`PlayingCard.kt`)
- Pass its `animationDurationMs` parameter into the `FlyingCardInstance`.

## Verification & Testing
- **Visual Check**: Observe card deals at different scales (e.g., multi-hand vs single hand). There should be no "jump" when the flying card is replaced by the playing card.
- **Responsiveness Check**: Verify that hand score badges and "active" glows appear immediately after the card reaches its destination.
- **Slow Deal Check**: Verify that the dealer's hole card (if `isSlowReveal` is true) correctly uses the longer duration.

## Implementation Plan

### Step 1: Update `FlyingCardInstance` and `DealAnimationRegistry`
In `sharedUI/src/ui/effects/FlyingCard.kt`:
- Add `durationMs: Int` to `FlyingCardInstance`.
- Change `var overlayOffset: Offset = Offset.Zero` to `var overlayOffset by mutableStateOf(Offset.Zero)`.

### Step 2: Update `PlayingCard` to pass duration
In `sharedUI/src/ui/components/PlayingCard.kt`:
- Update `registry.requestDeal(...)` to include `durationMs = animationDurationMs`.

### Step 3: Refactor `FlyingCard` animation logic
In `sharedUI/src/ui/effects/FlyingCard.kt`:
- Update `remember` blocks for `currentX` and `currentY` to set `visibilityThreshold = 1f`.
- Update `rotationAnim` to set `visibilityThreshold = 0.1f`.
- In `LaunchedEffect`, calculate `stiffness` from `instance.durationMs`.
  - Formula: `stiffness = ( (ln(1000) / (0.65 * (durationMs/1000.0))) )^2`.
  - Simpler alternative: Map `durationMs` to `StiffnessMedium` (300ms) or `StiffnessLow` (900ms) or an interpolated value.
- Fix `translationX` and `translationY` in `graphicsLayer`.

### Step 4: Fix centering math in `FlyingCard`
```kotlin
// From:
translationX = currentX.value - baseCardWidthPx * visualScale / 2 - overlayOffset.x
// To:
translationX = currentX.value - baseCardWidthPx / 2 - overlayOffset.x
```
