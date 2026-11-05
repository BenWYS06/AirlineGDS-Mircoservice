# CI pipeline (GitHub Actions)

**Goal:** every PR shows a check that proves the code still builds, and the check grows stronger as the code that it can test lands on `main`.

**Scope:** `.github/workflows/`, `README.md` (badges), `TODO.md`, `docs/decisions.md` (ADR-013).
**Out of scope:** deployment (images to a registry, environments): a separate plan after observability. Fixing the existing ESLint errors: its own task (stage 3).
**Contract changes:** none.

## Why staged

Checked on 2026-11-04 with the full code on a laptop:

| Command | Result |
|---|---|
| `mvn -B compile` (all modules) | passes |
| `npm run build` | passes (warning: one chunk > 500 kB) |
| `npm run lint` | **fails**: 200 errors, 27 warnings, all in code from the original project (128 `no-unused-vars`, 56 `no-undef`, 27 `react-hooks/exhaustive-deps`, 9 `react-refresh/only-export-components`, 7 `no-useless-escape`) |

Checked on 2026-11-05 on a clean copy of `main` (after PR #20): 12 of 14 modules compile (common-lib + 13 services). `booking-service` and `payment-service` fail only on `BookingEventProducer` and `PaymentEventProducer`, which arrive with the outbox (build step 6).

`mvn test` on the same copy: 10 tests pass without any database (common-lib, service-registry, config-server, api-gateway, user-service, notification-service). The `contextLoads` tests of airline-core, location, flight-ops, pricing, ancillary and seat fail because they need a real MySQL.

A job only goes into CI when it passes on `main`. A red check on every PR teaches everyone to ignore it.
Rules are not switched off to make lint pass; the errors get fixed.

## Stages

- [x] **1. Backend compile:** `.github/workflows/backend.yml`. Java 17 (Temurin), Maven cache, `mvn -B -ntp compile` from `backend/`. Runs on PRs and pushes to `main` that touch `backend/**`. Badge in README. Skips `booking-service` and `payment-service` for now. Also runs the 10 tests that need no database.
- [ ] **1b. Compile every module:** remove the `-pl` filter in the build step 6 PR, once the producers are on `main`.
- [ ] **2. Frontend build:** `.github/workflows/frontend.yml`. Node 22 with npm cache, `npm ci` + `npm run build` from `frontend/`, on `frontend/**`. Added when the frontend on `main` is complete (routes and pages), so the build has every import it needs.
- [ ] **3. Frontend lint:** fix the 200 ESLint errors (unused code removed, `vite.config.js` gets the Node globals), then add `npm run lint` to the frontend job.
- [ ] **4a. Concurrency tests:** add seat, booking and payment to the test step when the concurrency tests (refactor build step 10) are on `main`, right after the backend build steps; they don't depend on the frontend. Testcontainers works on the `ubuntu-latest` runner (Docker is installed).
- [ ] **4b. Core service tests:** the test step grows to the core services as their tests land ([plan](2026-11-05-core-service-tests.md)). Location, pricing and ancillary stay compile-only, so the step keeps a module list instead of a plain `mvn verify`.
- [ ] **5. Branch protection:** require the checks before merging into `main` (GitHub settings, done by the owner).

## Verification

- Stage 1: `mvn -B -ntp compile -pl '!services/booking-service,!services/payment-service'` passes on a clean copy of `main` (2026-11-05); the test step's command passes on the same copy (10 tests); the CI PR itself shows a green "Backend CI / Compile and test" check.
- Stage 1b: the build step 6 PR is green with the filter removed.
- Each later stage: the job passes on its own PR before it is required.

## Result

Stage 1 written 2026-11-04, changed 2026-11-05 to start before the backend is complete: it goes up now and skips the two modules that can't compile yet.
