# 005 — Shared Domain Foundation

**Status:** In progress
**Depends on:** 001
**Expected branch:** `feature/mvp-domain-foundation`

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Provide the platform-neutral models, policy rules, lifecycles, and privacy tests
needed by the MVP.

## Sources

- `docs/product-design/04_DOMAIN_MODEL_AND_DECISIONS.md`
- `specs/001-notimate-android-mvp/spec.md`
- `specs/001-notimate-android-mvp/tasks.md`

## Scope

- Model minimized notifications, classification, profiles, automations, proposals,
  and privacy-safe activity records.
- Implement invariants, policy routing, validation, and redaction as pure logic.
- Add focused tests in `shared/src/commonTest`.

## Guardrails

- No Android APIs, persistence implementation, raw payload fixtures, or UI work.

## Done When

- [ ] Domain rules and lifecycle transitions are explicit and tested.
- [ ] Public APIs are minimal and platform-neutral.

## Validation

- `./gradlew :shared:allTests :shared:check`

## Result

- The first shared-domain slice is committed in `53a2db2` with minimized
  notification input, classification and agent outcomes, Profile and Automation
  models, eligibility rules, and focused common tests.
- Proposal lifecycle, privacy-safe Activity records, redaction, and their focused
  tests remain in this workstream.
