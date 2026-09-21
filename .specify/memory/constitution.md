# NotiMate Constitution

## Product Principles

NotiMate turns transient mobile notifications into structured, private, useful actions. The product should reduce notification overload while preserving user trust.

## Non-Negotiable Privacy Rules

- Raw notification payloads stay on device.
- Logs, analytics, crash reports, screenshots, and tests must not contain notification bodies, MFA codes, financial balances, personal messages, or other sensitive payload data.
- API keys, tokens, credentials, signing secrets, and other sensitive values must not be embedded in source code, tests, specs, screenshots, or committed configuration.
- Secrets must use structured safe configuration: Gradle properties or environment variables for build-time values, Android Keystore or encrypted storage for runtime secrets, and ignored local files for developer-only values.
- Documentation may mention required secrets by placeholder name only.
- Before every commit and pull-request update, scan the complete intended diff
  and changed filenames for secrets and sensitive user data without printing
  matched values.
- A likely committed credential is a merge blocker. If one was published, revoke
  or rotate it; history cleanup is secondary and requires explicit approval.
- Cloud processing must not receive raw notification text. Future cloud features may receive only minimized structured data with explicit user consent.
- Test fixtures and demos must use synthetic notification content.

## Local Agent Principles

- The notification agent is an on-device policy and action engine, not a cloud-first assistant.
- The default pipeline is observe -> minimize -> classify -> extract locally -> produce a structured draft -> ask for confirmation when an external write is involved.
- The agent may ignore low-value notifications automatically when rules are deterministic and explainable.
- The agent must not automatically create calendar events, reminders, expense records, or provider writes in the MVP without user confirmation.
- Ambiguous, low-confidence, or sensitive notifications must become review items instead of automatic actions.
- Model output is advisory until validated against strict schemas and policy rules.

## Platform Strategy

- Build Android first while preserving KMP boundaries.
- Use shared code for platform-neutral domain models, schemas, routing rules, validation, redaction, and state logic.
- Keep OS integrations at platform edges.
- Do not promise cross-app notification interception on iOS. iOS support is deferred to own-app notification extensions or external BLE/ANCS-style integrations.

## Architecture Principles

- `:shared` owns shared UI, domain models, parsing contracts, routing rules, extraction schemas, privacy policies, and shared tests.
- `:androidApp` owns Android app shell, manifest, permissions, `NotificationListenerService`, WorkManager, local providers, and lifecycle behavior.
- `iosApp/` owns the Xcode shell and iOS hosting code.
- Prefer modern Kotlin, Compose Multiplatform, Compose, AndroidX, and coroutine APIs for implementation decisions.
- Use MVVM as the default application architecture: composables render state, ViewModels or shared state holders coordinate actions, and repositories own durable truth.
- Keep platform APIs out of `commonMain`.
- Prefer interfaces plus injected platform implementations over premature `expect`/`actual`.
- Prefer constructor injection and dependency-injection-friendly package structure over service locators or hidden global state.
- Repositories expose domain models, not DTOs or database entities.
- Composables render immutable state only; state holders coordinate actions and state.
- Platform entry points delegate quickly; services, activities, and workers must not become business-logic containers.
- Durable source of truth belongs in repositories/persistence, not UI or Android services.
- Choose local storage deliberately: Room for relational/queryable durable records, DataStore or platform settings for simple preferences, encrypted platform storage for secrets, and short-lived encrypted queues only when notification payloads must temporarily survive process death.

## Testability Principles

- Design code for testability with pure functions, injected dependencies, stable clocks/dispatchers, and deterministic state transitions.
- Mirror implementation paths in test source sets and use a `Test` filename suffix.
- Do not test UI. Test business logic, agent policy, mappers, validation, redaction, and persistence decisions.
- Do not over-test framework or library behavior.

## Workflow Principles

- Start implementation work only after a short user-approved Git plan.
- After plan approval, provide a high-level branch/merge diagram and the command list before code changes.
- Keep each feature or bug fix isolated on its own branch.
- Use `feature/<short-kebab-name>` for feature branches and `bug/<short-kebab-name>` for bug branches.
- Prefer `main -> dev -> feature/*` or `main -> dev -> bug/*`; pull requests merge back into the correct base branch after user review.
- An approved Git plan authorizes its task-scoped branch, commits, non-protected
  pushes, pull-request creation or updates, independent review, and validated
  review fixes. It never authorizes a merge.
- Never commit or push directly to `dev` or `main`, approve an agent-authored pull
  request, bypass push protection, force-push published history, or delete a
  branch or worktree without the user's explicit authorization.
- For long-running or high-effort work, split research, implementation, review, and validation across workflows or multiple agents when useful.

## Reliability Principles

- Treat OEM battery restrictions as a first-class Android risk.
- Notification listener recovery and diagnostics must be designed intentionally.
- Heavy local inference must check memory, battery, and thermal pressure before running.
- Use graceful degradation: defer extraction through WorkManager when device conditions are poor.
- Avoid permanent background work unless user-visible and justified.
- If local inference is unavailable, the agent must degrade to deterministic rules, queued review, or deferred work rather than sending raw payloads to a network service.

## Source Layout Principles

- `shared/src/commonMain`: platform-neutral UI/domain logic and shared abstractions.
- `shared/src/commonTest`: platform-neutral tests.
- `shared/src/androidMain`: Android implementations for shared abstractions.
- `shared/src/iosMain`: iOS implementations for shared abstractions.
- `androidApp/src/main`: Android app shell and platform declarations.
- `iosApp/`: Xcode iOS app shell.
