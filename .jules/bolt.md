## 2024-05-18 - Caching Static Data in Compose
**Learning:** Returning lazily-computed or dynamically allocated data structures (like maps via `associateWith`) from provider objects that are read inside Compose recomposition blocks can trigger heavy allocations and high GC overhead. Specifically, `StrategyProvider` was dynamically creating `StrategyCell` mappings every time a composition read the strategies.
**Action:** When working with static maps or lists used frequently in UI components, eagerly instantiate them as `private val` properties and return those instances rather than recreating them on each function call.

## 2024-05-20 - O(N) Particle Removal in Animation Loops
**Learning:** Removing multiple elements from an `ArrayList` using manual backward loops with `removeAt(i)` or `Iterator.remove()` inside an animation loop results in $O(N^2)$ complexity due to repeated element shifting.
**Action:** Use `removeAll { predicate }` (which maps to `removeIf` on JVM for `ArrayList`) to perform a single-pass $O(N)$ removal. If state updates are required before removal, use a two-pass $O(N)$ approach: `forEach { it.update() }` followed by `removeAll { it.isDone }`.

## 2024-05-24 - Collection Functions in Rendering Loops
**Learning:** Using inline collection functions like `.sumOf` and `.forEach` inside heavy Jetpack Compose rendering loops (e.g. `OverlayCardTable.kt`) allocates temporary `Iterator` objects that can lead to main-thread GC thrashing.
**Action:** Replace these collection extension functions with standard index-based loops (`for (i in 0 until list.size)`) to eliminate Iterator allocations.

## 2026-03-22 - Optimize Collection Iterations in GameLogic
**Learning:** When calculating properties from immutable collections in Kotlin (like `PersistentList` in `Hand`), chained higher-order functions (`filter`, `sumOf`, `count`) allocate intermediate lists and loop over elements multiple times.
**Action:** Replace chained functions with a single `for` pass when performance is critical (e.g., UI rendering updates), combining conditions and accumulating values simultaneously.

## 2026-03-22 - Optimize Iterator Allocations on Summation
**Learning:** Using collection extensions like `.sum()` or `.sumOf()` on `PersistentList`, `PersistentMap`, or within tight Compose UI code creates intermediate `Iterator` objects and closures, causing unnecessary main-thread GC pressure.
**Action:** Replace `.sum()` and `.sumOf()` with manual `for` loops (e.g., `for (i in 0 until list.size)` or accumulating Map entries) to keep performance allocations strictly O(1).

## 2026-03-22 - Manual Sorting Networks for Small Collections
**Learning:** Using chained collection extensions like `.map { }.sorted()` to evaluate small, fixed-size hands (like 3 cards in side bets) creates multiple intermediate `ArrayList` and `Iterator` allocations, resulting in significant GC pressure when invoked repeatedly during high-frequency game logic (e.g. dealing/resolving multi-hand side bets).
**Action:** Replace dynamic sorting and mapping of small collections (e.g., exactly 3 cards) with a manual sorting network using primitive variable extraction and inline comparisons to achieve true O(1) allocation overhead.

## 2026-03-24 - Allocation-Free Sorting for Small Fixed-Size Lists
**Learning:** For small, fixed-size collections (like the 3-card hands in side bet logic), using generic `map` and `sorted` operations allocates temporary list objects and boxes primitive values.
**Action:** Implement a manual sorting network using primitive variables to perform sorting with zero allocations.
## 2026-03-24 - Avoid Functional Fold in Computed Properties
**Learning:** Using functional extensions like `fold` on collections inside frequently accessed computed properties (like `GameState.totalBet`) or state machine transitions (like `finalizeGame`) allocates `Iterator` instances, leading to main-thread GC thrashing in Jetpack Compose when state changes rapidly.
**Action:** Replace `fold` (and similar higher-order functions like `sumOf`) with indexed `for` loops (`for (i in 0 until list.size)`) to eliminate iterator allocations on hot paths.

## 2026-03-26 - Defer State Reads in Compose Animation Loops
**Learning:** Reading animation state values (like `alphaProvider()`) inside a composable's composition phase triggers O(Frames) recompositions for the entire composable tree during the animation.
**Action:** Move state reads directly into the draw phase (e.g., inside the `drawBehind` modifier) so that only the layout/draw phase is invalidated, bypassing full composition recomposition and eliminating main-thread jank.

