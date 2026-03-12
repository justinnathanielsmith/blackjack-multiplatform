# Workflow - Blackjack Multiplatform

Standard operating procedures for developing and maintaining the Blackjack project.

## Build System (Amper)
- **Configuration**: Managed via `project.yaml` (root) and `module.yaml` (modules).
- **Dependencies**: Declared in `gradle/libs.versions.toml` and referenced in `module.yaml` using `$libs.`.
- **Commands**:
    - Build: `./amper build`
    - Run Desktop: `./amper run :desktopApp`
    - Run Android: `./amper run :androidApp`

## Version Control (Jujutsu)
- **Status**: `jj st`
- **Diff**: `jj diff`
- **Commit**: `jj commit -m "Description"`
- **Log**: `jj log`

## Development Principles
1. **Explain the "Why"**: Document architectural decisions and platform nuances.
2. **Walkthroughs**: Provide `walkthrough.md` for complex feature implementations.
3. **Benchmarks**: Reference `MemoryMatch` for UI/UX and animation standards.
4. **Validation**: Ensure all changes are verified across multiple platforms if possible.
