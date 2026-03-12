# Project Instructions

## Build System: JetBrains Amper

This project uses **JetBrains Amper** instead of Gradle. There is no `gradlew` script.

### Common Commands
```bash
./amper build              # Build all modules
./amper test               # Run all tests
./amper run                # Run the default application
./amper clean              # Clean build outputs
./amper show modules       # List all modules
```

### Configuration Files
- `project.yaml` - Root project configuration
- `module.yaml` - Per-module configuration (replaces build.gradle.kts)
- `gradle/libs.versions.toml` - Version catalog for dependencies

### Module Structure
Amper uses a simplified layout different from Gradle:
- Source code: `<module>/src/` (not `src/main/kotlin/`)
- Tests: `<module>/test/` (not `src/test/kotlin/`)
- Android resources: `<module>/res/` (not `src/main/res/`)
- Platform-specific code: `<module>/src@android/`, `<module>/src@ios/`, etc.

### Dependencies in module.yaml
```yaml
dependencies:
  - $libs.kotlinx.coroutines.core    # From version catalog
  - $compose.material3               # Compose Multiplatform
  - org.example:library:1.0.0        # Direct Maven coordinates
```

## Project Modules
- `shared/core` - Domain logic, state machine, game rules
- `shared/data` - Data layer (future persistence)
- `sharedUI` - Compose Multiplatform UI components
- `androidApp` - Android entry point
- `desktopApp` - JVM Desktop entry point
- `iosApp` - iOS entry point
- `wasmApp` - Kotlin/Wasm web entry point

## Testing
Run tests with `./amper test`. Tests use:
- `kotlin.test` - Assertions
- `kotlinx.coroutines.test` - Coroutine testing (`runTest`, `advanceUntilIdle`)

## Spec-Driven Development
This project follows spec-driven development. Implementation tracks are in `/conductor/tracks/`:
- `spec.md` - Requirements specification
- `plan.md` - Step-by-step implementation plan

Always write tests first based on the spec, then implement to make tests pass.

## Architecture Patterns
- **State Machine**: `BlackjackStateMachine` manages game state via `StateFlow`
- **Effects**: Side effects (audio, haptics) emitted via `SharedFlow`
- **Decompose**: Component lifecycle management
- **Composition Locals**: Dependency injection for services

## Linting

This project uses **ktlint 1.5.0** for formatting and **detekt 1.23.7** for static analysis.

### Commands
```bash
./ktlint              # Check formatting
./ktlint --format     # Auto-fix formatting issues
./detekt              # Run static analysis
./lint.sh             # Run both (for CI)
jj fix                # Auto-format changed Kotlin files
```

### Configuration
- `.editorconfig` - ktlint rules (function-naming disabled for Compose)
- `config/detekt.yml` - detekt rules with formatting plugin
- `.jj/repo/config.toml` - jj fix integration

### Before Committing
Run `./ktlint --format` to auto-fix formatting, then `./lint.sh` to verify no violations remain.
