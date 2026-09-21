# 009 — Permission Onboarding And Today

**Status:** Proposed
**Depends on:** 004, 006, 007
**Expected branch:** Set in the approved Git plan before work

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Guide users through required Android permissions and present a useful Today screen
for proposals, processing state, and recent outcomes.

## Scope

- Implement education, settings handoff, verification, denial, and revocation states.
- Request `POST_NOTIFICATIONS` only in context.
- Show active profile, proposal queue, processing state, and recent outcomes.
- Preserve useful in-app behavior when proposal notifications are disabled.

## Guardrails

- Use shared stateless Compose UI and explicit state/actions.
- Do not imply that Android permission dialogs can be customized.

## Done When

- [ ] First-run, denied, granted, revoked, empty, loading, and error paths work.
- [ ] English, Hebrew/RTL, dark theme, and text scaling are checked.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`
- Manual Android permission-state checks.
