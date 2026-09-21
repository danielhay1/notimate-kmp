# 006 — Notification Capture

**Status:** Proposed
**Depends on:** 005
**Expected branch:** Set in the approved Git plan before work

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Capture Android notifications and immediately map them to minimized shared-domain
input without leaking raw content.

## Scope

- Add the listener service declaration and required metadata.
- Keep the Android service thin and delegate to injected collaborators.
- Map only required fields and apply monitored-source filtering.
- Add privacy-safe connection diagnostics and focused mapper tests.

## Guardrails

- Never log, persist, upload, or place raw bodies in fixtures.
- Keep Android framework types outside shared domain APIs.

## Done When

- [ ] Capture, minimization, filtering, and revocation behavior are defined.
- [ ] Mapper and policy-boundary tests pass with synthetic data.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`
