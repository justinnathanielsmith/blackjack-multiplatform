## 2026-04-10 - DrawWithCache Brush Allocation Trap

**Learning:** Reading `Animatable.value` or any frequently changing state inside a `drawWithCache { ... }` block (outside the `onDrawBehind` lambda) causes the entire block to re-run on every frame. If the block contains `Brush.radialGradient` or other heavy object allocations, it generates massive GC pressure during animations.
**Action:** Always move `Animatable.value` reads into the `onDrawBehind` scope. Pre-compute brushes with a fixed size/center in `drawWithCache`, then use `translate`, `scale`, or the `alpha` parameter of `drawRect`/`drawCircle` to apply the animation state without re-allocating the brush.

## 2026-04-07 - Layout Phase Allocation Traps

**Learning:** `CasinoTableLayout.kt`'s `layout` phase executes continuously on every frame during positional animations (e.g., card deals driven by flight progress lerping). Using collection operators like `.filter { ... }` inside the `layout { }` block silently allocates new `ArrayList`s and iterators 60 times a second per child type, causing severe GC pressure.
**Action:** Always use zero-allocation, index-based `for (i in list.indices)` loops inside `layout { }` and `drawBehind { }` scopes. Never use chained collection operators (`.filter`, `.map`) in these hot paths.
