# Tasks: NotiMate Android MVP

## Phase 0: Scaffold Verification

- [x] Initialize Kotlin Multiplatform project with Android, iOS, and shared modules.
- [x] Add Gradle wrapper, version catalog, and daemon JVM configuration.
- [x] Add Android and iOS app shells from the KMP scaffold.
- [x] Add repository guidance and spec-driven development files.
- [ ] Run and document baseline validation commands.

## Phase 1: Shared Domain Foundation

- [ ] Define minimized `ObservedNotification` domain model.
- [ ] Define classification types: possible-calendar-event, ignored, unknown, review-needed.
- [ ] Define agent autonomy levels: ignore, draft, review, confirm-and-write, defer.
- [ ] Define `AutomationProfile` and Automation models with one-active-profile invariants.
- [ ] Define agent policy rules for sensitivity, confidence, monitored sources, active Automations, and device constraints.
- [ ] Define calendar Proposal lifecycle and editable structured fields.
- [ ] Define privacy-safe Activity record models.
- [ ] Define privacy redaction policy and tests.
- [ ] Add shared tests for classification, Profile invariants, agent policy routing, proposal lifecycle, and validation.

## Phase 2: Android Notification Capture

- [ ] Add `NotificationListenerService` declaration and permission metadata.
- [ ] Implement Android notification-to-domain mapper.
- [ ] Implement listener connection diagnostics without logging notification bodies.
- [ ] Add monitored-source filtering and active-profile lookup.
- [ ] Add unit tests for mapper and filtering behavior.

## Phase 3: Persistence And Settings

- [ ] Choose Android-first persistence strategy.
- [ ] Implement settings, monitored-source, Profile, and Automation source of truth.
- [ ] Persist structured Proposals and Activity records only, not raw notification bodies.
- [ ] Add retention policy for structured records.

## Phase 4: Extraction Pipeline

- [ ] Add `NotificationExtractor` interface.
- [ ] Implement rule-based calendar extractor as the first architecture-validating adapter.
- [ ] Add strict schema/parser contract for future local model output.
- [ ] Validate extractor output before creating Proposals.
- [ ] Add WorkManager only for deferred work that needs no durable raw payload.
- [ ] Add memory/battery/thermal gate before heavy extraction.
- [ ] Model ready, downloadable, downloading, unavailable, and unsupported local-AI states.

## Phase 5: Permission And Proposal UI

- [ ] Build Notification Access education, system-settings handoff, and verification.
- [ ] Build in-context `POST_NOTIFICATIONS` education and request.
- [ ] Build Today with processing state, active Profile, proposal queue, and recent outcomes.
- [ ] Build Proposal Detail with editable calendar fields and ambiguity guidance.
- [ ] Post standard Android proposal notifications with `Review` and `Dismiss`.
- [ ] Deep link notification body and `Review` directly to Proposal Detail with a normal back stack.
- [ ] Keep proposals in Today when alerts are swiped away or proposal notifications are disabled.
- [ ] Integrate reviewed proposals with Android Calendar through `ACTION_INSERT`.
- [ ] Use `Opened in Calendar` unless event creation is verifiably completed.
- [ ] Add recoverable error handling for missing Calendar handlers and failed handoffs.

## Phase 6: Profiles And Automations

- [ ] Build active Profile switcher.
- [ ] Build Automations list for the selected Profile.
- [ ] Build Manage Profiles and Profile Detail.
- [ ] Enforce exactly one active Profile and prevent deleting the only Profile.
- [ ] Build structured `When / Then / Confirm` Automation builder.
- [ ] Add optional `Describe an automation` entry only when local AI is available.
- [ ] Convert local-AI output into a validated, editable Automation draft.

## Phase 7: Settings And Optional Account

- [ ] Build permission and notification-channel status settings.
- [ ] Build monitored-app settings.
- [ ] Build on-device AI status and fallback settings.
- [ ] Build local data, retention, and clear-activity controls.
- [ ] Keep sign-in optional and contextual to backup and sync.
- [ ] Document and enforce the local-only versus syncable data boundary before enabling sync.
