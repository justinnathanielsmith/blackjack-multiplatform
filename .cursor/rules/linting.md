# Linting Rules

## Code Quality Tools

This project uses two linting tools:

1. **ktlint 1.5.0** - Kotlin code formatter
2. **detekt 1.23.7** - Static analysis with detekt-formatting plugin

## Before Suggesting Code Changes

- Ensure code follows ktlint formatting rules
- Be aware that Compose functions use PascalCase (function-naming rule is disabled)
- Trailing commas are not required (rules disabled in `.editorconfig`)

## Running Linters

```bash
./ktlint              # Check formatting
./ktlint --format     # Auto-fix formatting
./detekt              # Static analysis
./lint.sh             # Both tools (CI)
jj fix                # Format changed files
```

## Configuration Locations

- `.editorconfig` - ktlint configuration
- `config/detekt.yml` - detekt rules
- `.jj/repo/config.toml` - jj fix integration

## Common detekt Findings to Watch

- `MagicNumber` - Extract numeric literals to named constants
- `LongMethod` - Keep functions under 60 lines (relaxed for Composables)
- `LongParameterList` - Max 6 parameters for functions
- `EmptyFunctionBlock` - Avoid empty function bodies (use `Unit` or comment)
