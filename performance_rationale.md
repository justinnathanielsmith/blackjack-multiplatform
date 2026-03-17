# Performance Optimization Rationale: O(N) Particle Removal

## Problem: O(N^2) Removal in Animation Loops

The current implementation of particle effects (Confetti, Chip Eruption, Chip Loss) uses either a reversed `for` loop with `ArrayList.removeAt(i)` or an `Iterator.remove()` within a `while` loop to remove particles that have finished their animation.

```kotlin
// ConfettiEffect.kt (current)
for (i in particles.indices.reversed()) {
    val p = particles[i]
    p.update()
    if (p.alpha <= 0) {
        particles.removeAt(i)
    }
}
```

In an `ArrayList`, every call to `removeAt(i)` (or `Iterator.remove()`) requires shifting all subsequent elements to fill the gap. When multiple elements are removed in a single frame:
1.  If $K$ elements are removed from a list of size $N$, the total number of element shifts can be up to $O(K \times N)$.
2.  In the worst case where $K$ is proportional to $N$, this results in $O(N^2)$ time complexity.

While the reversed loop is often taught as a way to avoid `ConcurrentModificationException` or index-shifting issues during a single removal, it does not solve the underlying performance bottleneck of multiple `ArrayList` shifts.

## Solution: O(N) Single-Pass Removal

Kotlin's `MutableList.removeAll(predicate: (T) -> Boolean)` (and the underlying `removeIf` on JVM) is optimized for `ArrayList`. It uses a two-pointer approach (or similar) to:
1.  Iterate through the list once.
2.  Keep track of the "write" position for elements that should remain.
3.  Shift elements only once to their final positions.
4.  Truncate the list at the end.

This reduces the complexity of removing multiple elements from $O(N^2)$ to $O(N)$.

## Proposed Change

We will split the update and removal into two distinct $O(N)$ passes (or use a single pass that updates and removes efficiently):

```kotlin
// Optimized
particles.forEach { it.update() }
particles.removeAll { it.alpha <= 0 }
```

Even though this iterates twice, both passes are $O(N)$, resulting in $O(N)$ overall complexity, which is significantly better than $O(N^2)$ as the number of particles ($N$) grows.
