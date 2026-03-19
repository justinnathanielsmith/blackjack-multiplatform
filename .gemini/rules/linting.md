# Linting Rules

## Code Quality Tools

This project uses a unified linting approach via `lint.sh`, which wraps:

1. **ktlint** - Kotlin code formatter and style checker.
2. **detekt** - Static analysis for complexity, code smells, and common issues.

## Quality Standards

### 1. Unified Entry Point
- **Always use `./lint.sh`** to run the full suite.
- **Use `./lint.sh --format`** to automatically fix formatting and simple static analysis issues.
- **Avoid running tools individually** unless debugging a specific rule.

### 2. Architectural Constraints
- **Complexity**: Keep methods under a cyclomatic complexity of 15. Decompose complex logic into specialized sub-handlers.
- **Function Count**: Keep classes under 30 functions. Delegate logic to domain helpers (e.g., `BlackjackRules`) or state machine extensions.
- **Exceptions**: Never catch generic `Exception` at the top level without a specific reason; if required for a terminal loop, use `@Suppress("TooGenericExceptionCaught")` with justification.

### 3. Formatting
- **PascalCase**: Required for all `@Composable` functions.
- **Imports**: Must be ordered lexicographically. `./lint.sh --format` handles this automatically.
- **Line Length**: Max 120 characters. Manually break long expressions or chains.

## Configuration Locations

- `.editorconfig` - ktlint configuration (shared with IDE).
- `config/detekt/detekt.yml` - detekt rules and thresholds.
- `lint.sh` - Unified wrapper script (ignores generated code).

## Pre-Commit Checklist
- [ ] `./lint.sh --format` executes cleanly.
- [ ] No hardcoded strings in `sharedUI/` (use `Res.string`).
- [ ] All `shared/core` unit tests pass (`./amper test -p jvm`).
