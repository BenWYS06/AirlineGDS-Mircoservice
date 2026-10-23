---
name: ship-pr
description: Commits finished work in small logical commits, pushes the branch and drafts the pull request description. Use when a step or piece of work is verified and ready for review.
---

# Ship a pull request

## 1. Check the branch
- Work must be on a feature branch (`feat/…`, `fix/…`, `docs/…`, `chore/…`), never directly on `main`.
- `git status` and `git diff`: confirm only intended files changed. Never commit `.env`, `target/`, `node_modules/`.

## 2. Show the diff, then STOP
List the changed files with a one-line summary each and the planned commits (message + files). **Wait for the user's OK** before committing.

## 3. Commit
- Stage explicit paths only (`git add <file> <file>`), never `git add .`.
- Small commits: 1–3 related files each, in dependency order (common-lib → service → frontend).
- Message format (see CONTRIBUTING.md): subject `type(scope): summary`, imperative, under 72 chars; then a body of 2–5 lines (wrapped at 72) saying why the change was needed and what it does.
  e.g. `feat(seat): hold seats with a single conditional update` + body

## 4. Push
```bash
git push -u origin <branch>
```

## 5. Draft the PR
Use `.github/pull_request_template.md`:
- **Summary:** what changed and why (link the build step / DESIGN.md section)
- **Changes:** bullets per service
- **Verification:** exactly what was run (compile, build, scan) and what was not
- **Notes:** anything deferred or risky

Give the user the compare link printed by `git push` and the PR text to paste. The user reviews and merges on GitHub. After the merge: `git checkout main && git pull`.
