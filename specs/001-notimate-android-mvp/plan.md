# Implementation Plan: NotiMate Android MVP

## Architecture Summary

The scaffold already contains Android, iOS, and shared modules. The MVP should keep domain, state contracts, agent policy, and extraction schemas in `:shared`, while Android notification access and provider writes stay in Android-specific code.

The notification agent is a local policy engine. Its job is to decide, on device, how to handle a received notification: ignore it, classify it, extract structured data, defer safe local processing, or create a user-reviewable proposal.

## Proposed Ownership

- `shared/src/commonMain`
  - normalized notification models.
  - classification and extraction result contracts.
  - agent policy rules and action confidence thresholds.
  - privacy redaction rules.
  - MVVM state contracts and testable shared state holders.
  - `AutomationProfile`, Automation, Proposal, and privacy-safe Activity models.
  - proposal lifecycle and validation rules.
  - shared UI state contracts when cross-platform.
- `shared/src/androidMain`
  - Android implementations for shared abstractions only when they must be callable from shared code.
- `androidApp/src/main`
  - `NotificationListenerService`.
  - Notification Access and proposal-alert permission onboarding.
  - WorkManager orchestration.
  - Calendar insert intent and local proposal notification integration.
  - Android app navigation shell.
- `iosApp/`
  - iOS app host for shared UI.
  - no cross-app notification interception in MVP.

## Data Flow

1. `NotificationListenerService` receives a notification event.
2. Capture checks access, pause, monitored-source membership, and selected enabled
   Automation source selectors before reading content. Android maps only admitted
   input into a minimized shared `ObservedNotification`.
3. Repository supplies an atomic local configuration snapshot and owns privacy-safe
   processing metadata.
4. Local agent policy checks source, active Automation conditions, sensitivity, device conditions, and user settings.
5. Classifier routes the event to ignored, possible-calendar-event, unknown, or review-needed.
6. Deterministic extraction or local model inference runs only when device conditions are safe.
7. WorkManager may defer only work that does not require durably retaining raw notification content.
8. Schema validation converts extraction output into structured results or review-needed state.
9. Action engine creates a user-reviewable calendar proposal.
10. When allowed, NotiMate posts a proposal notification that opens Proposal Detail directly.
11. User-reviewed proposals continue to Android Calendar through `ACTION_INSERT`.
12. UI renders Today, Automations, Activity, and Settings from repository-owned state.

## Agent Autonomy Levels

- `Ignore`: deterministic local rules can suppress or ignore low-value notifications.
- `Draft`: useful notifications can become local structured proposals.
- `Review`: sensitive, ambiguous, low-confidence, or schema-invalid results require user review.
- `ConfirmAndWrite`: external writes require explicit user confirmation; the calendar MVP delegates final save to Android Calendar.
- `Defer`: constrained work has an explicit outcome; WorkManager is conditional on
  separately approved privacy-safe structured retry data.

No MVP path should send raw notification text to a network service or allow model output to bypass schema and policy validation.

## AI Extraction Boundary

Create a `NotificationExtractor` interface in shared/domain terms. The first implementation may use deterministic rules behind the same interface to validate architecture before adding LiteRT or another current Google-recommended on-device inference runtime.

Extractor implementations return strict sealed results. They do not execute side effects. Action execution belongs behind Android platform action handlers and is reached only after policy validation.

## Source Of Truth

- User settings, monitored sources, Profiles, Automations, diagnostics, Proposals, and Activity records are repository-owned.
- Raw notification text is not a durable source of truth.
- Temporary raw payload handling stays in memory. Deferred work carries only privacy-safe structured data.
- Select local storage deliberately: Room for structured proposals and queryable records, DataStore or platform settings for lightweight preferences, and encrypted platform storage for secrets or sensitive credentials.
- UI state is derived from repository/domain state and must not own durable processing truth.

## Testing Strategy

- Common tests for classification, extraction contracts, privacy redaction, and action draft validation.
- Common tests for agent policy decisions and autonomy-level routing.
- Android tests for notification mapping, listener diagnostics, WorkManager behavior, notification deep links, and Calendar intent construction.
- iOS tests only for actual iOS-specific shared behavior.
- Mirror implementation paths in tests and name files with the same subject plus `Test`, for example `AgentPolicy.kt` and `AgentPolicyTest.kt`.
- Do not add UI tests for the MVP. Keep tests focused on logic, business rules, mappers, validation, persistence choices, and privacy behavior.
- Avoid testing framework/library behavior directly.

## Shared Domain Foundation Mapping

