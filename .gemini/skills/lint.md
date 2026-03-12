# Lint Skill

## Description
Run code linting tools to check formatting and code quality.

## Commands

### Check Formatting Only
```bash
./ktlint
```

### Auto-fix Formatting
```bash
./ktlint --format
```

### Run Static Analysis
```bash
./detekt
```

### Run Full Lint Suite (CI)
```bash
./lint.sh
```

### Format Changed Files via jj
```bash
jj fix
```

## When to Use

- Before committing code changes
- After generating or modifying Kotlin files
- When CI reports linting failures
- To clean up imports and formatting automatically

## Notes

- ktlint can auto-fix most formatting issues
- detekt findings may require manual code changes
- The `jj fix` command only formats files changed in the current commit
