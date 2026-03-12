---
description: "Instructions for using Jujutsu (JJ) version control system instead of Git"
---

# Jujutsu (JJ) Guidelines

This project uses **Jujutsu (JJ)** as the version control system instead of Git.

## Key Differences from Git
- The default CLI command is `jj` rather than `git`.
- You do NOT "stage" and "commit" in distinct steps the way you do in Git. JJ always automatically tracks changes in the working copy.
- Your working copy is actually a commit itself (often referred to as the "working-copy commit").
- Branching is anonymous by default. You simply create new commits.
- Commits can be edited easily since everything is a commit.

## Command Equivalents
- `git status` -> `jj st` (or `jj status`)
- `git add <file>` + `git commit` -> In JJ, just use `jj commit -m "msg"`. It commits the current working copy and immediately creates a new working-copy commit on top of it.
- `git log` -> `jj log`
- `git diff` -> `jj diff`
- `git checkout -b branch_name` -> branching in JJ is usually anonymous (`jj new`). To name it: `jj branch create branch_name`.
- `git checkout <commit>` -> `jj edit <commit>`
- Untracking files -> `jj file untrack <file>` or similar (consult `jj help file`).

## Workflow Rules for Antigravity
1. **Always use `jj`** commands when managing version control in this repository. Avoid using `git`.
2. When the user asks you to commit, use `jj commit -m "Your detailed message"`.
3. Before committing, consider running `jj diff` to see what changes are present in the current working copy.
4. When tracking new files or checking status, rely on `jj st`.

