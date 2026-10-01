# 007 — Persistence And Settings

**Status:** Configuration slice implemented and locally validated; awaiting review and user merge
**Depends on:** 005
**Expected branch:** `feature/persistence-configuration` (base: `dev`)

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

- [ ] Durable ownership and retention behavior are deterministic and tested.
- [ ] Settings survive restart without leaking sensitive payloads.

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
after failure. The processor remains unavailable until task `008`; configuration
persistence does not claim end-to-end analysis. Configuration is local-only in a
no-backup database; automatic Android app backup is disabled.

The UI authority lists an empty Profile state, but the specification and implemented
domain require an Automation. This slice preserves the domain invariant and does
not implement empty Profiles, extra display metadata, or UI management.

Remaining task `007` work: structured proposal/activity storage with independent
origin snapshots; approved retention and expiry policy; clear-activity versus
proposal deletion versus full local reset semantics. No automatic deletion or
app-level reset is introduced in this slice. Configuration remains until explicit
edits or Android app-data clearing.

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
