# 004 — Runtime Compose Design System

**Status:** Initial runtime slice complete
**Depends on:** 003
**Expected branch:** `feature/runtime-compose-design-system`

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Implement the approved shared Compose theme, tokens, and core reusable components.

## Scope

- Place platform-neutral UI in `shared/src/commonMain`.
- Add light and dark `NotiMateTheme` schemes and resource-backed strings.
- Implement only components required by the approved inventory.
- Keep components stateless, previewable, adaptive, and RTL-safe.

## Guardrails

- Do not add unrelated navigation, persistence, or dependency injection.
- Use synthetic preview data and existing Google/Kotlin conventions.

## Done When

- [x] Initial theme, semantic colors, spacing, shapes, and two core components are implemented.
- [x] Shared source placement, localization, wrapping, contrast, and touch targets are verified.
- [x] Existing non-UI suites pass; no new business logic or UI tests were introduced.

## Initial Slice

- `NotiMateTheme` extends Material 3 with the approved palette and typography.
- `ProcessingStatusBanner` renders active, paused, permission-required, degraded,
  and unavailable states with explicit recovery events.
- `ProposalCard` renders needs-review, ready, deferred, failed, and expired
  summaries. Expired cards expose no lifecycle actions. Callers supply localized
  structured display text and enforce domain guards before handling events.
- Existing app, feature-key startup, and notification-test UI use the shared theme;
  their behavior and production capture gate are unchanged.
- Central synthetic previews cover light/dark, English/Hebrew RTL, 200% text,
  constrained width, and wider content. Other inventory items remain with their
  screen tasks; task `009` has not started.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`

Observed on 2026-10-12:

- Passed `:shared:testAndroidHostTest` (98 tests), `:androidApp:testDebugUnitTest`
  (40 tests), `:androidApp:lintDebug`, and `:androidApp:assembleDebug`.
- Passed `:shared:allTests :shared:check`, including 98 iOS simulator tests and
  compilation of the shared UI for `iosSimulatorArm64`.
- Explicit text/container pairs pass WCAG AA, minimum calculated ratio 5.49:1.
- Manually rendered synthetic previews on the local Android API 35 emulator using
  Compose tooling's existing debug `PreviewActivity`. Checked semantic states,
  English/LTR, Hebrew/RTL, wrapping at 200% text and 320 dp, and 720 dp presentation.
- Accessibility hierarchy inspected for visible labels and reading order. TalkBack,
  VoiceOver, iOS visual rendering, physical devices, and fold posture are unverified.
- Preview actions are inert fixtures; no real permission, processing, Proposal,
  or Calendar integration is claimed. No UI tests were added.
