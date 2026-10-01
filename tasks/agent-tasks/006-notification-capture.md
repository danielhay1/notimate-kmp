# 006 — Notification Capture

**Status:** Implemented; awaiting independent review and user merge
**Depends on:** 005
**Expected branch:** `feature/notification-capture` (base: `dev`)

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
- Reject unmonitored sources before accessing notification extras.
- Fail closed until task `007` supplies user-selected sources and a selected Profile.
- Keep capture transient; task `008` owns extraction, with no durable raw-payload queue.

## Done When

- [x] Capture, minimization, filtering, and revocation behavior are defined.
- [x] Mapper and policy-boundary tests pass with synthetic data.

## Validation

- `./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug --no-daemon`
- Redacted secret scan and `git diff --check` before publication.
- Android device capture/revocation checks only with synthetic notifications; report
  separately from host tests. UI tests and iOS validation are outside this slice.

## Observed validation — 2026-10-01

32 shared Android-host tests and 3 Android mapper tests passed. Android lint and
debug APK assembly passed, along with 24 CI-policy regression tests and whitespace
validation. No device/emulator was connected; live capture, revocation, installation,
and launch are not verified. No UI or iOS tests were run.

Production wiring denies content access until task `007` provides settings and
task `008` provides an available local processor. No notification content is
queued or retained. Listener diagnostics retain only connection and result enums.
