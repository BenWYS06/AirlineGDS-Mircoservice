---
name: progress-update
description: Records a finished step - ticks PLAN.md, adds an entry at the top of PROGRESS.md, updates "Current status", and adds a CHANGELOG line when a milestone ships. Use at the end of every build step or work session.
---

# Record progress

## 1. PLAN.md
Tick the checkboxes of the work that is done. If a new decision was made, add it to "Decisions" (§2).

## 2. PROGRESS.md
Add an entry **directly under "Current status"** (newest first):

```markdown
## YYYY-MM-DD: Build step N, <concept>

- `path/to/File.java`: what changed and why (one line)
- ...
- Decisions: anything decided along the way
- Verified: what was checked (compile, build, scan), and what was NOT verified
- Not touched on purpose: anything deferred to a later step
```

Then update "Current status": step just finished, next step, what is waiting on the user.

## 3. CHANGELOG.md
Only when a build step is merged into `main`: add a short user-facing line under `## Unreleased` (what now works differently, not how).

## 4. Report
Show the user the new PROGRESS entry.
