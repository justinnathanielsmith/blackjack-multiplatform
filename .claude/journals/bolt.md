# Bolt Journal

_Critical performance learnings and optimization history for the Blackjack project._

---

## 2026-04-10 - DrawWithCache Brush Allocation Trap
**Learning:** Reading `Animatable.value` or any frequently changing state inside a `drawWithCache { ... }` block (outside the `onDrawBehind` lambda) causes the entire block to re-run on every frame. If the block contains `Brush.radialGradient` or other heavy object allocations, it generates massive GC pressure.
**Action:** Always move `Animatable.value` reads into the `onDrawBehind` scope. Pre-compute brushes with a fixed size/center in `drawWithCache`, then use `translate`, `scale`, or the `alpha` parameter to apply the animation state.

## 2026-04-07 - Layout Phase Allocation Traps
**Learning:** `CasinoTableLayout.kt`'s `layout` phase executes continuously during positional animations. Using collection operators like `.filter { ... }` inside the `layout { }` block silently allocates new `ArrayList`s and iterators 60 times a second.
**Action:** Always use zero-allocation, index-based `for (i in list.indices)` loops inside `layout { }` and `drawBehind { }` scopes. Never use chained collection operators (`.filter`, `.map`) in these hot paths.

## 2026-04-04 - drawWithCache cache-scope reads invalidate the cache every frame
**Learning:** Reading an animated `Float` state (e.g. `shimmerX` from `rememberInfiniteTransition`) inside the `drawWithCache` cache lambda body causes the cache to be invalidated and re-executed on every animation frame.
**Action:** Keep animated reads inside `onDrawWithContent` / `onDrawBehind` as plain computations. Only create `Brush`, `Path`, `Paint`, and other expensive objects in the cache scope where they depend solely on `size` and stable state values.

## 2026-04-02 - Do not optimize inline map with explicit ArrayLists for small lists
**Learning:** In Kotlin, standard collection extension functions like `map` are `inline` and compile down to highly optimized loops. Replacing them with manual `for` loops and pre-sized collections for small item counts (e.g., 1-4 items) is a micro-optimization that reduces code readability without measurable performance gains.
**Action:** Avoid rewriting simple `.map` calls for small lists into manual loop accumulator logic, unless profiling dictates that an intermediate collection is actually causing GC pressure on a hot path.

## 2026-03-31 - AutoDealIcon Recomposition Loop
**Learning:** Animated states (like infinite transitions) that are read directly in the composition phase to construct standard modifiers (like `Modifier.border`) force the composable to completely recompose on every animation frame.
**Action:** Move the state read directly into the draw phase (e.g., `Modifier.drawWithCache { onDrawWithContent { ... } }`).

## 2026-03-31 - Avoiding Intermediate List Allocations in State Machine
**Learning:** For `kotlinx.collections.immutable.PersistentList`, using `.map { ... }.toPersistentList()` to update elements creates intermediate `ArrayList` allocations and extra iterations.
**Action:** Use `.mutate { builder -> ... }` with an indexed loop to modify the persistent list builder in-place.

## 2026-03-27 - O(0) Main-Thread Relayout for Positional Animations
**Learning:** Using `Modifier.offset { ... }` with animated state values triggers the layout phase on every frame.
**Action:** Replace `Modifier.offset` with `Modifier.graphicsLayer { translationX = ...; translationY = ... }` for pure positional animations. This bypasses the layout phase entirely, operating strictly during the GPU draw/layer phase.

## 2026-03-27 - Optimize intermediate allocations in BlackjackScreen
**Learning:** High-frequency recomposition blocks can trigger repeated allocations if operations like `flatMap` or chained additions (`+`) are used on collections.
**Action:** Replaced chained higher-order functions with a pre-sized `ArrayList` and `for` loops to avoid intermediate throwaway lists.

## 2026-03-27 - Zero-Allocation Helper Functions
**Learning:** In performance-critical logic, wrapping small fixed sets of domain objects into temporary collections just to pass them to helper methods incurs significant GC overhead.
**Action:** Extract explicit variables and rewrite helper functions to take positional arguments (e.g., `isFlush(c1: Card, c2: Card, c3: Card)`) rather than generically typed collections.

## 2026-03-26 - Defer State Reads in Compose Animation Loops
**Learning:** Reading animation state values (like `alphaProvider()`) inside a composable's composition phase triggers O(Frames) recompositions.
**Action:** Move state reads directly into the draw phase (e.g., inside the `drawBehind` modifier) so that only the layout/draw phase is invalidated.

## 2026-03-24 - Avoid Functional Fold in Computed Properties
**Learning:** Using functional extensions like `fold` on collections inside frequently accessed computed properties allocates `Iterator` instances, leading to main-thread GC thrashing in Jetpack Compose.
**Action:** Replace `fold` (and similar higher-order functions like `sumOf`) with indexed `for` loops (`for (i in 0 until list.size)`) on hot paths.

## 2026-03-24 - Allocation-Free Sorting for Small Fixed-Size Lists
**Learning:** For small, fixed-size collections (like 3-card hands), using generic `map` and `sorted` operations allocates temporary objects.
**Action:** Implement a manual sorting network using primitive variables for zero-allocation sorting.

---

## UI Animation Keys
- Added `key(event.id)` and `key(SideBetType)` inside `PayoutAnimationsOverlay` and `SideBetResultsOverlay` lists to prevent `Animatable` bleeding and sibling node recompositions.
