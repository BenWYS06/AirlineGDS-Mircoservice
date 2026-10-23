# AI-Assisted Development Workflow

> How this repository is planned, built and reviewed with **Claude Code** (Anthropic's CLI agent).
> Claude does most of the typing; the developer owns the design decisions, approves every step and reviews every PR.

## 1. Roles

| Who | Does |
|---|---|
| **Developer** | Sets goals, agrees the design section by section, approves each step before code is written, reviews and merges PRs |
| **Claude Code** | Reads the project state, proposes designs, writes code and docs, verifies with scan + compile, commits and drafts PRs |
| **Rules** (`CLAUDE.md`, `.claude/rules/`) | Project conventions Claude loads automatically, so they never need repeating |
| **Agents** (`.claude/agents/`) | Focused reviewers Claude can call: code review, concurrency review, impact scan |
| **Skills** (`.claude/skills/`) | Packaged procedures for recurring tasks: implement a step, verify, record progress, ship a PR |

## 2. The loop

```text
① DESIGN    discuss the problem → agree DESIGN.md section by section → PLAN.md (scope, decisions)
② ORDER     LEARNING-PATH.md: build order, one concept per step
③ OVERVIEW  /implement-step → Claude summarises the step → developer approves
④ BUILD     Claude writes the code on a feature branch, with a purpose comment at each new piece
⑤ VERIFY    /verify-build (impact scan + compile) · concurrency-reviewer / code-reviewer agents
⑥ RECORD    /progress-update → PLAN ticked, PROGRESS entry, CHANGELOG when merged
⑦ SHIP      /ship-pr → small commits, push, PR → developer reviews and merges
```

Nothing is coded before ③ is approved. Nothing is called done before ⑤ has actually run.

## 3. Where state lives

| What | Where |
|---|---|
| Rules Claude must follow | `CLAUDE.md`, `backend/services/booking-service/CLAUDE.md`, `.claude/rules/*.md` |
| Agreed design (source of truth) | `docs/booking-refactor/DESIGN.md` |
| Goals, decisions, scope | `docs/booking-refactor/PLAN.md` |
| Build order + lessons | `docs/booking-refactor/LEARNING-PATH.md` |
| What's done, what's next | `docs/booking-refactor/PROGRESS.md` ("Current status" at the top) |
| Architecture decisions | `docs/decisions.md` |
| Release notes | `CHANGELOG.md` |

A new session needs only `CLAUDE.md` + PROGRESS "Current status" + the current step's DESIGN section. No need to re-read the whole repo.

## 4. Skills and agents

| Name | Type | When |
|---|---|---|
| `implement-step` | skill | "Start the next step": overview → approval → code → self-check |
| `verify-build` | skill | After any change: grep impact across BE/FE/config, `mvn compile`, `npm run build` |
| `progress-update` | skill | End of a step: tick PLAN, write PROGRESS, CHANGELOG |
| `ship-pr` | skill | Work verified: commit in small steps, push, draft the PR |
| `impact-scanner` | agent | Before renaming/removing an endpoint, DTO field, enum, event or config key |
| `concurrency-reviewer` | agent | Changes to seat holds, booking status, payment results, consumers, jobs |
| `code-reviewer` | agent | Before committing a step |

## 5. Principles

- **Design before code.** Every pattern (idempotency, row locks, saga, outbox) is agreed in DESIGN.md first, with the failure cases it handles.
- **One concept per step.** The app compiles after every step, and each step is reviewable on its own.
- **Verify, don't assert.** A step is done when scan + compile actually passed. Anything unverified is written down as unverified.
- **Keep contracts.** API shapes the frontend and other services use don't change silently (`.claude/rules/api-contract.md`).
- **Write it down.** Decisions go to PLAN/DESIGN, progress to PROGRESS. A session can be cleared at any time without losing context.
