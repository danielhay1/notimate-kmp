# 008 — Extraction Pipeline

**Status:** Shared slice implemented and validated; awaiting independent review and user merge
**Depends on:** 006, 007
**Expected branch:** `feature/extraction-pipeline` (base: `dev`)

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Turn minimized notification input into validated calendar proposals or safe fallback
states using an on-device pipeline.

## Scope

- Define the extractor contract and strict validated output schema.
- Implement a deterministic rule-based calendar extractor first.
- Model ready, unavailable, downloading, unsupported, deferred, and failed states.
- Gate heavy work by memory, battery, and thermal conditions.
- Use WorkManager only when deferred data can remain privacy-safe.

## Guardrails

- Model output is advisory until schema and policy validation pass.
- Never send raw content to cloud services or durable unstructured queues.

## Done When

- [x] Valid, ambiguous, invalid, and constrained-device paths have focused tests.
- [x] Each input resolves to an explicit outcome without unsafe side effects.

## Validation

- `./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug --no-daemon`

## Approved First Slice

PRs #6 (domain), #7 (capture), and #8 (configuration) are merged. Starting `dev`:
`0df528d7cdfd6b558dcd89c27f5c99fba4e84bc5`. Task `007` proposal/activity storage,
retention, expiry, and clear-data decisions remain incomplete.

commonMain owns `NotificationExtractor`, a deterministic English labeled-block
extractor, strict `calendar.v1` JSON decoding/validation, local-AI/resource state
contracts, and a selected-Automation policy pipeline. Exact grammar, schema, and
processing limits are in `specs/001-notimate-android-mvp/spec.md`.

Rules parse explicit `Event/Meeting/Appointment`, `Date: YYYY-MM-DD`, `Time: HH:mm`,
and optional `Location` fields separated by newline or semicolon. Unknown narrative
and labels are unsupported. Missing, repeated, and invalid fields never receive
invented values. Time-zone/DST and Calendar handoff remain task `010`.

Confidence, sensitivity, threshold, clock, and nullable expiry are caller-supplied
policies, without runtime calibration or retention defaults. Complete low-confidence
or sensitive candidates return content-free review outcomes. Ambiguous fields can
produce NeedsReview proposals. All proposals remain advisory and local-only.

Basic rules work across every optional AI state and device constraint. Heavy work
requires Ready and known available memory/battery/thermal signals; otherwise rules
run first. Deferred outcomes contain no raw input and promise no scheduled retry.
No model runtime/download, resource probes, WorkManager, UI, or external writes
are introduced. Production capture remains unavailable until durable structured
storage and policies exist, so no proposal is discarded as a successful capture.

## Remaining Slices

- Task `007` structured proposal/activity repository and approved retention/expiry/
  clear-data policies before enabling production capture.
- Calibrated confidence/sensitivity handling and any broader parsing languages or
  natural-date formats require separate requirements.
- Real optional local inference, downloads, Android resource probes/thresholds,
  and privacy-safe scheduled retries require separate approval.

## Android-First Validation

- `./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug --no-daemon`
- Synthetic common business/privacy tests; no UI tests.
- Complete-diff whitespace and redacted secret scan before publication.
- Device install/launch/capture/revocation reported separately; no device was
  connected at the initial check on 2026-10-02. iOS runtime verification is deferred.

## Observed Local Validation — 2026-10-02

65 shared Android-host tests (23 new extraction tests) and 13 Android unit tests
passed with zero failures or skips. Android lint and debug APK assembly passed,
as did 23 CI-policy regression tests. Existing Gradle `androidLibrary` deprecation
and native-library stripping notices did not fail validation.

Device availability was rechecked with `adb devices -l`: no device/emulator was
connected. Installation, launch, live capture/revocation, and iOS runtime behavior
remain unverified. No UI tests were run. Complete-diff whitespace and redacted
Gitleaks 8.30.1 scanning of all 15 intended files passed. Independent review,
hosted checks, and human merge remain delivery gates.
