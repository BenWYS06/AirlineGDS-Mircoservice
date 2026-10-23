---
name: implement-step
description: Implements the next build step of the booking refactor (LEARNING-PATH.md build order) the agreed way - overview, wait for approval, code with purpose comments, self-check against DESIGN.md. Use when the user says "next step", "start step N" or "build step N".
---

# Implement a build step

## 1. Load state
Read, in this order:
- `docs/booking-refactor/PROGRESS.md` → "Current status": which step is next, what's pending
- `docs/booking-refactor/LEARNING-PATH.md` → the step's row in the build order + its lesson
- `docs/booking-refactor/DESIGN.md` → the sections the step implements

## 2. Overview, then STOP
Give the user a short overview:
- The concept this step teaches (one line)
- Files that will change or be created, one line each on what changes
- Contract changes (API, DTO, event, migration) and who is affected (run the `impact-scanner` agent if anything is renamed or removed)

**Wait for the user's OK. Do not write code before it.**

## 3. Implement
- Create a branch `feat/booking-step-N-<topic>` from an up-to-date `main`.
- Write the code. At each new piece, add a short comment saying **why** it exists (the pattern it applies), matching the surrounding comment density.
- DB changes = new `V{n}__*.sql` only.
- Fix every affected caller (FE, Feign, consumers) in the same step.

## 4. Self-check
- Run `/verify-build` (impact scan + compile).
- For steps touching seats, booking status, payment results or consumers: run the `concurrency-reviewer` agent on the diff.
- Compare against DESIGN.md: anything in the step's scope missing? anything added that's out of scope?

## 5. Report
List changed files (one line each), verification results, and anything not verified (e.g. "not run against a real DB"). Then run `/progress-update`.
