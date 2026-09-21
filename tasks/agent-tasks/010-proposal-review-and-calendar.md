# 010 — Proposal Review And Calendar

**Status:** Proposed
**Depends on:** 008, 009
**Expected branch:** Set in the approved Git plan before work

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Let users review and edit calendar proposals, then hand them to Android Calendar
without overstating completion.

## Scope

- Build Proposal Detail with editable structured calendar fields.
- Explain ambiguous or missing information.
- Add proposal notifications with Review and Dismiss actions.
- Deep link to detail with a normal back stack.
- Launch `ACTION_INSERT` and handle missing or failed handlers.

## Guardrails

- Require confirmation before the external Calendar handoff.
- Report `Opened in Calendar` unless creation can be verified.
- Never include sensitive proposal text in logs or test artifacts.

## Done When

- [ ] Review, edit, dismiss, retry, expiry, and handoff paths are covered.
- [ ] Swiping an alert does not delete the in-app proposal.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`
- Manual Calendar handler and deep-link checks.
