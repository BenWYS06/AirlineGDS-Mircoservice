---
paths:
  - "backend/**/controller/**"
  - "backend/**/clients/**"
  - "backend/common-lib/**/payload/**"
  - "frontend/src/Redux/**"
---

# API contract rules

The frontend and other services depend on the exact shape of requests and responses. Breaking it silently breaks a screen or a Feign call at runtime, and the compiler won't tell you.

**Before changing an endpoint, DTO field, enum value or event:**
1. Find every caller:
   - Feign clients in other services (`backend/services/*/src/main/java/**/clients/`)
   - `common-lib` payloads and events
   - Kafka producers/consumers of the event
   - `frontend/src/Redux/**` thunks and slices, plus pages that read the field
2. List them in the step's PROGRESS entry.
3. If the change is unavoidable, fix every caller **in the same step**.

**Defaults:**
- Prefer adding a field over renaming or removing one.
- Keep field names the FE already reads (e.g. `checkoutUrl`), even if the backend source changes.
- A derived field (e.g. `isAvailable` computed from `status`) can replace a stored one without changing the response.
- New error cases get a proper status code (409 conflict, 503 dependency down) and the FE handles them in the same step.
