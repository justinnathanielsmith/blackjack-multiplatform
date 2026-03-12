# Blackjack Multiplatform

A cross-platform Blackjack game built with **Compose Multiplatform** and **JetBrains Amper**.

This project demonstrates a modern Kotlin Multiplatform (KMP) architecture, supporting **Android**, **iOS**, **Desktop (JVM)**, and **Web (Kotlin/Wasm)** from a single codebase.

## 🚀 Features

- **Cross-Platform UI**: Beautifully crafted game interface using Compose Multiplatform.
- **Shared Game Logic**: Core Blackjack engine and state machine implemented in pure Kotlin.
- **Rich Audio**: Integrated sound effects for dealing, flipping cards, and game outcomes.
- **Modern Tech Stack**: Uses Amper for build configuration and Jujutsu (jj) for version control.

## 🛠 Tech Stack

- **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- **Build System**: [JetBrains Amper](https://github.com/JetBrains/amper)
- **Version Control**: [Jujutsu (jj)](https://github.com/martinvonz/jj)
- **Platforms**:
    - Android
    - iOS
    - Desktop (JVM)
    - Web (Kotlin/Wasm)

## 📁 Project Structure

- `androidApp`: Android-specific entry point and resources.
- `iosApp`: iOS-specific entry point and Swift integration.
- `desktopApp`: JVM Desktop entry point.
- `wasmApp`: Kotlin/Wasm web application entry point.
- `shared/core`: Domain logic, Blackjack state machine, and utility functions.
- `sharedUI`: Shared Compose components, theme, and audio services.

## 🏗 Building and Running

This project uses **Amper**. You can use the provided `./amper` wrapper to build and run the application.

### Desktop
```bash
./amper run :desktopApp
```

### Android
```bash
./amper run :androidApp
```

### Wasm
```bash
./amper build :wasmApp
# Then serve the output from build/tasks/_wasmApp_linkWasmJs
```

> [!IMPORTANT]
> As of Amper 0.9.x, the Wasm toolchain does not automatically bundle Skiko assets.
> You must manually ensure `skiko.mjs` and `skiko.wasm` are available in the deployment directory.

## 📜 Educational Focus

This project is built with educational principles in mind, focusing on explaining the "Why" behind architectural decisions and platform-specific implementations. Check out the `GEMINI.md` for more context on the development guidelines.

---

Built with ❤️ using Kotlin Multiplatform.
