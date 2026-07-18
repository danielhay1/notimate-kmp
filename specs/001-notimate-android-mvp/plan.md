# Implementation Plan: NotiMate Android MVP

## Architecture Summary

The scaffold already contains Android, iOS, and shared modules. The MVP should keep domain, state contracts, agent policy, and extraction schemas in `:shared`, while Android notification access and provider writes stay in Android-specific code.

The notification agent is a local policy engine. Its job is to decide, on device, how to handle a received notification: ignore it, classify it, extract structured data, defer local processing, or create a user-confirmable action draft.

## Proposed Ownership

- `shared/src/commonMain`
  - normalized notification models.
  - classification and extraction result contracts.
  - agent policy rules and action confidence thresholds.
  - privacy redaction rules.
  - action draft models.
  - shared UI state contracts when cross-platform.
- `shared/src/androidMain`
  - Android implementations for shared abstractions only when they must be callable from shared code.
- `androidApp/src/main`
  - `NotificationListenerService`.
  - permission onboarding.
  - WorkManager orchestration.
  - Calendar Provider and local notification integration.
  - Android app navigation shell.
- `iosApp/`
  - iOS app host for shared UI.
  - no cross-app notification interception in MVP.

## Data Flow

1. `NotificationListenerService` receives a notification event.
2. Android mapper converts OS data into a minimized shared `ObservedNotification`.
3. Repository applies blocked-source policy and stores privacy-safe processing metadata.
4. Local agent policy checks source, sensitivity, device conditions, and user settings.
5. Classifier routes the event to ignored, schedule, finance, unknown, or review-needed.
6. Deterministic extraction or local model inference runs only when device conditions are safe.
7. Heavy extraction is deferred through WorkManager if the device is constrained.
8. Schema validation converts extraction output into structured results or review-needed state.
9. Action engine creates user-confirmable drafts for provider writes.
10. UI renders dashboard state from repository-owned source of truth.

## Agent Autonomy Levels

- `Ignore`: deterministic local rules can suppress or ignore low-value notifications.
- `Draft`: useful notifications can become local structured action drafts.
- `Review`: sensitive, ambiguous, low-confidence, or schema-invalid results require user review.
- `ConfirmAndWrite`: external writes require explicit user confirmation in the MVP.
- `Defer`: constrained device state moves local processing to WorkManager.

No MVP path should send raw notification text to a network service or allow model output to bypass schema and policy validation.

## AI Extraction Boundary

Create a `NotificationExtractor` interface in shared/domain terms. The first implementation may use deterministic rules behind the same interface to validate architecture before adding LiteRT or another current Google-recommended on-device inference runtime.

Extractor implementations return strict sealed results. They do not execute side effects. Action execution belongs behind Android platform action handlers and is reached only after policy validation.

## Source Of Truth

- User settings, blocked packages, diagnostics, and action drafts are repository-owned.
- Raw notification text is not a durable source of truth.
- Temporary payload handling should be in memory or short-lived encrypted queues only when required for local extraction.
- UI state is derived from repository/domain state and must not own durable processing truth.

## Testing Strategy

- Common tests for classification, extraction contracts, privacy redaction, and action draft validation.
- Common tests for agent policy decisions and autonomy-level routing.
- Android tests for notification mapping, listener diagnostics, WorkManager behavior, and provider writes.
- iOS tests only for actual iOS-specific shared behavior.

## Open Decisions

- Final on-device inference API and model packaging.
- Whether expense records remain local-only in MVP.
- Retention policy for structured processed records.
- Whether users can enable trusted auto-confirmation later.
