# 013 — Optional Account And Sync

**Status:** Proposed
**Depends on:** 012
**Expected branch:** Set in the approved Git plan before work

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Define and, only after approval, implement optional account backup and sync without
weakening local-first privacy.

## Scope

- Specify exactly which configuration fields may sync and why.
- Keep core local use available without sign-in.
- Define consent, sign-out, deletion, conflict, and offline behavior.
- Implement only the separately approved boundary.

## Guardrails

- Raw notification content and proposal text never sync.
- Do not select a provider before requirements and threat boundaries are approved.

## Done When

- [ ] The local-only versus syncable boundary is documented and approved.
- [ ] Consent, deletion, conflict, and offline behavior are tested if implemented.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`
- Privacy-boundary review before any external integration.
