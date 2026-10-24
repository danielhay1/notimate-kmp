# 009 — Permission Onboarding And Today

**Status:** Approved core slice implemented; delivery validation in the pull request
**Depends on:** 004, 006, 007
**Expected branch:** `feature/permission-onboarding-today` (base: `dev`)

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

- [x] Core first-run, denied, granted, revoked, empty, loading, and error paths are implemented.
- [x] English, Hebrew/RTL, dark theme, and text scaling are checked for the core slice.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`
- Manual Android permission-state checks.

## Approved Core Slice — 2026-10-24

Android welcome/privacy education leads to Notification Access and optional alert
permission education. Each step, previously verified access, and alert-request history
are stored atomically outside automatic backup. Android remains the current grant
authority. Settings return and foreground entry recheck permissions; declined access
opens inactive Today. Alert denial never removes proposals or means listener denial.
Unreadable progress fails with retry rather than resetting existing data.

Today observes the existing configuration and results repositories independently:
selected Profile, source/enabled-Automation counts, pause preference, stored structured
proposals, and the five latest typed Activity outcomes. The outcome limit is display
only. Dismissed/handed-off proposals leave the queue; expired records remain visible
without actions. Explicit deadlines affect display without writing lifecycle events.
Dates/times use locale-aware formatting and the system clock preference; proposal
wall-clock fields are not interpreted as device-zone instants.

Production capture stays `PROCESSOR_UNAVAILABLE`. Granted permissions never imply
active analysis, deterministic fallback, or automatic proposal alerts. Proposal cards
are explicitly view-only; no fake Review/Dismiss/Retry/Calendar buttons are shown.
Only Today and the working permission destination are exposed in the Android shell.
Debug feature-key startup, overrides, native sender, and own-package exclusion remain.
`POST_NOTIFICATIONS` now belongs to the main manifest for normal-app onboarding;
the native test sender remains debug-only.

## Remaining Slices

- Monitored-source selection and the remaining canonical onboarding steps.
- Model readiness/provisioning and production capture/storage integration, requiring
  separate qualified policies and approval before activation.
- Task `010`: Proposal Detail, lifecycle actions, production proposal alerts/deep links,
  and Calendar handoff.
- Task `011`: Profile switching and Profile/Automation management. The older starter
  examples do not seed additional unsupported automations; the actual repository
  default remains Personal, calendar suggestions, AlwaysReview, and zero sources.
- Task `012` full Activity, Settings, retention, and deletion controls; task `013` account/sync.

## Observed Validation — 2026-10-24

Android host/unit tests, lint, debug/release builds, shared multiplatform checks, and
CI-policy regression tests are recorded with final counts in the pull request.
No UI tests were added. State tests cover partial storage failure/retry, save failures,
permission independence, revocation history, contextual effects, pause, and expiry.

Installed and launched on the local Pixel 8 API 35 ARM64 emulator. Verified real
Android settings handoff/return unchanged and granted, contextual runtime alert
denial, app notification settings/re-enable, access revocation on foreground return,
pause/resume, persisted completion after force-stop/relaunch, debug override gating,
and native posting with synthetic input. Posting is not ingestion evidence.
Synthetic Compose debug previews verified English/Hebrew RTL, light/dark, 200% text,
320 dp constraints, and a 720 dp feed. Hierarchy labels/reading order were inspected.
Physical devices, TalkBack/VoiceOver, fold posture, older-OS runtime behavior, OEM
settings-unavailable behavior, and iOS visual rendering remain unverified. Host tests
cover legacy permission mapping and recoverable platform launch failures.
