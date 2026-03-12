# Project context and rules

## Tools and Architecture
This project uses JetBrains Amper instead of Gradle as the primary build configuration tool.

### Version Control
This project uses **Jujutsu (JJ)** for version control instead of Git. Use JJ commands (e.g., `jj st`, `jj commit`) when interacting with the repository.

#### Standard JJ Workflow
1. **Check Status**: `jj st` - Shows modified, added, or deleted files.
2. **View Diff**: `jj diff` - Shows the exact changes in the current working-copy commit.
3. **Commit**: `jj commit -m "Description"` - Finalizes the current commit and starts a new one.
4. **History**: `jj log` - Displays the commit graph.
5. **Edit History**: `jj edit <revision>` to modify a past commit, or `jj new <revision>` to start fresh from one.

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
- `wasmApp`: The entry point for the Kotlin/Wasm web application.
- `shared`: Contains common core logic, domains, or viewmodels. Divided into `core` and `data`.
- `sharedUI`: Contains Compose Multiplatform UI components shared across desktop, android, and iOS.

### Execution
- Use `./amper build` to run build steps via CLI, or integrate with JetBrains Fleet / IntelliJ IDEA.
- Ensure any added directories/files are properly checked into `module.yaml` files if required, although Amper conventionally includes all code in `src/`.

### Kotlin/Wasm Deployment (JetBrains Amper)
> [!IMPORTANT]
> As of Amper 0.9.x, the Wasm toolchain does not automatically bundle Skiko assets or provide a dev server.

#### Skiko Asset Extraction Procedure:
Find the Skiko runtime JAR in the Amper cache and extract the assets:
```bash
# Locate the JAR (version may vary)
JAR_PATH=~/Library/Caches/JetBrains/Amper/.m2.cache/org/jetbrains/skiko/skiko-js-wasm-runtime/<version>/skiko-js-wasm-runtime-<version>.jar

# Extract to the distribution directory
unzip -j $JAR_PATH "*.mjs" "*.wasm" -d build/tasks/_wasmApp_linkWasmJs/
```

- **Serving**: Build via `./amper build`, then serve `build/tasks/_wasmApp_linkWasmJs` manually.
- **Skiko Assets**: `skiko.mjs` and `skiko.wasm` must be extracted and placed alongside `wasmApp.mjs` as shown above.
- **Import Maps**: Use an HTML `<script type="importmap">` to resolve bare module imports for libraries like `@js-joda/core`.


## Educational Principles
This project is as much about learning as it is about building. 
- **Explain the "Why"**: Don't just implement fixes. Explain the underlying platform mechanics (e.g., JVM threads, Compose Insets).
- **Walkthroughs**: After completing a complex task, always provide a `walkthrough.md` in the artifacts directory.
- **Technical Context**: Use GitHub alerts (TIP/NOTE/IMPORTANT) in documentation to highlight "Aha!" moments and best practices.
- **Active Dialogue**: Encourage questions about architecture (e.g., Scaffold vs. Box) even if the current implementation works.
## Design & Animation Benchmarking

This project aims for a "premium" feel. Use **MemoryMatch** (located in the same directory) as a primary reference for UI/UX and performance-optimized animations.

### Reference Patterns from MemoryMatch
- **High-Performance Particles**: Use `Canvas` + `withFrameNanos` index-based loops for many moving elements (see `ConfettiEffect.kt`).
- **Juicy Feedback**: Implement dramatic screen shakes (`runShakeAnimation`) and pulsing glows for game events.
- **Adaptive UI**: Follow the layout helpers in `MemoryMatch` for mobile/desktop responsiveness.
- **Color Palettes**: Mirror the curated, harmonized color systems (e.g., FeltGreen variations, ModernGold highlights).

> [!TIP]
> When building new UI features, first check how they were implemented in `MemoryMatch`. If a pattern works well there, adapt it here rather than inventing a new one from scratch.

## Linting

This project uses **ktlint** for code formatting and **detekt** for static analysis.

### Commands
```bash
./ktlint              # Check formatting
./ktlint --format     # Auto-fix formatting issues
./detekt              # Run static analysis
./lint.sh             # Run both (for CI)
jj fix                # Auto-format changed Kotlin files via jj
```

### Configuration
- `.editorconfig` - ktlint style rules (Compose-friendly: function-naming disabled)
- `config/detekt.yml` - detekt rules with detekt-formatting plugin (ktlint rules)
- `.jj/repo/config.toml` - jj fix tool configuration

### Tool Versions
- **ktlint**: 1.5.0 (binary in `tools/ktlint`)
- **detekt**: 1.23.7 (CLI in `tools/detekt-cli-1.23.7/`)
- **detekt-formatting**: 1.23.7 (plugin jar in `tools/`)

### Before Committing
1. Run `./ktlint --format` to auto-fix formatting
2. Run `./lint.sh` to verify no violations
3. Or use `jj fix` to format only changed files
