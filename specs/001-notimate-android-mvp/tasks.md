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
- [x] Persist structured Proposals and Activity records only, not raw notification bodies.
- [x] Add retention policy for structured records.

Task `007` is split into configuration (merged through PR #8) and structured results.
The user approved the results slice and interim retention policy on 2026-10-03:
retain until explicitly deleted; preserve caller-supplied nullable expiry without
automatic deletion; clear Activity preserves Proposals/configuration; Proposal
deletion preserves Activity references. Full reset and local-control UI are deferred.
Implementation and validation evidence belong in packet `007`.

## Phase 4: Extraction Pipeline

- [x] Add `NotificationExtractor` interface.
- [x] Implement rule-based calendar extractor as the first architecture-validating adapter.
- [x] Add strict schema/parser contract for future local model output.
- [x] Validate extractor output before creating Proposals.
- [ ] Add WorkManager only for deferred work that needs no durable raw payload.
- [x] Add memory/battery/thermal gate contract before heavy extraction.
- [x] Model ready, downloadable, downloading, unavailable, and unsupported local-AI states.
- [x] Add asynchronous local inference and a strict classification/action envelope.
- [x] Add the Android LiteRT-LM adapter, explicit resource probes, and provisioned-model checks.
- [ ] Qualify a model and target device for inference quality, privacy, latency, and memory.

Task `008` includes deterministic fallback plus the approved local model adapter
and extensible typed classification/action boundary. Provisioning and resource
limits are explicit; no model or target device is implicitly qualified. Model
downloads/UI, confidence calibration, actual promotion filtering/new actions, and
production capture/storage integration remain later slices. WorkManager remains
conditional: no raw content is retained and no retry is scheduled. Delivery and
validation evidence belongs in packet `008`. Results storage, production integration,
and model/device qualification are separate dependencies for activation.

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
