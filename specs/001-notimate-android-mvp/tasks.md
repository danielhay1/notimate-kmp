# Tasks: NotiMate Android MVP

## Phase 0: Scaffold Verification

- [x] Initialize Kotlin Multiplatform project with Android, iOS, and shared modules.
- [x] Add Gradle wrapper, version catalog, and daemon JVM configuration.
- [x] Add Android and iOS app shells from the KMP scaffold.
- [x] Add repository guidance and spec-driven development files.
- [ ] Run and document baseline validation commands.

## Phase 1: Shared Domain Foundation

- [ ] Define minimized `ObservedNotification` domain model.
- [ ] Define classification types: schedule, finance, ignored, unknown, review-needed.
- [ ] Define agent autonomy levels: ignore, draft, review, confirm-and-write, defer.
- [ ] Define agent policy rules for sensitivity, confidence, blocked sources, and device constraints.
- [ ] Define action draft models for calendar, reminder, and expense records.
- [ ] Define privacy redaction policy and tests.
- [ ] Add shared tests for classification, agent policy routing, and validation.

## Phase 2: Android Notification Capture

- [ ] Add `NotificationListenerService` declaration and permission metadata.
- [ ] Implement Android notification-to-domain mapper.
- [ ] Implement listener connection diagnostics without logging notification bodies.
- [ ] Add blocked package filtering.
- [ ] Add unit tests for mapper and filtering behavior.

## Phase 3: Persistence And Settings

- [ ] Choose Android-first persistence strategy.
- [ ] Implement settings and blocked-package source of truth.
- [ ] Persist structured records only, not raw notification bodies.
- [ ] Add retention policy for structured records.

## Phase 4: Extraction Pipeline

- [ ] Add `NotificationExtractor` interface.
- [ ] Implement rule-based extractor as the first architecture-validating adapter.
- [ ] Add strict schema/parser contract for future local model output.
- [ ] Validate extractor output before creating action drafts.
- [ ] Add WorkManager worker for deferred extraction.
- [ ] Add memory/battery/thermal gate before heavy extraction.

## Phase 5: Action Engine And UI

- [ ] Build notification access onboarding.
- [ ] Build dashboard with counts, pause/resume, diagnostics, and blocked packages.
- [ ] Build review queue for ambiguous drafts.
- [ ] Require user confirmation before external provider writes.
- [ ] Integrate confirmed schedule actions with Android Calendar Provider.
- [ ] Add recoverable error handling for provider failures.
