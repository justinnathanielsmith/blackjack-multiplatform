# Bolt Journal — Critical Learnings

## 2026-04-04 - drawWithCache cache-scope reads invalidate the cache every frame
**Learning:** Reading an animated `Float` state (e.g. `shimmerX` from `rememberInfiniteTransition`) inside the `drawWithCache` cache lambda body causes the cache to be invalidated and re-executed on every animation frame — defeating the purpose of caching. Brushes or objects created there are allocated per-frame, not per-size-change. This was found in `GameStatusMessage.kt`'s shimmer effect.
**Action:** Keep animated reads (values that change every frame) inside `onDrawWithContent` / `onDrawBehind` as plain computations (Float maths, Offset positioning). Only create `Brush`, `Path`, `Paint`, and other expensive objects in the cache scope where they depend solely on `size` and stable state values.
