# Blackjack Project Rules

## Version Control
This project uses [Jujutsu (`jj`)](https://github.com/jj-vcs/jj) for version control instead of Git. Always prefer `jj` commands over `git` commands.

Key equivalents:
- Status: `jj status` (not `git status`)
- Diff: `jj diff` (not `git diff`)
- Commit message: `jj describe -m "..."` (not `git commit -m "..."`)
- New commit: `jj new`
- History: `jj log`
- Fetch/push: `jj git fetch` / `jj git push`
- Bookmarks (branches): `jj bookmark create <name>`
