# 🤖 AI Subagents & Workflows Guide

This guide explains how to leverage the AI subagents and structured workflows built into the Blackjack project to maintain high code quality, performance, and a premium user experience.

---

## 🛠 Subagent Directory (Slash Commands)

Each subagent is a specialized "personality" with a focused mission. They follow a strict **Scan → Select → Implement → Verify → Present** lifecycle.

| Command | Subagent | Mission | Key Focus Areas |
| :--- | :--- | :--- | :--- |
| `/bolt` | **Bolt** ⚡ | Performance | Recomposition, memory leaks, high-frequency frame loops, O(n²) logic. |
| `/architect`| **Architect** 🏛️ | Structure | Clean Architecture, MVVM/MVI, `commonMain` purity, DRY, DI via `AppGraph`. |
| `/palette` | **Palette** 🎨 | UX/UI | Micro-animations, accessibility (`contentDescription`), visual polish, "juice". |
| `/bumper**` | **Bumper** 📦 | Dependencies | Auditing `libs.versions.toml`, upgrading stable libs, Kotlin/Compose compatibility. |
| `/linter` | **Linter** 🎨 | Consistency | `ktlint` & `detekt` violations, hardcoded strings, Modifier ordering. |
| `/tutor` | **Tutor** 🧪 | Testing | Missing unit tests, Edge cases, Room test fakes, Coroutine testing. |
| `/doc` | **Doc** 📝 | Documentation | KDoc blocks, documenting complex logic, maintaining project READMEs. |
| `/sentinel` | **Sentinel** 🛡️ | Security | Dependency vulnerabilities, data exposure, sensitive logic auditing. |
| `/claude**` | **Claude** 🧠 | Reasoning | Complex debugging, deep architectural discussions, multi-phase planning. |

---

## 🔄 The Subagent Workflow Lifecycle

When a subagent runs, it follows a standardized process to ensure safety and quality:

1.  **🔍 SCAN**: The agent scans the relevant part of the codebase for one specific problem.
2.  **🎯 SELECT**: It selects exactly **ONE** high-impact improvement to implement.
3.  **🔧 IMPLEMENT**: It applies the change using project-standard patterns (Amper, jj, Kotlin idiomatic).
4.  **✅ VERIFY**: It runs `./amper build -p jvm`, `./amper test -p jvm`, and `./lint.sh`.
5.  **🎁 PRESENT**: It creates a PR-ready commit with a structured Title and Description.

### 📓 Journaling (`.jules/`)
Subagents maintain a persistent memory in the `.jules/` directory (e.g., `.jules/bolt.md`).
- **Learning Logs**: They record non-obvious learnings to avoid repeating past mistakes.
- **Project Memory**: These journals help agents understand the specific nuances of *this* codebase.

---

## 🛤 Spec-Driven Development (Conductor Tracks)

For larger feature development, we use **Conductor Tracks**. This is a **Spec-First** approach to engineering.

### Track Structure
Tracks live in `conductor/tracks/<track-name>/` and contain two primary files:

- **`spec.md`**: The source of truth for requirements. It defines the "What" and the "Acceptance Criteria".
- **`plan.md`**: The granular, step-by-step technical implementation plan.

### How to Use
1.  **Pick a Track**: Find an existing track in `conductor/tracks/` or create a new one.
2.  **Read the Spec**: Understand the requirements fully before writing code.
3.  **Follow the Plan**: Execute the steps in `plan.md`, marking them off as you go.
4.  **Test against Spec**: Verify the implementation satisfies the acceptance criteria in `spec.md`.

---

## 🧰 Developer Experience (Tooling)

### Version Control: Jujutsu (`jj`)
This project uses `jj` instead of `git`.
- Use `jj st` for status.
- Use `jj fix` to auto-format files.
- Use `jj git push` to push bookmarks to the remote.

### Build System: Amper
Use `./amper` for all builds and tests.
- `./amper build -p jvm` (Fast JVM build)
- `./amper test -p jvm` (Fast JVM tests)

---

## 💡 Best Practices for Working with Subagents

- **One at a time**: Let agents finish their work before starting a new one in the same module.
- **Review Descriptions**: Agents are trained to provide high-quality PR descriptions. Always read the **"Why"** and **"Impact"** sections.
- **Collaborative Refinement**: If an agent proposes a plan you don't like, ask it to "re-scan" or "pivot" to a different approach.
