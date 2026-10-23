---
paths:
  - "backend/services/*/src/main/resources/db/migration/**"
  - "backend/services/*/src/main/java/**/model/**"
---

# Database migration rules (Flyway)

- **Never edit a migration that has already run.** Every schema change is a new file `V{n}__short_description.sql`, where `n` is the next number in that service.
- One DB per service. Don't reference another service's tables.
- **booking-service is PostgreSQL.** Every other service with a DB is MySQL. Use the right dialect.
- Hibernate runs with `ddl-auto: validate`. After changing an entity, the migration must make the schema match exactly (column names, types, nullability), or the service fails at startup.
- Store enums as `VARCHAR` without a `CHECK` constraint, so a new enum value needs no migration.
- Add an index for every foreign key and for every column used in a hot `WHERE` clause.
- Constraints that enforce business rules (unique payment per booking, partial unique index for active bookings) belong in the migration. They are the last line of defence against races.
- `user-service` tests run on H2 with `spring.flyway.enabled=false`. Keep it that way.