Task `005` implements pure contracts under `shared/src/commonMain/.../domain` and
focused tests in `commonTest`. Android host tests exercise those common contracts
for the Android-first MVP; this does not assert iOS runtime verification.

Source admission accepts only source metadata and must run before reading content.
Policy results are advisory and never execute external writes. Profile selection
is separate from global pause. Proposals retain an immutable origin snapshot,
typed ambiguity markers, and local calendar fields; Activity stores typed outcomes
and local identifiers, not free-text content. Redacted string representations are
defense in depth, not permission to log models or persist raw inputs.

Proposal creation starts at `Draft`; validation yields `Ready` or `NeedsReview`.
Dismissal, confirmed successful UI handoff, and expiry are terminal. Failure and
deferral remain retryable without durable raw payloads. Expiry and confidence
thresholds are injected rather than introducing unapproved defaults. The Android
Calendar adapter will resolve wall-clock fields and time-zone/DST ambiguity before
external handoff; this foundation does not implement that adapter.

## Open Decisions

- Final on-device inference API and model packaging.
- Retention policy for structured processed records.
- Whether optional non-sensitive configuration sync ships in the first public release.
- Post-MVP action types and trusted confirmation policies.

## Task 007 Configuration Slice

The Android-first configuration repository uses Room for settings, monitored
application identifiers, Profiles, and their owned Automations. Keeping these in
one database supports atomic selection, pause, source, and ownership updates.
Shared APIs expose domain snapshots through Flow and main-safe suspend operations;
Room entities, converters, relations, and schema remain in `:androidApp`.

A missing installation initializes Personal with calendar suggestions enabled,
`AlwaysReview`, and no monitored applications. Existing unreadable or invalid data
causes a failure; it is not silently replaced. Profiles and Automations retain
their explicit order. The selected Profile survives pause and restart. Deleting
the selected Profile requires an existing replacement in the same transaction;
the last Profile or a Profile's last Automation cannot be deleted.

Capture observes complete committed configuration snapshots. Loading and storage
failure deny source admission; task `008` must supply a real available processor
before content can be read. The configuration observer is process-owned and stops
on storage failure; the repository failure remains observable to other callers.
Restart retries loading without deleting local data.

The database lives in Android's no-backup directory, and automatic app backup is
disabled. No sync, raw payload storage, queue, extraction, or UI is introduced.
Configuration is retained until explicitly edited or Android app data is cleared.
Application-level reset, proposal/activity storage, retention durations, proposal
expiry, and clear-data scope need the next task `007` slice and approved policy.

`03_UI_STATES_AND_CONTENT.md` lists an empty Profile state, while specification
Requirement 7 and `AutomationProfile` require at least one Automation. This slice
preserves the existing specification/domain invariant; empty Profile support
requires a separate aligned product decision.

## Task 008 Shared Extraction Slice

`domain/extraction` in commonMain owns the synchronous extractor contract,
deterministic labeled-block adapter, strict streaming JSON decoder, resource-gate
contracts, and policy-to-Proposal pipeline. Tests mirror these files in commonTest.
The JSON library uses a custom decoder without adding a serialization compiler
plugin. Duplicate keys are rejected during decoding; parser exceptions never leave
the boundary, and debug input is disabled in decoder exceptions.

The pipeline resolves one explicitly requested Automation in the selected Profile
from a single configuration snapshot. It retains neither raw input nor a queue.
It returns structured local results to its caller; future storage must commit them
before production capture is enabled. Calendar origin labels are independent
snapshots. No repository, UI state holder, notification alerts, or Calendar adapter
is added; existing Room configuration/Flow and fail-closed capture wiring remain.

Confidence, sensitivity, clock, expiry, and threshold are injected without runtime
defaults. Complete candidates routed to review by policy produce a content-free
review outcome; incomplete/ambiguous fields may produce NeedsReview proposals.
Every Calendar candidate, including future adapter output, is schema-validated.

Heavy adapters require Ready and all resource readings Available. Unknown readings
deny heavy work. Deterministic fallback can still produce a Proposal, ignore, or
review outcome; unsupported fallback becomes a typed unavailable/deferred/failure
outcome. Cancellation propagates; ordinary adapter failure exposes no exception
details and attempts deterministic fallback for heavy work.

Parsing/language and schema limits are defined in the specification's Task 008
section. Android time-zone/DST resolution stays with task 010. Actual local-AI
runtime/download support, Android resource probes/thresholds, privacy-safe scheduled
retry, calibrated confidence/sensitivity detection, and production capture/storage
integration remain separately approved slices. WorkManager is intentionally absent
because there is no approved durable retry payload or structured result repository.
