---
name: verify-build
description: Verifies a change by static impact scan plus compile, without starting the services. Use after any code change and before committing.
---

# Verify by scan + compile

Starting all microservices takes too long, so a change is verified like this:

## 1. Impact scan
For every symbol the change renamed, removed or changed shape (class, method, DTO field, enum value, endpoint path, event, config key):
```bash
grep -rn "<symbol>" backend frontend/src config-repo docker --include=*.java --include=*.js --include=*.jsx --include=*.ts --include=*.tsx --include=*.yml --include=*.yaml --include=*.sql
```
Every hit must be either updated in this change or confirmed unaffected.

## 2. Compile the backend
From `backend/`:
```bash
mvn -q compile                                    # if common-lib changed
mvn -q compile -pl services/<service> -am          # otherwise, each touched service
```

## 3. Build the frontend (only if `frontend/` changed)
From `frontend/`:
```bash
npm run build
npm run lint
```

## 4. Migrations (only if a `V{n}__` file was added)
Check the SQL against the entity, column by column: name, type, nullability, indexes. Note it as "not run against a real DB" unless it was.

## Report
One line per check: ✅ / ❌ with the error. Don't claim a check passed if it wasn't run.
