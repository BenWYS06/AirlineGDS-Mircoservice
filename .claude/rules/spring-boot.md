---
paths:
  - "backend/**/*.java"
  - "backend/**/pom.xml"
---

# Spring Boot rules

**Injection:** constructor injection via Lombok `@RequiredArgsConstructor`. No `@Autowired` on fields.

**Layers:** `controller` → `service` (interface) → `service/impl` → `repository`. Controllers stay thin: validate, call the service, return a DTO.

**DTOs:** never return a JPA entity from a controller. Shared request/response DTOs, enums and events live in `common-lib`. Map with the service's `mapper/` classes.

**Identity:** the gateway forwards a trusted `X-User-Id` / `X-User-Roles`. Read the user from there, never from a request body or query param. Guard endpoints with `@RequiredRoles`.

**Transactions:**
- Keep them short. No Feign/HTTP call or Kafka send inside `@Transactional`.
- Use `TransactionTemplate` when a method mixes remote calls and DB writes.
- State transitions are conditional updates (`WHERE id = ? AND status = ?`); check the affected row count.

**Feign clients:** every client has a fallback (or fallback factory) that **throws** (503). A fallback never returns fake data.

**Errors:** throw the `common-lib` exceptions (`ResourceNotFoundException`, `ForbiddenException`, …). Don't swallow exceptions in `catch` blocks; if a transaction must roll back after catching, call `setRollbackOnly()`.

**Config:** service config lives in `config-repo/<service>.yml` **and** the service's `application.yaml`. Change both when a key exists in both. Secrets come from env vars with placeholder defaults only.

**JPA:** `ddl-auto: validate`. The schema is owned by Flyway (see `db-migrations.md`).
