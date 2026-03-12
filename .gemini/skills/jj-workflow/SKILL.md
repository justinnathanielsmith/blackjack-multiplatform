---
description: "Skill for standard Jujutsu (JJ) version control operations"
---

# Jujutsu (JJ) Workflow Skill

Use this skill when performing version control operations in a project that uses Jujutsu (JJ) instead of Git.

## Standard Commit Workflow

1. **Check Status**:
   ```bash
   jj st
   ```
   This will show you what files have been modified, added, or deleted in the current working-copy commit.

2. **View Diff**:
   ```bash
   jj diff
   ```
   This will show the exact changes that will be captured.

3. **Commit**:
   ```bash
   jj commit -m "Type your commit message here"
   ```
   This finalizes the current working-copy commit with your message, and creates a new empty working-copy commit on top of it.

## Basic Repository Navigation

- **View History**:
  ```bash
  jj log
  ```
  This displays the commit graph.

- **Start Fresh on a Previous Commit**:
  ```bash
  jj new <revision>
  ```
  This creates a new empty working-copy commit starting from `<revision>`.

- **Edit an Existing Commit**:
  ```bash
  jj edit <revision>
  ```
  This makes the specified `<revision>` your current working copy. Any changes you make will be directly applied to that commit.

## Handling Ignored/Untracked Files

If you add a file to `.gitignore` that was previously tracked:
1. Untrack the file:
   ```bash
   jj file untrack <file-path>
   ```
2. The file will now be ignored by JJ.

## Best Practices
- Avoid running standard git commands (like `git add` or `git commit`) as they might conflict with or misrepresent JJ's state.
- Keep commit messages concise but descriptive. Include a summary on the first line, followed by detailed bullet points if necessary.
