# Specification: NotiMate Android MVP

## Overview

Build the Android-first MVP of NotiMate: an on-device smart notification manager that observes selected notification sources, identifies possible calendar events, extracts structured data locally, and creates user-reviewed proposals.

The preferred agent behavior is local-first: every received notification is minimized, classified, and handled on device whenever possible. The agent should produce validated structured proposals and policy decisions, not free-form autonomous side effects.

The first complete action journey is a calendar-event proposal. Reminders, financial records, additional actions, automatic profile switching, and direct provider writes remain post-MVP candidates.

## Requirement 1: Permission Onboarding

**User story:** As a privacy-conscious user, I want a clear onboarding flow before granting notification access so that I understand what NotiMate can read and what stays on device.

### Acceptance Criteria

1. The app explains why notification access is required before opening Android settings.
2. The app states that raw notification content is processed locally.
3. The app detects whether notification access is granted.
4. The app handles denied, granted, and revoked permission states.
5. The app separately explains proposal alerts before requesting `POST_NOTIFICATIONS` on Android 13 or later.
6. If proposal notifications are denied or disabled, processing can continue and proposals remain available in the app.
7. Login is not required to complete onboarding or use the local product.

## Requirement 2: Notification Interception

**User story:** As a user, I want NotiMate to observe incoming notifications so that useful events can be detected automatically.

### Acceptance Criteria

1. Android registers a `NotificationListenerService`.
2. The service extracts only the minimum fields needed for processing.
3. The service processes only sources allowed by the user's monitored-source settings and active Profile.
4. The service records privacy-safe diagnostics when listener connection changes.
5. The product does not claim access to full message or conversation history.

## Requirement 3: Local Classification And Extraction

**User story:** As a user, I want NotiMate to identify possible calendar events without manually reviewing every notification.

### Acceptance Criteria

1. Notifications are classified into possible-calendar-event, ignored, unknown, or review-needed outcomes.
2. Extracted output uses strict structured models.
3. Ambiguous extraction is marked for review instead of auto-action.
4. Raw payloads are not logged or sent to the network.
5. Model output is validated against schemas before any action draft is created.
6. Low-confidence extraction produces `review-needed`, not an automatic action.
7. When optional local generative AI is unavailable, supported deterministic processing and structured automation management remain usable.

## Requirement 4: On-Device Agent Policy

**User story:** As a user, I want the notification agent to act conservatively and privately so that useful automation does not become surprising or unsafe.

### Acceptance Criteria

1. The default policy is on-device processing for received notifications.
2. The agent can automatically ignore deterministic low-value notifications.
3. The agent can create structured action drafts for useful notifications.
4. The agent cannot write to external providers without user confirmation in the MVP.
5. The agent routes sensitive or ambiguous notifications to review.
6. If local inference is unavailable, the agent falls back to deterministic rules, structured review, or deferred local work that does not durably retain raw notification text.

## Requirement 5: Calendar Proposal

**User story:** As a user, I want a possible event found in a notification to become an editable calendar proposal that I control.

### Acceptance Criteria

1. A possible event can produce a local structured proposal containing editable title, date, time, and location fields.
2. Proposal Detail identifies the originating source, Profile, and Automation without exposing raw notification history.
3. Ambiguous or missing required fields are highlighted for review.
4. The user can continue to Android Calendar through a pre-filled `ACTION_INSERT` intent.
5. The MVP does not request direct calendar read/write permission.
6. The product uses `Opened in Calendar` unless event creation is verifiably completed.
7. Failed handoffs expose recoverable errors without sensitive content.

## Requirement 6: Today And Controls

**User story:** As a user, I want visibility and control over what NotiMate processed.

### Acceptance Criteria

1. Today shows processing state, the active Profile, proposals needing attention, and recent privacy-safe outcomes.
2. Users can choose monitored notification sources.
3. Users can inspect structured proposals without exposing raw notification history.
4. Users can pause notification processing.
5. Swiping away a proposal notification removes the alert but does not reject the underlying proposal.
6. The notification body and `Review` action open the specific Proposal Detail destination directly.
7. An explicit `Dismiss` action rejects the proposal.

## Requirement 7: Notification Profiles And Automations

**User story:** As a user, I want to group notification automations into selectable Notification Profiles so that I can change how NotiMate behaves.

### Acceptance Criteria

1. Every local installation has at least one Notification Profile.
2. Exactly one Profile is active while processing is enabled in the MVP.
3. A Profile contains one or more user-visible Automations.
4. Each Automation defines sources, conditions, an action, and a confirmation policy.
5. Switching Profiles affects newly received notifications only.
6. Existing proposals retain their originating Profile and Automation.
7. Pausing processing does not delete or replace the selected Profile.
8. Profile management lives under Automations rather than Account settings.
9. External actions use `Always review` in the MVP.
10. The implementation model is named `AutomationProfile`; `Account` remains reserved for login and sync.

## Requirement 8: Optional Account And Sync

**User story:** As a user, I want NotiMate to work without an account and to opt into supported configuration backup only when I choose.

### Acceptance Criteria

1. Core local features work while signed out.
2. Sign-in is offered contextually for backup and sync, not as an onboarding gate.
3. Enabling backup requires a separate explicit opt-in after sign-in.
4. Raw notification content, proposal content, model prompts, and activity history are never synced.
5. Signing out stops future sync without deleting local Profiles and Automations.
6. Sensitive free-text conditions remain local unless a separately specified client-side encryption design is implemented.

## Product Design Authority

Approved screen, flow, state, domain, and design requirements live in `docs/product-design/`.
