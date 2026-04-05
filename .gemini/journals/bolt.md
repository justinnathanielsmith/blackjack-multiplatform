## 2026-03-29 - Recomposition Trap: animateOffsetAsState with 'by'
**Learning:** Using the delegated 'by' property with animateOffsetAsState in a high-level Screen component causes the entire screen to recompose on every animation frame. This is extremely expensive if the screen has complex drawing or many children.
**Action:** Use 'val state = animateOffsetAsState(...)' and read 'state.value' only inside the narrowest possible scope, preferably a draw scope (drawBehind/onDrawBehind) or a leaf Composable, to bypass high-level recomposition.

## 2026-03-29 - kotlinx.serialization and `by lazy` properties
**Learning:** In a `@Serializable` data class, properties defined in the class body using `by lazy` do not need the `@Transient` annotation. The `kotlinx.serialization` plugin automatically ignores them because they lack a primary constructor backing field. Adding `@Transient` produces a compiler warning: `Property does not have backing field which makes it non-serializable and therefore @Transient is redundant`.
**Action:** Do not use `@Transient` on `by lazy` or computed properties in `@Serializable` data classes.

## 2026-04-05 - GC Churn with removeAll in withFrameNanos
**Learning:** The `MutableList.removeAll { ... }` extension function in the Kotlin standard library creates an inline iterator under the hood. When placed inside a high-frequency loop like `withFrameNanos`, it causes continuous Iterator allocation and GC churn, leading to micro-stutters.
**Action:** Always replace `removeAll {}` and standard `for (item in list)` loops inside `withFrameNanos` with zero-allocation, index-based two-pointer compaction loops.