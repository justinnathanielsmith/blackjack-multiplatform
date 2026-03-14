# Implementation Plan - Landscape Mode Optimization

## Prerequisites

- ✅ `ui-juice` track completed - All animations and visual polish in place
- ✅ `advanced-rules` track completed - Split hand functionality implemented
- ✅ `betting-system` track completed - Betting phase and chip management working

## Implementation Phases

### Phase 1: Landscape-Specific HandContainer Sizing

**Goal**: Create compact variants of UI components optimized for landscape's limited vertical space.

#### Step 1.1: Add `isCompact` parameter to HandContainer
- **File**: `sharedUI/src/ui/components/HandContainer.kt`
- **Changes**:
  - Add `isCompact: Boolean = false` parameter
  - When `isCompact = true`:
    - Reduce vertical padding from `padding(horizontal = 16.dp, vertical = 24.dp)` to `padding(horizontal = 12.dp, vertical = 16.dp)`
    - Reduce corner radius from `24.dp` to `16.dp`
    - Use `MaterialTheme.typography.labelSmall` instead of `labelMedium` for title
    - Scale score badge size down slightly
    - Reduce bet chip size by 15%

#### Step 1.2: Add `isCompact` parameter to HandRow
- **File**: `sharedUI/src/ui/components/HandRow.kt`
- **Changes**:
  - Add `isCompact: Boolean = false` parameter
  - When `isCompact = true`:
    - Reduce card overlap from `-40.dp` to `-30.dp`
    - This provides better readability in cramped landscape layouts

#### Step 1.3: Add `cardScale` parameter to PlayingCard
- **File**: `sharedUI/src/ui/components/PlayingCard.kt`
- **Changes**:
  - Add `scale: Float = 1f` parameter
  - Apply scale to card size using `Modifier.scale(scale)`
  - Pass `scale = 0.85f` in landscape compact mode

**Testing**: Verify HandContainers render correctly in both compact and normal modes.

---

### Phase 2: Improved Landscape Layout Proportions

**Goal**: Rebalance the landscape layout to use available space more effectively.

#### Step 2.1: Update LandscapeLayout column weights
- **File**: `sharedUI/src/ui/screens/BlackjackScreen.kt`
- **Location**: `LandscapeLayout` function (lines 338-426)
- **Changes**:
  - Change left column from `Modifier.weight(1.2f)` to `Modifier.weight(1f)`
  - Change right column from `Modifier.weight(0.8f)` to `Modifier.weight(1f)`
  - Increase horizontal spacing from `24.dp` to `32.dp`

#### Step 2.2: Improve vertical spacing in landscape
- **Changes**:
  - Change dealer/player spacer from `Spacer(modifier = Modifier.height(8.dp))` to `Spacer(modifier = Modifier.height(16.dp))`
  - Add `verticalArrangement = Arrangement.SpaceEvenly` to left column
  - Remove fixed `height(80.dp)` anchor box in right column, use flexible spacing

#### Step 2.3: Optimize split hand layout for landscape
- **Current issue**: Split hands use `Row(horizontalArrangement = Arrangement.spacedBy(8.dp))` which creates cramped horizontal layout
- **Changes**:
  - Detect available width in landscape
  - If width is insufficient (phone landscape), stack split hands vertically with `Column`
  - If width is sufficient (tablet landscape), keep horizontal `Row` but increase spacing to `16.dp`
  - Threshold: `maxWidth < 700.dp` → vertical, else horizontal

**Testing**: Verify split hands display correctly on phone and tablet landscape.

---

### Phase 3: Landscape Betting Phase Screen

**Goal**: Create a dedicated landscape-optimized betting phase layout.

#### Step 3.1: Add landscape layout detection to BettingPhaseScreen
- **File**: `sharedUI/src/ui/screens/BettingPhaseScreen.kt`
- **Changes**:
  - Add `BoxWithConstraints` wrapper to detect orientation
  - When `maxWidth > maxHeight`: use new `LandscapeBettingLayout`
  - When `maxHeight >= maxWidth`: use existing portrait layout

#### Step 3.2: Create LandscapeBettingLayout composable
- **File**: `sharedUI/src/ui/screens/BettingPhaseScreen.kt`
- **Structure**:
```kotlin
@Composable
private fun LandscapeBettingLayout(
    state: GameState,
    component: BlackjackComponent,
    audioService: AudioService,
    flyingChips: SnapshotStateList<FlyingChip>,
    onBetDisplayPositioned: (Offset) -> Unit,
)
```
- **Layout**:
  - Use `Row` as root with two equal-weight columns
  - Left column: Balance/bet info card (compact, centered)
  - Right column: Chip selector + Deal/Reset buttons stacked vertically
  - Reduce overall padding to fit better

#### Step 3.3: Optimize chip selector for landscape
- **Changes**:
  - Chip selector already uses horizontal `Row`, no structural changes needed
  - Ensure touch targets remain 48.dp minimum
  - Center chip selector in its container

