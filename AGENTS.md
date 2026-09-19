# Repository Guidelines

NotiMate is a Kotlin Multiplatform project built with Compose Multiplatform.
Preserve unrelated user changes and follow the most specific applicable skill.

## Required Skills

- For Kotlin Multiplatform, Android, Compose, Gradle, shared domain/data, or
  architecture work, use `$google-kmp-senior-developer` together with
  `$notimate-kmp-development-conventions`.
- For code comments, KDoc, TODOs, or documentation affected by implementation,
  use `$notimate-kmp-comments-and-kdoc`.
- For commit messages and pull-request titles or bodies, use
  `$notimate-kmp-git-writting-conventions`.
- For independent PR and merge-readiness reviews, use
  `$notimate-kmp-pr-reviewer`.
- Add a more specialized KMP skill when the task materially concerns its domain,
  such as testing, navigation, state management, platform bridges, data, UI,
  modularization, or Gradle governance.

## Git Workflow and Authorization

Before implementation work, provide a short Git plan for the feature or bug fix
and wait for user approval. After approval, provide a short branch/merge diagram
and the exact command list before changing code.

Use isolated branches:

```text
main
  dev
    feature/<short-kebab-name>
    bug/<short-kebab-name>
```

Create and check out the approved branch before implementation. Keep the work
isolated from unrelated changes. Open a pull request into the approved base only
after validation. Never commit, push, open or approve a pull request, or merge
without the user's authorization for that action.

## Specification Authority

Before implementing features, read `.specify/memory/constitution.md` and the
relevant `specs/<number>-<feature>/` files. Keep `spec.md`, `plan.md`, and
`tasks.md` aligned when decisions change.

Use `docs/product-design/` as the product and design authority. Use the relevant
numbered packet in `tasks/agent-tasks/` as execution context. If sources are
missing or contradictory, report the exact gap instead of inventing behavior.

For long-running or high-effort work, use workflows or multiple agents to split
research, implementation, review, and validation. If the scope is unclear, ask
the user whether to use a workflow before starting.

## Security and Privacy

Raw notification payloads must stay on device. Do not log, persist, upload, or
include sensitive notification bodies in tests, screenshots, analytics, crash
reports, durable queues, or network requests. Use synthetic notification data.

Do not embed API keys, tokens, credentials, signing secrets, or sensitive values
in source code, tests, specs, screenshots, comments, KDoc, or committed
configuration. Use structured platform-safe configuration and document required
keys with placeholder names only.

## Notification Agent Invariants

Treat the NotiMate agent as a local policy engine:

1. Reject globally unmonitored sources before reading notification content.
2. Minimize OS notification data at the Android boundary.
3. Classify and extract on device.
4. Validate model or rule output against strict schemas.
5. Produce ignored, draft, review-needed, deferred, or failed states.
6. Keep model output separate from side effects.
7. Require user confirmation before external provider writes in the MVP.

Do not implement cloud-first processing, raw-payload upload, or direct
model-to-side-effect execution. Do not claim a calendar event was created when
only the editor was opened.
