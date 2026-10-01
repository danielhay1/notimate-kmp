# 005 — Shared Domain Foundation

**Status:** Implemented; awaiting independent review and user merge
**Depends on:** 001
**Expected branch:** `feature/mvp-domain-completion`

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

- [x] Domain rules and lifecycle transitions are explicit and tested.
- [x] Public APIs are minimal and platform-neutral.

## Validation

- `./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug`
- Android is the validation target for this slice; iOS verification is deferred.

## Result

- Reuses the notification and automation foundation from `53a2db2`, which was not
  merged into `dev`; unrelated branch documents are not copied.
- Shared models now cover source admission, conservative policy routing, selected
  Profile invariants, editable calendar fields, guarded Proposal transitions, and
  typed Activity metadata. No UI, persistence, extraction, or platform side effects
  are implemented in this packet.
- Notification and Proposal string output is redacted, not a content sanitizer.
  Raw content remains transient; structured Proposal fields remain local-only.
- Confidence thresholds and Proposal expiry are caller-owned policies. Calendar
  dates/times are local wall-clock values; the later Android adapter must resolve
  time zones and reject nonexistent/ambiguous local times before handoff.
- On 2026-10-01 the validation command above passed using Android Studio's JDK21:
  26 shared Android-host tests (24 new domain tests), zero failures; Android lint
  and debug APK assembly passed. Android app unit-test task had no sources.
- No UI tests, device install/launch, or iOS verification were performed for this
  pure-domain slice. Independent review and user merge remain required.
