# Specification - Code Quality & Testing Refinement

## Goal
Improve the overall code quality, maintainability, and reliability of the Blackjack Multiplatform application.

## Requirements

### 1. Unified Linting Strategy
- **`lint.sh`**: Create a single entry point for all code quality checks (ktlint + detekt).
- **Auto-Fixing**: Ensure `./ktlint --format` is used to fix most issues automatically.
- **Exclusion**: Properly exclude generated code (e.g., `build/generated`, `amper`) from analysis to avoid noise.

### 2. Static Analysis Hardening
- **`detekt`**: Resolve specific architectural and complexity issues:
    - **`BlackjackStateMachine`**: Address `TooManyFunctions` by refactoring or delegating logic.
    - **`GameEffectHandler`**: Reduce `CyclomaticComplexity` through strategy patterns or decomposed logic.
    - **`TooGenericExceptionCaught`**: Replace with specific exception types for better error handling.

### 3. Comprehensive Testing
- **Shared Logic**: Ensure all game rules (split, double, insurance) have pass-guaranteed unit tests.
- **State Transitions**: Verify all state machine transitions lead to valid terminal or intermediate states.
- **Platform Verification**: Ensure JVM tests run efficiently via `./amper test -p jvm`.

### 4. Code Standards & Style
- **Import Hygiene**: Fix `standard:import-ordering` and `standard:no-unused-imports` project-wide.
- **UI Architecture**: Verify Composable functions follow the project's explicit state parameter requirement.
- **Resource Management**: Confirm all UI strings use `stringResource(Res.string.xxx)` instead of hardcoded text.

## Success Criteria
- [ ] `./lint.sh` executes successfully without reporting issues.
- [ ] All `shared/core` unit tests pass.
- [ ] Detekt report shows 0 issues in source directories.
- [ ] No hardcoded strings in `sharedUI/src/ui/`.
