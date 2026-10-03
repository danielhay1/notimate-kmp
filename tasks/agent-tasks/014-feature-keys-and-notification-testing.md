# 014 — Feature Keys And Notification Testing

**Status:** Implemented; delivery validation in the pull request
**Base:** `dev`
**Branch:** `feature/feature-keys`

## Outcome

Provide shared typed JSON feature configuration, persistent debug overrides, and
an Android manual local-notification sender behind one disabled-by-default key.

## Scope

- One bundled JSON and shared manager for Boolean, Int, Float, Double, and String.
- Release loads defaults and opens normally without accessing debug overrides.
- Android debug launcher Activity and iOS debug host show the shared editor.
- Continue validates, saves local overrides, updates the manager, and opens the app.
- Debug-only Android title/body form, permission handling, posting, and settings.

## Guardrails

- Keep the JSON immutable at runtime; persist only feature overrides separately.
- Never persist or log form content; use synthetic notifications for validation.
- Preserve own-package filtering and fail-closed production capture.
- No UI tests, production notification alerts, or iOS native sender in this slice.
- Follow repository `AGENTS.md`; never approve or merge the pull request.

## Validation

- `./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest`
- `./gradlew :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease`
- `./gradlew :shared:iosSimulatorArm64Test`
- Xcode simulator Debug and Release builds.
- Manual Android/iOS launch, Continue, restart, and Android synthetic notification checks.
- Secret scan of the complete intended diff and independent current-head review.

Contract and usage: [`docs/feature-keys.md`](../../docs/feature-keys.md).
