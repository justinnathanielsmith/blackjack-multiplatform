# 🎨 Linter's Journal

This journal documents non-obvious style or tooling insights discovered during linting runs for the Blackjack project.

---

## 2026-04-05 - [Modifier] Convention for BaseOverlay
**Learning:** Shared UI components like `BaseOverlay` must also accept a `modifier` parameter, even if they wrap a `Dialog`. This allows parent layers (like `OverlayLayer`) to apply `zIndex`, animations, or other layout properties consistently.
**Action:** Always include `modifier: Modifier = Modifier` in the first optional position for any new `@Composable` overlay or dialog base.

