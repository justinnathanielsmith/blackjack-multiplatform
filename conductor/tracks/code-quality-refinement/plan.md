# Implementation Plan - Code Quality & Testing Refinement

## Phase 1: Linting Infrastructure
1.  **Re-establish `lint.sh` script.**
    -   Consolidate `ktlint` and `detekt` calls into one script.
    -   Add logic to fail on errors and warn on potential issues.
    -   Ensure it properly ignores generated files.
2.  **Run `ktlint --format` project-wide.**
    -   Focus on source directories (`shared/`, `sharedUI/`, `androidApp/`, `desktopApp/`, `iosApp/`).
    -   Manually resolve any formatting issues that cannot be auto-fixed.

## Phase 2: Static Analysis Resolution
1.  **Refactor `BlackjackStateMachine.kt`.**
    -   Analyze the 30+ functions and delegate some logic to `GameLogic.kt` or a helper class.
    -   Replace the `TooGenericExceptionCaught` on line 103 with specific exceptions or proper error handling.
2.  **Refactor `GameEffectHandler.kt`.**
    -   Break down the complex `handleGameEffect` function into smaller, specialized functions.
    -   Consider using a sealed class or strategy pattern to reduce cyclomatic complexity.

## Phase 3: Testing & Validation
1.  **Run existing tests.**
    -   Execute `./amper test -p jvm` and verify all tests pass.
    -   Identify any flaky or slow tests.
2.  **Audit test coverage.**
    -   Ensure critical game paths (split, insurance, betting transitions) have thorough coverage.
    -   Add missing tests for edge cases identified in the `shared/core/test` audit.

## Phase 4: Code Hygiene & Standards
1.  **Import & Resource Hygiene.**
    -   Fix all import orderings and unused imports.
    -   Verify no hardcoded UI strings remain in `sharedUI/`.
2.  **Final Lint Pass.**
    -   Run `./lint.sh` to confirm 0 issues remain in source directories.
