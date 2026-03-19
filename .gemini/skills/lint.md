# Lint Skill

## Description
Maintain high code quality and consistency using the unified project linting suite.

## Commands

### Full Check (CI Style)
```bash
./lint.sh
```

### Auto-Fix Formatting & Basic Smells
```bash
./lint.sh --format
```

### Partial Formatting (jujutsu)
```bash
jj fix
```

## When to Use

- **Before Committing**: Run `./lint.sh --format` to ensure clean code.
- **After UI Changes**: Check for hardcoded strings or layout-specific lint issues.
- **After Logic Changes**: Verify that architectural complexity hasn't exceeded limits.
- **On Test Failure**: Use linting to check for formatting-related test issues.

## Handling Failures

1. **ktlint**: Most errors are resolved with `--format`. Manually break lines exceeding 120 characters.
2. **detekt (Complexity)**: If a method exceeds complexity (e.g., `handleGameAction`), decompose it into smaller, specialized functions.
3. **detekt (TooManyFunctions)**: Delegate class logic to `BlackjackRules` or other domain helpers.
4. **detekt (Exceptions)**: Prefer specific `catch` blocks. If a catch-all is necessary for a top-level loop, use `@Suppress("TooGenericExceptionCaught")`.

## Notes
- `lint.sh` ignores generated code (`build/`, `.amper/`) automatically.
- Always run the linter before pushing or submitting a task.
