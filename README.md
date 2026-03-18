# Blackjack Multiplatform

A premium cross-platform Blackjack game built with **Compose Multiplatform** and **JetBrains Amper**.

This project demonstrates a modern Kotlin Multiplatform (KMP) architecture, supporting **Android**, **iOS**, and **Desktop (JVM)** from a single codebase. It features a reactive UI pattern powered by a state machine and Decompose for lifecycle management.

## 🚀 Features

- **Cross-Platform UI**: Beautifully crafted game interface using Compose Multiplatform with adaptive layouts, "juicy" animations, and modern aesthetics.
- **Shared Game Logic**: Core Blackjack engine, standard rules (Split, Double-down, Insurance), Multi-Hand support, Side Bets (21+3, Perfect Pairs), and dealer AI implemented in pure Kotlin.
- **Strategy Guide**: Integrated strategy chart to assist players with optimal decision making based on hard, soft, and pair hands.
- **Reactive Architecture**: State machine pattern using `StateFlow` and decoupled side effects (audio, haptics) via `SharedFlow`.
- **Rich Audio & Haptics**: Integrated sound effects for dealing, flipping cards, and game outcomes, with haptic feedback on supported platforms.
- **Modern Tech Stack**: Uses **Amper** for build configuration and **Jujutsu (jj)** for version control.

## 🛠 Tech Stack

- **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- **Build System**: [JetBrains Amper](https://github.com/JetBrains/amper) (No Gradle/Gradlew)
- **Version Control**: [Jujutsu (jj)](https://github.com/martinvonz/jj)
- **Lifecycle Management**: [Decompose](https://github.com/arkivanov/Decompose)
- **Serialization**: `kotlinx-serialization`
- **Concurrency**: `kotlinx-coroutines`
- **Platforms**:
    - Android
    - iOS
    - Desktop (JVM)

## 📁 Module Map

- `shared/core`: Domain logic, `BlackjackStateMachine`, `GameLogic`, and core models (`GameState`, `Card`, `Hand`).
- `shared/data`: Persistence layer (DataStore).
- `sharedUI`: Shared Compose components, screens, theme, and service interfaces (Audio, Haptics).
- `androidApp`: Android-specific entry point and resources.
- `desktopApp`: JVM Desktop entry point.
- `iosApp`: iOS-specific entry point and Swift integration.

## 🏗 Building and Running

This project uses **Amper**. Use the provided `./amper` wrapper for all build and run tasks.

### Desktop
```bash
./amper run :desktopApp
```

### Android
```bash
./amper run :androidApp
```

### Build & Test Commands
```bash
./amper build -p jvm                          # Fast: JVM only
./amper test -p jvm                           # Fast: JVM tests only
./amper build -m core -m sharedUI -p jvm      # Specific modules
./amper build                                 # All platforms (slow)
```

## 🧹 Development Workflow

### Linting & Formatting
This project uses **ktlint** for formatting and **detekt** for static analysis.

```bash
./ktlint --format     # Auto-fix formatting issues
./lint.sh             # Run ktlint + detekt (used in CI)
jj fix                # Auto-format changed Kotlin files via jj
```

### String Resources
**Never hardcode UI strings.** Use Compose Multiplatform resources:
1. Add strings to `sharedUI/composeResources/values/strings.xml`.
2. Build the project to generate the `Res` class.
3. Import: `import sharedui.generated.resources.my_string_key`.

### Testing
Tests are located in `test/` directories within each module.
```bash
./amper test -p jvm
```
We follow a **Spec-Driven Development** approach. Requirements and plans for features are documented in `conductor/tracks/<feature>/spec.md`.

## 🔄 Version Control: Jujutsu (jj)

This project is managed with **Jujutsu (jj)**. While a `.git` folder exists for compatibility, use `jj` commands for development:
```bash
jj st                     # Status
jj diff                   # Current changes
jj commit -m "Message"    # Finalize commit
jj log                    # View commit graph
```

---

Built with ❤️ using Kotlin Multiplatform.
