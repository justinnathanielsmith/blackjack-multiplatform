
## 2024-05-18 - Caching Static Data in Compose
**Learning:** Returning lazily-computed or dynamically allocated data structures (like maps via `associateWith`) from provider objects that are read inside Compose recomposition blocks can trigger heavy allocations and high GC overhead. Specifically, `StrategyProvider` was dynamically creating `StrategyCell` mappings every time a composition read the strategies.
**Action:** When working with static maps or lists used frequently in UI components, eagerly instantiate them as `private val` properties and return those instances rather than recreating them on each function call.

## 2024-05-20 - O(N) Particle Removal in Animation Loops
**Learning:** Removing multiple elements from an `ArrayList` using manual backward loops with `removeAt(i)` or `Iterator.remove()` inside an animation loop results in $O(N^2)$ complexity due to repeated element shifting.
**Action:** Use `removeAll { predicate }` (which maps to `removeIf` on JVM for `ArrayList`) to perform a single-pass $O(N)$ removal. If state updates are required before removal, use a two-pass $O(N)$ approach: `forEach { it.update() }` followed by `removeAll { it.isDone }`.

## $(date +%Y-%m-%d) - Optimize Collection Iterations in GameLogic
**Learning:** When calculating properties from immutable collections in Kotlin (like `PersistentList` in `Hand`), chained higher-order functions (`filter`, `sumOf`, `count`) allocate intermediate lists and loop over elements multiple times.
**Action:** Replace chained functions with a single `for` pass when performance is critical (e.g., UI rendering updates), combining conditions and accumulating values simultaneously.
