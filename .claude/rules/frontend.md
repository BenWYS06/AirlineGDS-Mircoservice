---
paths:
  - "frontend/**"
---

# Frontend rules (React 19 + Vite)

**Data flow:** components dispatch thunks from `src/Redux/<domain>/`; thunks call the API Gateway through `src/utils/api.js` with headers from `getHeaders.js`. Components never call axios directly.

**State:** one slice + one thunk file per domain. Keep loading and error flags per action, and use the right flag for the right action (a wrong flag means a button doesn't lock while a request is in flight).

**UI:** shadcn/ui components from `src/components/ui`, Tailwind 4 classes, `@` alias → `src/`. Forms use Formik + Yup.

**Calling the booking API:**
- Disable the submit button while the request is in flight.
- Handle `409` (seat already taken → reload the seat map) and `503` (try again) with a clear message.
- A `202` means "still processing": show a spinner and poll; don't treat it as success.

**Status values:** booking statuses come from the backend enum (`PENDING`, `AWAITING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `EXPIRED`, `REFUND_REQUIRED`). When a new status is added, grep every comparison in `src/` and update it in the same change.

**Verify:** `npm run build` and `npm run lint` from `frontend/`.
