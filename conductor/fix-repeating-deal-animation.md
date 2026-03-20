# Objective
Fix the bug where the dealing animation repeats unintentionally, such as when new cards are added to a hand or when the dealer's hole card flips.

# Root Cause Analysis
The issue stems from two problems in `sharedUI/src/ui/components/OverlayCardTable.kt`:

1. **Missing Stable Keys**: The `OverlayCardTable` iterates over `tableLayout.cardSlots` using a `forEach` loop without providing a Compose `key`. When a new card is dealt (e.g., to the first player hand), the number of cards in that hand increases, shifting the sequence of all subsequent cards in the `cardSlots` list. Without a key, Compose matches components by their positional order, causing subsequent cards to receive the `Card` data of their new sequence index. 
2. **Fragile `remember` Blocks**: Inside `PositionedCardItem`, the `Animatable` instances for X, Y, scale, and rotation use `slot.card` as their `remember` key (e.g., `remember(slot.card) { Animatable(...) }`). When `slot.card` changes—either because of the list shift described above or because the dealer's hole card toggles its `isFaceDown` state—the `remember` block resets. This throws away the card's current visual state on the table and creates a new `Animatable` starting back at the shoe (`slot.startOffset`), resulting in a repeated dealing animation.

# Implementation Steps

1. **Add Stable Keys**:
   - In `sharedUI/src/ui/components/OverlayCardTable.kt`, wrap the `PositionedCardItem` call inside the `forEach` loop with a `key` block using the hand and card indices:
     ```kotlin
     key(slot.handIndex, slot.cardIndex) {
         PositionedCardItem(
             slot = slot,
             // ...
         )
     }
     ```
   - This ensures each physical card slot on the table retains its identity regardless of how many new slots are inserted before it.

2. **Decouple Animatable State from Card Value**:
   - Inside the `PositionedCardItem` composable, remove `slot.card` from the `remember` blocks. Since a specific physical slot (identified by the key above) doesn't change its starting origin once it enters the composition, we only need to initialize the animatables once:
     ```kotlin
     val currentX = remember { Animatable(slot.startOffset.x) }
     val currentY = remember { Animatable(slot.startOffset.y) }
     val currentScale = remember { Animatable(0.5f) }
     val currentRotation = remember { Animatable(if (slot.isDealer) -45f else 45f) }
     ```

3. **Preserve Reveal & Movement Dynamics**:
   - The `LaunchedEffect(slot.card, slot.centerOffset)` remains appropriate. Because the `Animatable` instances are no longer reset, when `slot.card` changes (e.g., `isFaceDown` becomes false), the `LaunchedEffect` will seamlessly animate from the card's *current* resting position rather than teleporting back to the shoe. This properly preserves the scale "pop" and rotation fix when the dealer hole card is revealed.

# Verification & Testing
- **Multi-Hand Play**: Play a game with multiple hands. Hit on the first hand and verify that cards in the second hand do not animate from the shoe again.
- **Hole Card Reveal**: Complete a player turn and observe the dealer's hidden card. When it flips face-up, verify that it scales and rotates in place without flying from the shoe again.