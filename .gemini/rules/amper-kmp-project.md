---
description: JetBrains Amper rules for the Blackjack Compose KMP App
---

# Rules for this Blackjack Project

This project uses **JetBrains Amper** and **Compose Multiplatform**. You MUST adhere to these rules when working in this codebase:

1. **Build Tool (Amper)**: 
   - Never generate or modify `build.gradle.kts` files.
   - Use `project.yaml` and `module.yaml` for all configuration.
   - Run all builds using `./amper build` at the root project level.
   
2. **Project Structure**:
   - `androidApp`: The Android executable. Source is `androidApp/src/`. Resources are in `androidApp/res/`.
   - `desktopApp`: The Desktop executable.
   - `iosApp`: The iOS executable.
   - `shared`: Common business logic.
   - `sharedUI`: Shared Compose UI logic.
   
3. **Dependencies**:
   - Resolve dependencies via `$libs.<alias>` mapping to `gradle/libs.versions.toml`.
   - DO NOT refer to bundles like `$libs.bundles.xxx`.
   - Use `$compose.<library>` for all JetBrains Compose artifacts.

4. **KMP Structure**:
   - Do not assume `src/commonMain/kotlin` structure. Amper uses simplified `src/` and `src@platform/` paths.
