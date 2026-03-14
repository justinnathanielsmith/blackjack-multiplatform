# Specification - Landscape Mode Optimization

## Overview

Optimize the landscape mode experience to match the quality and polish of the portrait mode. The current landscape implementation is functional but lacks the visual refinement, spacing, and responsiveness that makes portrait mode feel premium.

## Problem Statement

The current landscape layout (`LandscapeLayout` in `BlackjackScreen.kt:338-426`) has several issues:

1. **Cramped spacing**: Minimal vertical spacing (8.dp) between dealer and player hands
2. **Poor proportions**: Left/right column ratio (1.2f/0.8f) creates awkward visual balance
3. **Component scaling**: HandContainers, cards, and buttons don't adapt well to landscape constraints
4. **Split hand layout**: Side-by-side split hands are squeezed horizontally instead of being optimized for the available space
5. **Betting phase**: Reuses portrait layout without landscape-specific optimizations
6. **Hardcoded breakpoints**: Uses `maxHeight < 500.dp` threshold which doesn't account for aspect ratio or device type

## Goals

1. **Visual Parity**: Landscape mode should feel as polished and spacious as portrait mode
2. **Responsive Design**: Layouts should adapt intelligently to different screen sizes and aspect ratios
3. **Touch Ergonomics**: Buttons and interactive elements should be optimized for landscape thumb zones
4. **Performance**: No performance regression from layout changes
5. **Code Quality**: Maintain clean separation of concerns and composable reusability

## Functional Requirements

### FR1: Landscape-Specific HandContainer
- **FR1.1**: HandContainers in landscape mode should use compact padding and typography
- **FR1.2**: Score badges and bet chips should scale appropriately
- **FR1.3**: Status badges ("ACTIVE", "WAITING") should remain clearly visible

### FR2: Optimized Card Display
- **FR2.1**: Card overlap in `HandRow` should be reduced in landscape mode (from -40.dp to -30.dp)
- **FR2.2**: Card size should scale based on available vertical height
- **FR2.3**: Cards should maintain proper aspect ratio and readability

### FR3: Improved Layout Proportions
- **FR3.1**: Main content area should use balanced column weights (prefer 1f/1f or 1.3f/0.7f)
- **FR3.2**: Vertical spacing should be proportional to available height
- **FR3.3**: Split hands should stack vertically or use optimized horizontal layout based on available space

### FR4: Landscape Betting Phase
- **FR4.1**: Balance and bet display should use horizontal layout to maximize space
- **FR4.2**: Chip selector should be optimized for landscape with better touch targets
- **FR4.3**: Deal/Reset buttons should be positioned for easy thumb access

### FR5: Responsive Breakpoints
- **FR5.1**: Replace hardcoded height threshold with aspect ratio calculation
- **FR5.2**: Support three layout modes: portrait, landscape-compact (phones), landscape-wide (tablets/desktop)
- **FR5.3**: Ensure safe drawing padding works correctly in all orientations

### FR6: Action Button Layout
- **FR6.1**: Hit/Stand buttons should be appropriately sized for landscape
- **FR6.2**: Split/Double icons should maintain visibility without cramping
- **FR6.3**: New Game button should be properly centered and sized

## Non-Functional Requirements

### NFR1: Performance
- All animations should maintain 60fps
- Layout calculations should not cause jank during orientation changes
- No unnecessary recomposition

### NFR2: Accessibility
- Touch targets should meet minimum 48.dp size requirements
- Text should remain legible at all sizes
- Color contrast should be maintained

### NFR3: Maintainability
- Landscape-specific components should be clearly separated
- Shared logic should be extracted to avoid duplication
- Code should follow existing project patterns

## Out of Scope

- **Foldable-specific layouts**: Will handle foldables using existing responsive logic, no custom foldable detection
- **Dynamic orientation locking**: The app will support all orientations; no code to force orientation
- **Landscape-only features**: No features that only work in landscape
- **Different game rules in landscape**: Game logic remains unchanged

## Design Decisions

| Decision | Rationale |
|:---------|:----------|
| Aspect ratio over hardcoded breakpoints | `maxWidth > maxHeight && maxHeight < 500.dp` fails on tablets. Aspect ratio (width/height > 1.5) is more reliable. |
| Separate composables for compact variants | Creating `CompactHandContainer` etc. is cleaner than adding multiple `isCompact` parameters throughout existing components. |
| Three layout modes instead of two | Phones, tablets, and desktop have different constraints; three modes provide better UX across all devices. |
| Vertical split hand stacking in compact landscape | Horizontal split hands are too cramped on phones; vertical stacking works better. |
| Dedicated landscape betting layout | Betting phase has different constraints than play phase; separate layout provides better optimization. |
| Safe area padding at root level | Keep padding in `BlackjackScreen` root Column to avoid duplication in each layout variant. |

## Success Criteria

1. ✅ Landscape mode feels as polished as portrait mode
2. ✅ All interactive elements are easily reachable with thumbs in landscape orientation
3. ✅ Layouts adapt smoothly to different screen sizes (phone, tablet, desktop)
4. ✅ No visual jank or layout shifts during gameplay
5. ✅ All existing animations work correctly in landscape
6. ✅ Status messages, overlays, and effects scale appropriately
7. ✅ Code review passes with no major refactoring requests

## Testing Checklist

- [ ] Test on small phone in landscape (iPhone SE, Pixel 5)
- [ ] Test on large phone in landscape (iPhone Pro Max, Pixel 7 Pro)
- [ ] Test on tablet in landscape (iPad, Android tablet)
- [ ] Test on desktop/browser with various window sizes
- [ ] Test orientation change during active game
- [ ] Test all game states (betting, playing, insurance, terminal states)
- [ ] Test split hand scenarios
- [ ] Verify all animations play correctly
- [ ] Verify touch targets meet accessibility guidelines
- [ ] Test with different Android system fonts/display sizes
