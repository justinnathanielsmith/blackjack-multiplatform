
## 2024-05-18 - Caching Static Data in Compose
**Learning:** Returning lazily-computed or dynamically allocated data structures (like maps via `associateWith`) from provider objects that are read inside Compose recomposition blocks can trigger heavy allocations and high GC overhead. Specifically, `StrategyProvider` was dynamically creating `StrategyCell` mappings every time a composition read the strategies.
**Action:** When working with static maps or lists used frequently in UI components, eagerly instantiate them as `private val` properties and return those instances rather than recreating them on each function call.
