# Tasks: NotiMate Android MVP

## Phase 0: Scaffold Verification

- [x] Initialize Kotlin Multiplatform project with Android, iOS, and shared modules.
- [x] Add Gradle wrapper, version catalog, and daemon JVM configuration.
- [x] Add Android and iOS app shells from the KMP scaffold.
- [x] Add repository guidance and spec-driven development files.
- [x] Run and document baseline validation commands.

## Phase 1: Shared Domain Foundation

- [x] Define minimized `ObservedNotification` domain model.
- [x] Define classification types: possible-calendar-event, ignored, unknown, review-needed.
- [x] Define agent autonomy levels: ignore, draft, review, confirm-and-write, defer.
- [x] Define `AutomationProfile` and Automation models with one-active-profile invariants.
- [x] Define agent policy rules for sensitivity, confidence, monitored sources, active Automations, and device constraints.
- [x] Define calendar Proposal lifecycle and editable structured fields.
- [x] Define privacy-safe Activity record models.
- [x] Define privacy redaction policy and tests.
- [x] Add shared tests for classification, Profile invariants, agent policy routing, proposal lifecycle, and validation.

Task `005` merged into `dev` through PR #6. See its task packet for observed
commands and intentionally deferred platform integration.

## Phase 2: Android Notification Capture

- [x] Add `NotificationListenerService` declaration and permission metadata.
- [x] Implement Android notification-to-domain mapper.
- [x] Implement listener connection diagnostics without logging notification bodies.
- [x] Add monitored-source filtering and active-profile lookup.
- [x] Add unit tests for mapper and filtering behavior.

Task `006` implements the capture boundary with fail-closed production wiring.
Settings and local extraction remain tasks `007` and `008`. Host validation passed;
live device capture/revocation remains unverified. Task `006` merged through PR #7.

## Phase 3: Persistence And Settings

- [x] Choose Room for the Android-first atomic configuration source of truth.
- [x] Implement settings, monitored-source, Profile, and Automation source of truth.
- [ ] Persist structured Proposals and Activity records only, not raw notification bodies.
- [ ] Add retention policy for structured records.

Task `007` is split into configuration first and structured proposal/activity storage
after its dependency merge. Configuration merged through PR #8; task `007` remains
incomplete. Configuration validation and delivery evidence belong
in the task packet. Automatic retention/deletion, expiry, and clear-data scope
require an explicit product decision before the second slice.

## Phase 4: Extraction Pipeline

- [x] Add `NotificationExtractor` interface.
- [x] Implement rule-based calendar extractor as the first architecture-validating adapter.
- [x] Add strict schema/parser contract for future local model output.
- [x] Validate extractor output before creating Proposals.
- [ ] Add WorkManager only for deferred work that needs no durable raw payload.
- [x] Add memory/battery/thermal gate contract before heavy extraction.
- [x] Model ready, downloadable, downloading, unavailable, and unsupported local-AI states.

Task `008` first delivers shared contracts and explicit conservative outcomes using
the bounded grammar in `spec.md`. Actual inference, download, resource probes and
thresholds, confidence calibration, and production capture/storage integration
remain follow-up slices. WorkManager is conditional: no raw content is retained
and no retry is scheduled in this slice. Delivery/validation evidence belongs in
task packet `008`; task `007` structured storage remains a dependency for activation.

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
