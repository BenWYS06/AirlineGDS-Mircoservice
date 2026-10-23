# CLAUDE.md

> Read this file before doing anything.
> Service context loads on demand: `backend/services/booking-service/CLAUDE.md`.
> Folder-scoped rules load automatically from `.claude/rules/`.

---

## Project: Ben Airline, an Airline GDS on microservices

Spring Boot 4 microservices (Maven multi-module) + React 19 / Vite frontend. Covers the booking lifecycle: flight search → seat selection → booking → VNPay payment → notification.

**Current work: booking flow refactor.** The booking saga is being rebuilt to practise the core distributed-systems patterns: idempotency, row locks, deadlock avoidance, race conditions, saga orchestration and the transactional outbox. Before touching anything in that area, read:

1. [docs/booking-refactor/PLAN.md](docs/booking-refactor/PLAN.md): goals, locked decisions, scope.
2. [docs/booking-refactor/PROGRESS.md](docs/booking-refactor/PROGRESS.md): current step, what's next.
3. [docs/booking-refactor/DESIGN.md](docs/booking-refactor/DESIGN.md): the agreed design (wins over PLAN where they differ).
4. [docs/booking-refactor/LEARNING-PATH.md](docs/booking-refactor/LEARNING-PATH.md): build order, one concept per step.

How we work with Claude on this repo: [docs/AI_WORKFLOW.md](docs/AI_WORKFLOW.md).

## Working mode

**Step by step, approved before code.** For every build step:
1. Give a short overview of the step (what gets written, main points only).
2. Wait for the user's OK. Never write code for a step before that.
3. Write the code. At each new piece, add a short comment stating its purpose.
4. Re-check: compiles, nothing missing for the step, matches DESIGN.md. List the changed files.
5. Tick the checkbox in PLAN, add an entry at the top of PROGRESS, update "Current status".

**Stop and ask when:** a change would break an API contract, the task can be read two very different ways, a secret is missing, or an action can't be undone (dropping data, force push).

**Don't ask about:** naming, file layout, or small choices inside the agreed step. Pick the standard option and say why.

## Critical rules

- **Stay in scope.** Only change what PLAN §3 covers. No drive-by refactors.
- **Scan impact before removing or renaming anything:** other services' Feign clients, `common-lib` DTOs, Kafka consumers, `frontend/src` (thunks, slices, pages), `config-repo`, `docker`. Use the `impact-scanner` agent.
- **Keep the API contract** (PLAN §4b). If a change is unavoidable, fix every caller in the same step.
- **DB changes = a new `V{n}__*.sql` file.** Never edit a migration that has already run.
- **No Feign/HTTP call inside `@Transactional`.** Keep transactions short.
- **Verify by scan + compile, not by running every service** (see Verify below).
- Never commit secrets. Real values live in `docker/.env` (git-ignored); `.env.example` holds placeholders only.
- Reply to the user in English: brief, main point, practical.

## Services & ports

| Service | Port | DB | Role |
|---|---|---|---|
| api-gateway | 5000 | — | OAuth2 resource server (Keycloak JWT), routing, trusted user headers |
| config-server | 8888 | — | Git-backed config from `config-repo/` |
| service-registry | 8761 | — | Eureka |
| user-service | dynamic | MySQL `airline_user` | Profiles, Keycloak subject → user id |
| airline-core-service | dynamic | MySQL `airline_core_db` | Airlines, aircraft |
| location-service | dynamic | MySQL `airline_location_db` | Cities, airports |
| flight-ops-service | 5002 | MySQL `airline_flight_db` | Flights, schedules, instances, search |
| seat-service | 5005 | MySQL `airline_seat_db` | Seat maps, seat hold / confirm / release |
| pricing-service | dynamic | MySQL `airline_pricing_db` | Fares, baggage, fare rules |
| ancillary-service | dynamic | MySQL `airline_ancillary_db` | Meals, insurance |
| booking-service | dynamic | **PostgreSQL** `airline_booking_db` | Saga orchestrator: bookings, passengers, tickets |
| payment-service | dynamic | MySQL `airline_payment_db` | VNPay: payment URL, IPN, return |
| notification-service | 8094 | — | Email (Gmail SMTP + Thymeleaf), SMS |
| subscription-service | dynamic | — | Airline subscriptions |

Infra: Keycloak 8180 · Kafka 9092 (KRaft) · Redis 6379 · MySQL 3306 · PostgreSQL 5432 · frontend 5173.

## Key paths

- Shared DTOs, enums, events, security: `backend/common-lib/src/main/java/com/ben/common_lib/`
- Service code: `backend/services/<service>/src/main/java/com/ben/services/`
- Migrations (Flyway): `backend/services/<service>/src/main/resources/db/migration/`
- Central config: `config-repo/<service>.yml`, `config-repo/application.yml`
- Local infra: `docker/docker-compose.yml`, `docker/docker-compose.dev.yml`
- Frontend: `frontend/src/` (`Redux/` slices + thunks, `pages/`, `components/`)

## Build & verify

```bash
# backend: from backend/
mvn -q compile                              # all modules
mvn -q compile -pl services/booking-service -am   # one service + common-lib

# frontend: from frontend/
npm run build
npm run lint
```

After a change: grep every usage of changed symbols across `backend/`, `frontend/src/`, `config-repo/`, `docker/`, then compile. Run `npm run build` if the frontend was touched. Start services only when the user asks. Skills: `/verify-build`.

## Tooling

- Java 17, Maven (multi-module from `backend/pom.xml`), Spring Boot 4, Spring Cloud 2025.1
- Frontend: npm, Vite 7, Redux Toolkit, Tailwind 4 + shadcn/ui
- Git: one branch per piece of work, PR into `main`. Conventions: [CONTRIBUTING.md](CONTRIBUTING.md)

## Reference docs (load on demand)

- Architecture and data flows: `docs/architecture.md`
- Authentication (Keycloak → gateway → services): `docs/authentication.md`
- Architecture decisions: `docs/decisions.md`
- VNPay sandbox setup: `docs/booking-refactor/VNPAY.md`
- Release notes: `CHANGELOG.md`

## Compaction instructions

When compacting, always keep: the current build step and its status, the list of modified files, decisions made this session, and pending TODOs.
