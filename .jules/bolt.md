## 2024-05-24 - Optimize straight detection to eliminate array allocation overhead
**Learning:** In performance-critical logic for games like Blackjack, allocating small, fixed-size temporary collections (like `IntArray` or `listOf()`) and using generic sorting algorithms inside hot code paths (e.g., evaluating side bets for every hand) introduces measurable GC overhead and property access overhead.
**Action:** Use a manual sorting network with primitive local variables (`var`) when evaluating permutations of small fixed sizes (e.g., 3 cards) to eliminate object allocations entirely.
