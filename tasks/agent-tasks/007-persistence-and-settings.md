# 007 — Persistence And Settings

**Status:** Proposed
**Depends on:** 005
**Expected branch:** Set in the approved Git plan before work

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Provide local sources of truth for settings, monitored sources, profiles,
automations, proposals, and privacy-safe activity.

## Scope

- Choose storage by data shape and access needs.
- Expose domain models through repository boundaries.
- Enforce one active profile, retention, and clear-data behavior.
- Add focused persistence and repository tests.

## Guardrails

- Do not persist raw notification bodies.
- Keep entities and storage APIs out of domain and UI contracts.
- Use encrypted platform storage only for genuine secrets.

## Done When

- [ ] Durable ownership and retention behavior are deterministic and tested.
- [ ] Settings survive restart without leaking sensitive payloads.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`
