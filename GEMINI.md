# Project context and rules

## Tools and Architecture
This project uses JetBrains Amper instead of Gradle as the primary build configuration tool.

### Version Control
This project uses **Jujutsu (JJ)** for version control instead of Git. Use JJ commands (e.g., `jj st`, `jj commit`) when interacting with the repository.


### JetBrains Amper Guidelines
- Configuration is driven by `project.yaml` in the root, and `module.yaml` in sub-modules, rather than `build.gradle.kts`.
- Standard project layouts differ from Gradle. E.g., Android code goes in `<module-root>/src/` directly rather than `src/main/java/`. Android resources go into `<module-root>/res/`.
- Dependency management uses [Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html) declared in `gradle/libs.versions.toml`. 
- To declare a dependency down in a `module.yaml`, reference the catalog using the `$libs.` prefix (e.g., `- $libs.kotlinx.coroutines.core`). Do NOT refer to `[bundles]`.
- For Compose Multiplatform elements, use the `$compose.` prefix (e.g. `- $compose.material3`).

### Modules Overview
- `androidApp`: The entry point for the Android executable.
- `desktopApp`: The entry point for the JVM Desktop executable.
- `iosApp`: The entry point for the iOS app.
- `shared`: Contains common core logic, domains, or viewmodels. Divided into `core` and `data`.
- `sharedUI`: Contains Compose Multiplatform UI components shared across desktop, android, and iOS.

### Execution
- Use `./amper build` to run build steps via CLI, or integrate with JetBrains Fleet / IntelliJ IDEA.
- Ensure any added directories/files are properly checked into `module.yaml` files if required, although Amper conventionally includes all code in `src/`.
