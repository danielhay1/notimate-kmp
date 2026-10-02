# 008 — Extraction Pipeline

**Status:** Local model extension implemented; validation and independent review in progress
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
The first slice introduced no model runtime/download or resource probes. The approved
extension below adds runtime and probes. Production capture remains unavailable until
structured storage and policies exist, so no proposal is discarded as a successful capture.

## Approved Local Model Extension — 2026-10-03

The user approved a real local model adapter and an architecture that can grow to
promotion classification/filtering and additional suggested actions. In this slice,
classification and typed suggested actions are separate; calendar is the only
supported proposal payload. Matching promotion classification returns review and filtering
remains future work. New action payloads must have explicit schema, policy, and
proposal handling; unknown actions never execute.

Shared extraction is asynchronous. The local model receives the bounded title/body
as untrusted JSON data with explicit caller-resolved posting date/time, time zone,
and locale. The `notification.v1` envelope validates classification, confidence,
sensitivity, and the action-specific `calendar.v1` payload. Model confidence can
only lower caller confidence; model sensitivity cannot clear caller sensitivity.
Deterministic rules remain fallback for unavailable/constrained inference.

Android owns pinned LiteRT-LM 0.17.1, a CPU adapter, private no-backup model-file
confinement/SHA256 verification, explicit resource policies and budgets, bounded
output, isolated conversations, and cancellation-aware cleanup. There is no model
bundle/download UI, network permission, raw queue, WorkManager, or production
activation. The caller supplies a qualified immutable model and limits.

Native initialization cannot be interrupted; timeout/cancellation cleanup waits for
native acknowledgement, which can exceed the deadline. A stalled runtime stays
busy rather than closing active JNI resources. Target-device qualification remains
required; current host tests exercise fakes, not actual model accuracy/performance.

## Remaining Slices

- Task `007` structured proposal/activity repository and approved retention/expiry/
  clear-data policies before enabling production capture.
- Model/device qualification, calibrated confidence/sensitivity, and demonstrated
  language/natural-date accuracy before production activation.
- Model provisioning/download UI and privacy-safe scheduled retries.
- Actual promotion filtering and additional actions with defined permissions,
  confirmation, persistence, and recovery behavior.

## Android-First Validation

- `./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug --no-daemon`
- Synthetic common business/privacy tests; no UI tests.
- Complete-diff whitespace and redacted secret scan before publication.
- Device install/launch/capture/revocation reported separately; no device was
  connected at the initial check on 2026-10-02. iOS runtime verification is deferred.

## Observed Local Validation — 2026-10-02

66 shared Android-host tests (24 new extraction tests) and 13 Android unit tests
passed with zero failures or skips. Android lint and debug APK assembly passed,
as did 23 CI-policy regression tests. Existing Gradle `androidLibrary` deprecation
and native-library stripping notices did not fail validation.

Device availability was rechecked with `adb devices -l`: no device/emulator was
connected. Installation, launch, live capture/revocation, and iOS runtime behavior
remain unverified. No UI tests were run. Complete-diff whitespace and redacted
Gitleaks 8.30.1 scanning of all 15 intended files passed. Independent review,
hosted checks, and human merge remain delivery gates.

## Observed Local Validation — 2026-10-03

81 shared Android-host tests and 26 Android unit tests passed with zero failures,
errors, or skips. Android lint, debug APK assembly, and all 24 CI-policy tests passed.
No UI tests were run. No Android device/emulator was connected; install, launch,
live capture, cancellation under device pressure, and iOS runtime remain unverified.

A synthetic Mac ARM64 JVM experiment used official LiteRT-LM 0.17.1 CPU inference
and `litert-community/Qwen3-0.6B` revision
`a3c5d805ae362dff7f580bc25f2dfb9a5a7eaa76`. It exposed an unsupported `uniqueItems`
generation-schema keyword. That keyword was removed; shared validation still
rejects duplicate ambiguous fields. After one bounded prompt refinement, all four
calendar, promotion, ambiguity, and hostile-instruction outputs passed strict
schema validation. Calendar facts remained inaccurate, including an unstated time.
The candidate is unqualified; syntactic acceptance is not extraction accuracy.
Observed per-case duration was 5.2–7.8 seconds including fresh engine initialization,
on Mac CPU only. This is no Android benchmark or qualified budget.

Models and the temporary experiment remained outside the repository. No real
notification input or generated payload was saved or logged. Production activation,
model/device qualification, independent review, and hosted checks remain separate gates.
