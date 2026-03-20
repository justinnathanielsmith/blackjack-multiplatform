# Objective
Refactor the Blackjack dealing animation to use a fully declarative Compose approach. By removing the imperative hand-off between a moving `FlyingCard` and a static `PositionedCardItem`, we simplify state management, eliminate visual jitter, and allow cards to seamlessly reposition themselves during layout changes.

# Key Files
- `sharedUI/src/ui/components/CardTablePositioner.kt`
- `sharedUI/src/ui/components/OverlayCardTable.kt`
- `sharedUI/src/ui/screens/BlackjackScreen.kt`
- `sharedUI/src/ui/effects/FlyingCard.kt`

# Implementation Steps

1. **Update `CardSlotLayout` and `CardTablePositioner`**:
   - In `CardTablePositioner.kt`, add `val startOffset: Offset` to `CardSlotLayout`.
   - Update `computeTableLayout` and `computeZone` to accept a `shoePosition: Offset` and pass it to `CardSlotLayout` as its `startOffset`.

2. **Gut `DealAnimationRegistry`**:
   - In `sharedUI/src/ui/effects/FlyingCard.kt`, rename the file or just repurpose the registry. We will delete the `FlyingCard` composable, `FlyingCardInstance` data class, and completely empty out `DealAnimationRegistry`'s complex states (`flyingCards`, `landedCards`, `positionedCards`, `requestDeal`, `markLanded`, etc.).
   - The stripped-down `DealAnimationRegistry` (which we can keep naming as such, or `TableLayoutState`) will *only* hold `tableLayout`, `gameplayAreaOffset`, and `overlayOffset`.
   - Delete `FlyingCard.kt`'s file logic entirely or rename it to `TableLayoutState.kt` to house the new simplified state holding the layout. 

3. **Refactor `OverlayCardTable.kt`**:
   - Change `OverlayCardTable` to map directly over `tableLayout.cardSlots`.
   - Update `PositionedCardItem` signature to take `CardSlotLayout` instead of `PositionedCardEntry`.
   - Inside `PositionedCardItem`, initialize animatable properties (`currentX`, `currentY`, `currentScale`, `currentRotation`) using `remember(entry.card) { Animatable(...) }` starting with `entry.startOffset`, a scale of 0.5f, and an initial rotation of -45f.
   - Use `LaunchedEffect(entry.card, entry.centerOffset)` to trigger the animations using `spring()` to fly the card from the start position to `entry.centerOffset` and `entry.rotationZ`, alongside a slight scale "pop" effect.
   - Combine the local layout coordinates (`currentX.value + coordOffsetX`) to correctly position the animated box.
   - Dynamically calculate the shadow elevation inside `PositionedCardItem` depending on whether the `currentX/Y` animatables are currently running.

4. **Update `BlackjackScreen.kt`**:
   - Remove the old `LaunchedEffect(allCards)` block that manually iterated over slots, checked `isLanded`, and dispatched `FlyingCardInstance` requests.
   - Replace it with a simple `LaunchedEffect(allCards, areaW, areaH, density, shoePosition.value)` that simply computes the new `TableLayout` (passing the `shoePosition`) and assigns it to the layout state.
   - Remove the `dealRegistry.flyingCards.forEach` rendering loop inside the overlay Box. The UI will now purely rely on `OverlayCardTable` rendering the slots.

# Verification & Testing
- Start a new game and verify cards are smoothly animated from the shoe position directly into their final positions without any frame skips or missing cards.
- Hit a few times to ensure the declarative layout automatically pushes existing cards over without any visual glitching.
- Check dealer hole card flip logic to ensure the rotation and scale pop function correctly when revealed.
- Confirm resizing the screen or updating layout gracefully shifts all cards utilizing the existing `Animatable` targets.