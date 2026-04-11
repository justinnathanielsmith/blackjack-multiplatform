# Eval's Journal

## 2026-04-11 - [Effects]
**Surprise:** Several `GameEffect` variants (`NearMissHighlight`, `BigWin`, `PlayPlinkSound`) were defined in the domain but had zero test coverage, and one was not even emitted.
**Rule:** Always audit `GameEffect.kt` against `GameEffectsFlowTest.kt` to ensure "premium" feedback is actually reaching the user.
