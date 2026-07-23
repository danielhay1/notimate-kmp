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
2. Android mapper converts OS data into a minimized shared `ObservedNotification`.
3. Repository applies monitored-source and active-profile policy and stores privacy-safe processing metadata.
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
- `Defer`: constrained device state moves local processing to WorkManager.

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

## Open Decisions

- Final on-device inference API and model packaging.
- Retention policy for structured processed records.
- Whether optional non-sensitive configuration sync ships in the first public release.
- Post-MVP action types and trusted confirmation policies.
