# 012 — Activity And Local Controls

**Status:** Proposed
**Depends on:** 007, 010
**Expected branch:** Set in the approved Git plan before work

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Provide privacy-safe activity history, retention settings, and clear-data controls.

## Scope

- Show structured outcomes without reconstructing raw notifications.
- Add permission, notification-channel, monitored-app, and local-AI status settings.
- Support retention choices and deliberate clear-activity behavior.
- Explain local-only data boundaries in user-facing language.

## Guardrails

- Store only the minimum structured information needed for history.
- Destructive clear actions need confirmation and a precise target.

## Done When

- [ ] Activity loading, empty, filtered, retained, and cleared states work.
- [ ] Privacy and retention behavior are covered by focused tests.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`
- Manual destructive-action confirmation check.
