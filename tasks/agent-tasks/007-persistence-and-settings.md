# 007 — Persistence And Settings

**Status:** Configuration merged through PR #8; results implemented and locally validated
**Depends on:** 005
**Expected branch:** `feature/persistence-results` (base: `dev` at `338d693`)

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Provide local sources of truth for settings, monitored sources, profiles,
automations, proposals, and privacy-safe activity.

## Scope

- Choose storage by data shape and access needs.
- Expose domain models through repository boundaries.
- Enforce one active profile, retention, and clear-data behavior.
- Add focused persistence and repository tests.

## Guardrails

- Do not persist raw notification bodies.
- Keep entities and storage APIs out of domain and UI contracts.
- Use encrypted platform storage only for genuine secrets.

## Done When

- [x] Durable ownership and retention behavior are deterministic and tested.
- [x] Settings survive restart without leaking sensitive payloads.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`

## Configuration-First Slice

The approved first PR uses Android Room for one atomic configuration repository:
processing pause, monitored application identifiers, ordered Profiles, selected
Profile, and owned Automations. Shared callers receive domain models through Flow
and suspend operations. Android owns database entities, schema, and storage wiring.

Personal is initialized only for missing configuration, with calendar suggestions
enabled, `AlwaysReview`, and no monitored sources. Invalid existing data fails
without resetting it. Profile selection, pause, ownership, nonempty Profile rules,
and source admission are enforced across atomic mutations and restart.

Capture consumes committed snapshots and denies content access before loading or
after failure. The processor remains unavailable pending production integration and
model/device qualification. Configuration is local-only in a
no-backup database; automatic Android app backup is disabled.

The UI authority lists an empty Profile state, but the specification and implemented
domain require an Automation. This slice preserves the domain invariant and does
not implement empty Profiles, extra display metadata, or UI management.

Configuration remains until explicit edits or Android app-data clearing.

## Approved Results Slice — 2026-10-03

Following merged PR #9, the user approved structured proposal and privacy-safe
Activity storage before the permission/Today UI slice. Shared domain APIs expose
complete committed snapshots and main-safe operations. Android Room owns a separate
no-backup results database, with immutable origin labels independent of configuration.
Create and revision-guarded lifecycle changes atomically append typed audit metadata.
Corrupt existing rows fail observation and mutations without resetting storage.

Interim retention policy: keep records until explicitly deleted. Nullable expiry is
caller-supplied; reaching it blocks actions, and an expiry command marks the Proposal
Expired without deleting fields. No default duration, automatic deletion, or scheduler
is introduced. Clear Activity preserves Proposals/configuration. Explicit Proposal
deletion preserves Activity references. Full local reset and UI controls are deferred.
Storage never executes provider work; production capture remains unavailable pending
integration and model/device qualification. No UI tests are introduced.

Android-first validation:

- `./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug --no-daemon`
- Focused shared business tests, file-backed repository restart/concurrency/rollback
  tests, and capture loading/failure/privacy tests; no UI tests.
- Redacted secret scan and `git diff --check` before publication.
- Device install, launch, and capture/revocation evidence must be reported separately.
  iOS storage and runtime validation are deferred for this Android-first slice.

## Observed Local Validation — 2026-10-02

42 shared Android-host tests and 13 Android unit tests passed, including seven
file-backed Room repository tests. Android lint and debug APK assembly passed.
The nullable database-path test warning was corrected; Android unit tests and lint
passed again. All 23 CI-policy regression tests passed. Whitespace validation and
the redacted intended-file secret scan passed before commit.

No device/emulator was connected. Installation, launch, and live notification
capture/revocation are unverified. No UI or iOS tests were run. Independent review,
exact-head hosted checks, and user merge remain delivery gates.

## Observed Results Validation — 2026-10-03

88 shared Android-host tests and 36 Android unit tests passed with zero failures,
errors, or skips, including seven new shared policy tests and ten file-backed results
tests. Android lint and debug APK assembly passed. All 24 CI-policy tests passed.
Restart preserves every Proposal state, explanations, nullable expiry, independent
origins, revision, and Activity metadata. Concurrency, stale writes, audit-collision
rollback, atomic observation, clear/delete independence, and corrupt-data failure
are covered. The initial JUnit signature issue was corrected before this passing run.

No Android device/emulator was connected. Install, launch, live capture, and iOS
runtime remain unverified; no UI tests were run. Redacted intended-diff secret
scanning, independent review, exact-head hosted checks, and user merge are delivery
gates. Production capture remains unavailable.
