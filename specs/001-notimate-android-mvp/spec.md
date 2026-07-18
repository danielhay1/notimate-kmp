# Specification: NotiMate Android MVP

## Overview

Build the Android-first MVP of NotiMate: an on-device smart notification manager that observes notifications, classifies useful events, extracts structured data locally, and creates user-approved actions.

The preferred agent behavior is local-first: every received notification is minimized, classified, and handled on device whenever possible. The agent should produce structured drafts and policy decisions, not free-form autonomous side effects.

## Requirement 1: Permission Onboarding

**User story:** As a privacy-conscious user, I want a clear onboarding flow before granting notification access so that I understand what NotiMate can read and what stays on device.

### Acceptance Criteria

1. The app explains why notification access is required before opening Android settings.
2. The app states that raw notification content is processed locally.
3. The app detects whether notification access is granted.
4. The app handles denied, granted, and revoked permission states.

## Requirement 2: Notification Interception

**User story:** As a user, I want NotiMate to observe incoming notifications so that useful events can be detected automatically.

### Acceptance Criteria

1. Android registers a `NotificationListenerService`.
2. The service extracts only the minimum fields needed for processing.
3. The service ignores notifications from blocked packages.
4. The service records privacy-safe diagnostics when listener connection changes.

## Requirement 3: Local Classification And Extraction

**User story:** As a user, I want NotiMate to identify schedule and financial notifications without manual input.

### Acceptance Criteria

1. Notifications are classified into schedule, finance, ignored, unknown, or review-needed.
2. Extracted output uses strict structured models.
3. Ambiguous extraction is marked for review instead of auto-action.
4. Raw payloads are not logged or sent to the network.
5. Model output is validated against schemas before any action draft is created.
6. Low-confidence extraction produces `review-needed`, not an automatic action.

## Requirement 4: On-Device Agent Policy

**User story:** As a user, I want the notification agent to act conservatively and privately so that useful automation does not become surprising or unsafe.

### Acceptance Criteria

1. The default policy is on-device processing for received notifications.
2. The agent can automatically ignore deterministic low-value notifications.
3. The agent can create structured action drafts for useful notifications.
4. The agent cannot write to external providers without user confirmation in the MVP.
5. The agent routes sensitive or ambiguous notifications to review.
6. If local inference is unavailable, the agent falls back to deterministic rules, queued review, or deferred local work.

## Requirement 5: Action Engine

**User story:** As a user, I want useful notifications to become calendar, reminder, or expense actions.

### Acceptance Criteria

1. Schedule notifications can produce draft calendar/reminder actions.
2. Financial notifications can produce draft expense records.
3. The MVP requires user confirmation before writing to external system providers.
4. Failed actions expose recoverable errors without sensitive content.

## Requirement 6: Dashboard And Controls

**User story:** As a user, I want visibility and control over what NotiMate processed.

### Acceptance Criteria

1. The dashboard shows counts of processed, ignored, and review-needed notifications.
2. Users can block or unblock notification source packages.
3. Users can inspect structured extracted results without exposing raw notification history by default.
4. Users can pause notification processing.
