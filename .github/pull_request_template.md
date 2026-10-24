## Summary

<!-- What changed and why. Link the build step / DESIGN.md section if any. -->

## Changes

- 

## Verification

<!-- Exactly what was run, and what was not. -->
- [ ] `mvn -q compile` (backend) passes
- [ ] `npm run build` (frontend) passes, if touched
- [ ] Impact scan: every caller of changed endpoints / DTOs / events updated
- [ ] DB changes are new `V{n}__*.sql` files
- [ ] No secrets committed; `.env.example` updated for new env vars

## Notes

<!-- Deferred work, risks, follow-ups. -->
