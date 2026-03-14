# Track Index - Landscape Mode Optimization

Bring landscape mode UI quality up to par with the polished portrait mode. Optimize layouts, spacing, and responsiveness for phones, tablets, and desktop in landscape orientation.

## Documentation
- [**Specification**](./spec.md): Functional requirements, responsive breakpoints, and design decisions.
- [**Implementation Plan**](./plan.md): Step-by-step development guide with detailed component changes.

## Status

- **Phase 1: Landscape-Specific HandContainer Sizing** (compact variants for constrained layouts): 📅 Pending
- **Phase 2: Improved Landscape Layout Proportions** (rebalanced columns and spacing): 📅 Pending
- **Phase 3: Landscape Betting Phase Screen** (dedicated landscape betting layout): 📅 Pending
- **Phase 4: Dynamic Responsive Breakpoints** (aspect ratio-based layout detection): 📅 Pending
- **Phase 5: Polish & Testing** (animations, accessibility, performance): 📅 Pending

## Prerequisites

- `ui-juice` ✅ — All animations and visual polish must work correctly in landscape
- `advanced-rules` ✅ — Split hand display needs landscape optimization
- `betting-system` ✅ — Betting phase layout needs landscape variant

## Key Decisions

| Decision | Rationale |
| :--- | :--- |
| Three layout modes: portrait, landscape-compact, landscape-wide | Phones, tablets, and desktop have different constraints; three modes provide better UX across all devices. |
| Aspect ratio over hardcoded height breakpoints | `maxHeight < 500.dp` fails on tablets; aspect ratio (width/height > 1.5) is more reliable. |
| Separate landscape betting layout | Betting phase has different constraints than play phase; separate layout enables better optimization. |
| Vertical split hand stacking in compact landscape | Horizontal split hands are too cramped on phones; vertical stacking works better. |
| Component-level `layoutMode` parameter | Pass `LayoutMode` enum to components instead of multiple boolean flags for cleaner API. |
| Keep safe area padding at root | Avoid duplicating safe area padding in each layout variant. |

## Implementation Order

Following the plan in `plan.md`:

1. ✅ **Documentation** (spec.md, plan.md, index.md)
2. 📅 **Phase 1** - Foundation for compact layouts
3. 📅 **Phase 4** - Breakpoint system (enables Phase 2 & 3)
4. 📅 **Phase 2** - Improve play phase landscape
5. 📅 **Phase 3** - Improve betting phase landscape
6. 📅 **Phase 5** - Polish, test, ship

## Testing Checklist

- [ ] Small phone landscape (iPhone SE, Pixel 5)
- [ ] Large phone landscape (iPhone Pro Max, Pixel 7 Pro)
- [ ] Tablet landscape (iPad, Android tablet)
- [ ] Desktop/browser with various window sizes
- [ ] Orientation change during active game
- [ ] All game states (betting, playing, insurance, terminal)
- [ ] Split hand scenarios in landscape
- [ ] All animations play correctly
- [ ] Touch targets meet accessibility guidelines
- [ ] Safe area padding works correctly

## Success Criteria

- ✅ Landscape mode feels as polished as portrait mode
- ✅ Layouts adapt smoothly to different screen sizes
- ✅ All interactive elements easily reachable with thumbs in landscape
- ✅ No visual jank during orientation changes
- ✅ All animations work correctly in landscape
- ✅ Code passes linting and review