**Testing**: Verify betting phase looks good in landscape on phone and tablet.

---

### Phase 4: Dynamic Responsive Breakpoints

**Goal**: Replace hardcoded breakpoints with intelligent aspect ratio detection.

#### Step 4.1: Define layout mode helper
- **File**: `sharedUI/src/ui/screens/BlackjackScreen.kt`
- **Add at top level**:
```kotlin
enum class LayoutMode {
    PORTRAIT,
    LANDSCAPE_COMPACT,  // Phone landscape
    LANDSCAPE_WIDE      // Tablet/desktop landscape
}

@Composable
private fun BoxWithConstraintsScope.detectLayoutMode(): LayoutMode {
    val aspectRatio = maxWidth / maxHeight
    return when {
        maxHeight > maxWidth -> LayoutMode.PORTRAIT
        aspectRatio > 1.8f -> LayoutMode.LANDSCAPE_WIDE  // Very wide (desktop/tablet)
        else -> LayoutMode.LANDSCAPE_COMPACT  // Phone landscape
    }
}
```

#### Step 4.2: Update BlackjackScreen to use LayoutMode
- **Changes**:
  - Replace `isLandscape` and `isCompactHeight` booleans with single `layoutMode`
  - Pass `layoutMode` to child composables instead of `isCompact`
  - Update `BettingPhaseScreen` call to use `layoutMode`

#### Step 4.3: Update component signatures
- **Files**: `HandContainer.kt`, `HandRow.kt`, `GameActions.kt`, `GameStatusMessage.kt`
- **Changes**:
  - Replace `isCompact: Boolean` with `layoutMode: LayoutMode = LayoutMode.PORTRAIT`
  - Update internal logic to check `layoutMode == LayoutMode.LANDSCAPE_COMPACT`
  - In `LANDSCAPE_WIDE` mode, use slightly larger sizing than `LANDSCAPE_COMPACT` but smaller than `PORTRAIT`

**Testing**: Test on various screen sizes and verify correct mode is detected.

---

### Phase 5: Polish & Testing

**Goal**: Ensure all edge cases are handled and the experience is smooth.

#### Step 5.1: Verify animations work in landscape
- Test all animation sequences:
  - [ ] Card deal animation
  - [ ] Hole card flip
  - [ ] Chip toss animation
  - [ ] Balance counter roll
  - [ ] Confetti effect
  - [ ] Button press feedback
  - [ ] Status message transitions
- Verify no clipping or overflow issues

#### Step 5.2: Verify overlays scale correctly
- **GameStatusMessage**: Should scale appropriately in compact mode
- **InsuranceOverlay**: Should fit within landscape constraints
- **ConfettiEffect**: Should spawn particles across full screen width
- **Flash effect**: Should cover entire screen

#### Step 5.3: Test orientation changes
- Start game in portrait → rotate to landscape during:
  - [ ] Betting phase
  - [ ] Playing phase
  - [ ] Insurance decision
  - [ ] Terminal state (win/loss/push)
- Verify no crashes, layout shifts, or broken animations

#### Step 5.4: Accessibility audit
- Verify all buttons meet 48.dp minimum touch target
- Verify text remains legible at all sizes
- Test with Android system font scaling (small, default, large, largest)
- Test with developer "Show layout bounds" enabled

#### Step 5.5: Performance profiling
- Run Layout Inspector to check recomposition counts
- Verify no excessive recompositions during animations
- Check memory usage during orientation changes
- Ensure 60fps is maintained during confetti/animations

#### Step 5.6: Code cleanup
- Remove any debug logging
- Ensure all `isCompact` references are replaced with `layoutMode`
- Run `./ktlint --format` to fix formatting
- Run `./lint.sh` to verify no violations

**Testing**: Complete full regression test across all devices and orientations.

---

## Implementation Order

1. **Phase 1** → Foundation for compact layouts
2. **Phase 4** → Breakpoint system (needed before Phase 2 & 3 optimizations)
3. **Phase 2** → Improve play phase landscape layout
4. **Phase 3** → Improve betting phase landscape layout
5. **Phase 5** → Polish, test, and ship

## Success Metrics

- [ ] All layouts render correctly in portrait, landscape-compact, and landscape-wide modes
- [ ] No visual jank or layout shifts during orientation changes
- [ ] All animations play at 60fps
- [ ] Touch targets meet accessibility guidelines
- [ ] Code passes ktlint and detekt checks
- [ ] Manual testing completed on at least 3 different screen sizes

## Rollback Plan

If landscape optimization introduces regressions:
1. Revert changes to `BlackjackScreen.kt` layout logic
2. Keep `LayoutMode` enum for future use
3. Document issues in track `index.md`
4. Create follow-up tasks for specific problems

## Notes

- Maintain backward compatibility with existing portrait layouts
- Don't modify core game state machine
- All changes are purely presentational in `sharedUI` module
- Follow existing animation patterns from `ui-juice` track
