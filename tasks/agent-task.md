# Agent Task: NotiMate Product Development

**Status:** Active execution guide
**Scope:** Product, design-system, architecture, and incremental MVP delivery
**Workspace:** Repository root
**Primary specification:** `specs/001-notimate-android-mvp/`
**Created:** 2026-08-21

## 1. Purpose

This file is the operational task prompt for developing NotiMate. It translates the
approved product, design, privacy, and architecture material into a gated,
spec-driven workflow for a coding agent working with the user.

This document does not authorize repository changes by itself. Before editing code,
documentation, Git state, or external systems, the agent must follow the approval
rules in this file and the repository's `AGENTS.md`.

The agent must:

- guide the user through consequential product and architecture decisions;
- work in small, coherent, reviewable slices;
- keep product documents, specifications, implementation, and tests aligned;
- preserve local-first privacy boundaries;
- verify assumptions against the current repository instead of relying on this
  document as a frozen snapshot;
- stop at the initial Git-plan, merge, scope-expansion, and destructive-action
  approval gates and wait for an explicit user response.

## 2. Agent Role And Skills

Act as a senior product-minded Kotlin Multiplatform engineer. Prefer simple,
idiomatic, maintainable architecture and production-ready Android/KMP conventions.

Use the custom agent `$google-kmp-senior-developer` and follow its loaded
instructions.

Use only the additional skill or skills required by the current slice:

- `kotlin-project-feature-implementation` for feature delivery;
- `kotlin-ui-compose-multiplatform` for shared Compose UI;
- `kotlin-ui-adaptive-resources` for adaptive layout and resources;
- `kotlin-project-state-management` for state-holder decisions;
- `kotlin-navigation-compose-multiplatform` for navigation and back stacks;
- `kotlin-testing-kmp` for shared and platform test strategy;
- `kotlin-data-kmp-data-layer` for repositories and persistence;
- `kotlin-platform-kmp-bridges` for genuine platform integrations;
- `kotlin-project-bugfix` for diagnosis and minimal bug fixes;
- `kotlin-project-code-review` for post-implementation review.

Do not activate skills merely to produce more process. When a skill is used, follow
its complete instructions and say why it applies.

## 3. Source-Of-Truth Hierarchy

When sources conflict, apply this precedence order:

1. Current system and user instructions.
2. Repository `AGENTS.md` and the NotiMate constitution.
3. Canonical repository product-design documents.
4. Current MVP `spec.md`, `plan.md`, and `tasks.md`.
5. Current implementation, tests, and Gradle configuration.
6. Canonical Google Drive mirror and approved visual research.
7. Historical Google Drive research.
8. External references and remembered guidance.

Never silently reconcile a material conflict. State the conflict, identify the
controlling source, and update affected specifications when the user approves a new
decision.

### 3.1 Mandatory Repository Reading Order

Read these files before planning a new product slice:

```text
AGENTS.md
.specify/memory/constitution.md
```

Read the product and design foundation in order:

```text
docs/product-design/README.md
docs/product-design/00_PRODUCT_CONTEXT.md
docs/product-design/01_MVP_SCREEN_MAP.md
docs/product-design/02_CORE_USER_FLOWS.md
docs/product-design/03_UI_STATES_AND_CONTENT.md
docs/product-design/04_DOMAIN_MODEL_AND_DECISIONS.md
docs/product-design/05_DESIGN_BRIEF.md
docs/product-design/06_REFERENCE_SOURCES.md
```

Read the canonical MVP delivery documents:

```text
specs/001-notimate-android-mvp/spec.md
specs/001-notimate-android-mvp/plan.md
specs/001-notimate-android-mvp/tasks.md
```

Inspect the relevant current source files and build configuration rather than
assuming the scaffold is unchanged.

### 3.2 Google Drive Map

Use the Google Drive connector read-only for discovery unless the user explicitly
authorizes a Drive write.

The private local source map in `tasks/private/drive-sources.local.md` may contain
the current Drive folder URLs. It is intentionally excluded from Git. Resolve the
folders by these stable names:

- NotiMate canonical design system;
- NotiMate reference screens.

Reference categories:

- Android Platform;
- Profiles and Automations;
- Proposals and Calendar;
- Privacy and Trust;
- Mood Only.

At this document's creation, those category folders contained organizational
README files but no actual reference screenshots. Recheck before claiming visual
references have been collected or approved.

Repository documents are canonical after promotion to Git. Drive remains the
collaboration and visual-assets workspace. Do not edit both copies independently.

### 3.3 Historical Drive Material

The parent Drive folder includes older documents such as:

- `PRODUCT_REQUIREMENTS.md`;
- `UI_UX_SPEC.md`;
- `CORE_SERVICES_SPEC.md`;
- `EDGE_AI_SPEC.md`;
- `ACTION_HANDLERS_SPEC.md`;
- `תוכנית למימוש`;
- `יצירת מסמך קונטקסט ל-Notification Manager`.

Use them for research context only. They do not override the repository. In
particular, do not reintroduce these superseded ideas without a new explicit product
decision:

- mandatory Firebase login before core local use;
- chatbot-first home navigation;
- UI in a nonexistent `composeApp` module;
- direct model-to-side-effect execution;
- silent Calendar Provider writes;
- cloud or FCM analysis of notification content;
- durable WorkManager queues containing raw notification text;
- claims of an indestructible notification listener;
- financial extraction in the calendar-focused first MVP;
- a fixed LiteRT/Gemma runtime before current feasibility is verified.

## 4. Product Map

### 4.1 Product Promise

NotiMate finds useful actions in Android notifications, explains each proposal, and
keeps the user in control without uploading raw notification content.

The product should feel like a quiet, reliable assistant:

- background work is calm and conservative;
- interruptions occur only when potentially useful;
- uncertainty is explained rather than hidden;
- external writes require user review in the MVP;
- unsupported or degraded capabilities leave useful fallbacks available.

### 4.2 Primary Navigation

```text
Today | Automations | Activity
```

Settings is nested from the app bar. Profile management belongs under Automations,
not Account.

### 4.3 Hero MVP Journey

```text
Notification Access education
-> Android settings
-> access verification
-> monitored applications
-> local classification and extraction
-> strict validation
-> calendar proposal
-> Proposal Detail
-> user review and editing
-> Android Calendar ACTION_INSERT handoff
```

Do not claim the event was created when only the Calendar editor was opened.

### 4.4 Core Domain Concepts

- `AutomationProfile`;
- `Automation`;
- `Source`;
- `Condition`;
- `Action`;
- `AlwaysReview` confirmation policy;
- `Proposal`;
- `ActivityRecord`;
- optional `Account` for supported configuration backup only.

### 4.5 Proposal Lifecycle

- `Draft`;
- `NeedsReview`;
- `Ready`;
- `Dismissed`;
- `HandedOff`;
- `Deferred`;
- `Failed`;
- `Expired`.

## 5. Architecture Map

### 5.1 Module Ownership

`:shared` owns:

- shared Compose UI;
- platform-neutral domain models;
- extraction and validation contracts;
- policy and privacy rules;
- shared state contracts;
- platform-neutral tests.

`:androidApp` owns:

- Android application shell;
- `NotificationListenerService`;
- Android permission and settings handoffs;
- proposal notifications;
- WorkManager orchestration;
- Calendar `ACTION_INSERT` integration.

`iosApp/` owns:

- the iOS application host;
- shared Compose hosting;
- no promise of Android-equivalent cross-application notification interception.

### 5.2 Processing Pipeline

```text
observe
-> minimize at the Android boundary
-> apply monitored-source and active-Profile policy
-> classify locally
-> extract locally
-> validate against a strict schema
-> create ignored/review/proposal/deferred result
-> require confirmation
-> hand off to the platform action
```

### 5.3 State And Data Ownership

- Composables render immutable state and emit actions.
- State holders coordinate; they do not become repositories or workflow engines.
- Repositories own durable truth.
- Platform entry points delegate quickly.
- Domain APIs must not expose DTOs or persistence entities.
- Prefer constructor injection.
- Prefer an interface plus injected platform implementation over premature
  `expect`/`actual`.

### 5.4 Non-Negotiable Privacy Boundary

Raw notification content must never be:

- uploaded;
- logged;
- included in analytics or crash reports;
- persisted as ordinary durable history;
- committed in fixtures, previews, screenshots, or tests;
- supplied to a cloud model;
- carried into deferred work as an unstructured durable payload.

API keys, tokens, passwords, signing material, private keys, service-account
credentials, and other sensitive values must never enter source, tests,
documentation, screenshots, logs, commits, or pull-request text. Before every
commit and pull-request update, scan the intended diff and changed filenames.
Report suspected matches by path, line when safe, and redacted type only.

Use synthetic, privacy-safe content for development artifacts.

## 6. Design Direction

Until replaced by an approved visual-direction document, propose this synthesis:

- Calm Material as the overall foundation;
- Operational Control clarity for the `When / Then / Confirm` builder;
- warmer explanatory treatment only for onboarding, privacy, ambiguity, and
  optional AI-assistance surfaces.

Desired characteristics:

- calm productivity rather than futuristic AI spectacle;
- warm neutral surfaces;
- restrained blue or blue-teal primary color;
- moderate rounding;
- strong information hierarchy;
- clear semantic states that do not rely on color alone;
- minimal decorative gradients and glow;
- Material 3 familiarity without looking like Android Settings;
- light and dark themes;
- Hebrew and RTL support;
- dynamic text and accessible semantics;
- adaptive layouts;
- synthetic preview data only.

Do not freeze final color, type, motion, or shape tokens before the visual direction
and reference board are approved.

## 7. Workstream Index

This parent file tracks sequence and stable context only. Create one focused file
under `tasks/agent-tasks/` from `TEMPLATE.md` before planning or implementing a
workstream. The subtask owns its detailed scope, decisions, files, tests, and
validation.

| ID | Workstream | Subtask | Depends on |
|---|---|---|---|
| 001 | Baseline | [Open](agent-tasks/001-baseline.md) | — |
| 002 | Visual direction | [Open](agent-tasks/002-visual-direction.md) | 001 |
| 003 | Design-system specification | [Open](agent-tasks/003-design-system-specification.md) | 002 |
| 004 | Runtime Compose design system | [Open](agent-tasks/004-runtime-compose-design-system.md) | 003 |
| 005 | Shared domain foundation | [Open](agent-tasks/005-shared-domain-foundation.md) | 001 |
| 006 | Notification capture | [Open](agent-tasks/006-notification-capture.md) | 005 |
| 007 | Persistence and settings | [Open](agent-tasks/007-persistence-and-settings.md) | 005 |
| 008 | Extraction pipeline | [Open](agent-tasks/008-extraction-pipeline.md) | 006, 007 |
| 009 | Permission onboarding and Today | [Open](agent-tasks/009-permission-onboarding-and-today.md) | 004, 006, 007 |
| 010 | Proposal review and Calendar | [Open](agent-tasks/010-proposal-review-and-calendar.md) | 008, 009 |
| 011 | Profiles and Automations | [Open](agent-tasks/011-profiles-and-automations.md) | 004, 007 |
| 012 | Activity and local controls | [Open](agent-tasks/012-activity-and-local-controls.md) | 007, 010 |
| 013 | Optional account and sync | [Open](agent-tasks/013-optional-account-and-sync.md) | 012 |

Use `specs/001-notimate-android-mvp/tasks.md` as the canonical implementation
backlog. This index groups that backlog into context-sized execution units; it does
not replace or duplicate it.

### 7.1 Selecting The Next Subtask

1. Prefer the earliest incomplete workstream whose dependencies are satisfied.
2. Create `tasks/agent-tasks/<id>-<short-kebab-name>.md` from the template.
3. Load only the parent file, named sources, and files relevant to that subtask.
4. Keep one primary outcome; split the file if it crosses unrelated modules or can
   no longer be reviewed as one coherent change.
5. Update status in the subtask rather than expanding this parent file.

## 8. Required Planning Output

Before any implementation edit, provide:

1. current-state and pre-coding inspection summary;
2. source-of-truth assessment;
3. conflicts, missing inputs, and assumptions;
4. the smallest coherent milestone;
5. files to inspect, modify, and create;
6. module and source-set placement;
7. state pipeline and data ownership, when applicable;
8. risks and privacy implications;
9. tests and validation commands;
10. a short Git plan;
11. a direct request for approval.

Do not edit anything in that response. Wait for an explicit user approval.

After approval and before editing, show:

- a compact branch/merge diagram;
- the exact intended command list;
- the files expected to change;
- which actions remain outside the approval.

## 9. Git Flow Thinking

### 9.1 Principles

- Git operations are deliberate product actions, not incidental implementation
  details.
- Follow the repository `AGENTS.md` for authorization, approval, and destructive
  action rules; do not duplicate those policies in this task file.
- Inspect before mutating.
- Preserve unrelated user changes.
- Keep each feature or bug isolated and reviewable.
- Treat approval of the explicit Git plan as standing authorization for its
  task-scoped branch, edits, staging, commits, non-protected pushes, pull-request
  creation or updates, review, and validated review fixes.
- Never treat task approval as authorization to merge, change `dev` or `main`
  directly, approve an agent-authored PR, bypass push protection, rewrite
  published history, or delete branches or worktrees.
- If the diff, branch target, base branch, or risk changes materially, request new
  approval.

### 9.2 Branch Decision Process

Before proposing a branch:

1. Inspect the current branch, status, log, and available base branches.
2. Decide whether the work is a feature, bug, documentation-only continuation, or
   an unrelated change.
3. Prefer the documented flow:

   ```text
   main
   └── dev
       ├── feature/<short-kebab-name>
       └── bug/<short-kebab-name>
   ```

4. Continue on an existing feature branch only when the work clearly belongs to
   that branch and the user approves doing so.
5. Do not mix unrelated work to avoid creating a branch.
6. Explain the proposed base and destination before requesting branch creation.

The Git plan must name the exact branch creation or switch. Approval of that plan
authorizes the named task branch action but no other branch mutation.

### 9.3 Commit Thinking

Before committing under an approved Git plan:

1. Show the relevant status and diff summary.
2. Identify every file intended for the commit.
3. Exclude unrelated changes.
4. Propose the exact Conventional Commit title and required body, following the
   Google KMP Senior Developer guidance and repository `AGENTS.md`.
5. Inspect the intended diff and changed filenames for secrets and sensitive user
   data without printing matched values.
6. Stage only task-scoped files and commit only after validation passes.

Prefer additive commits over amending published work. If the change materially
exceeds the approved plan, stop and request expanded authorization.

### 9.4 Push And Pull-Request Thinking

Before pushing under an approved Git plan:

1. Show the current branch and commits that would be pushed.
2. Name the exact remote and remote branch.
3. State whether the push creates or updates the remote branch.
4. Confirm that no force option will be used.
5. Confirm the intended commits passed the secret and sensitive-data gate.
6. Never force push or bypass push protection.

Before opening or updating a pull request under an approved Git plan:

1. Name the head and base branches.
2. Provide the exact proposed Conventional Commit-style PR title and full required
   body, following repository `AGENTS.md`; include the problem, resulting behavior,
   rationale, linked task/spec, and actual validation evidence.
3. Report validation results and known limitations.
4. State whether the PR will be draft or ready for review.
5. Run the independent PR reviewer and address validated findings with additive
   commits when they remain within scope.

### 9.5 Merge Thinking

Before requesting permission to merge:

1. Confirm the exact PR or source and destination branches.
2. Report review and CI status.
3. Explain the merge method: merge commit, squash, or rebase merge.
4. Explain any effect on commit history.
5. Wait for explicit merge approval.

Opening or approving a PR does not authorize merging it. After a merge, do not
delete local or remote branches without separate explicit permission.

### 9.6 Rebase And History-Rewrite Thinking

Rebase of published or shared work is never routine or implicitly authorized.
Before requesting one:

1. Explain why it is needed and identify a safer alternative when available.
2. Name the exact branch being rebased and the exact new base.
3. Show whether commits are already published.
4. Explain conflicts, rewritten commit identifiers, and push implications.
5. State whether a later force push would be required.
6. Wait for explicit approval that specifically says rebase is allowed.

Approval to rebase does not authorize force-pushing the rewritten branch.

A clean, unpublished, agent-owned branch may be rebased onto `dev` when the
approved Git plan requires it and no user work can be overwritten.

The same explicit-permission rule applies to:

- `git commit --amend`;
- interactive rebase;
- `git reset` that changes files, index, or branch position;
- `git cherry-pick`;
- reverting shared history;
- deleting branches or tags;
- force pushes, including `--force-with-lease`;
- replacing or overwriting remote history.

## 11. Dangerous And Destructive Actions

Never infer authorization for dangerous actions. Resolve exact targets with
read-only checks first.

Dangerous actions include, but are not limited to:

- rebasing or rewriting commits;
- force pushing;
- resetting branches or files;
- deleting files, branches, tags, releases, Drive files, or cloud resources;
- overwriting user changes;
- rotating or modifying credentials;
- modifying production data or configuration;
- publishing packages, releases, PRs, or messages;
- broad recursive filesystem operations;
- any operation whose recovery path is unclear.

Before requesting permission:

1. identify exact targets;
2. explain why the action is necessary;
3. explain safer alternatives;
4. explain whether and how recovery is possible;
5. show the exact command or API operation;
6. request explicit approval;
7. execute only the approved action;
8. report the result and remaining state.

Never use destructive commands against broad paths, unresolved variables, the
workspace root, the home directory, or `/`.

## 12. Implementation Rules

- Implement the smallest coherent approved slice.
- Keep shared code platform-neutral.
- Keep Android APIs at Android edges.
- Keep business logic out of composables and platform entry points.
- Expose immutable UI state and accept explicit actions.
- Separate persistent state from one-time effects.
- Use resource-backed user-facing strings.
- Keep preview/sample data separate and synthetic.
- Handle loading, empty, error, denied, revoked, paused, degraded, and retry states
  when reachable.
- Do not add speculative abstractions or pass-through use cases.
- Add focused tests alongside business rules and boundary logic.
- Preserve coroutine cancellation and structured concurrency.
- Verify dependency versions and unstable APIs with primary documentation when a
  current decision depends on them.

## 13. Validation And Completion

Select validation proportionate to the slice. Normal project commands include:

```text
./gradlew :shared:allTests
./gradlew :shared:check
./gradlew :androidApp:assembleDebug
```

Before claiming completion:

- [ ] inspect the final diff;
- [ ] run `git diff --check`;
- [ ] report validation commands and results;
- [ ] confirm source-set placement;
- [ ] confirm privacy constraints;
- [ ] confirm the intended diff and changed filenames pass the secret and
  sensitive-data gate;
- [ ] confirm specs and tasks remain aligned;
- [ ] identify remaining risks and untested platform behavior;
- [ ] keep delivery within the approved Git plan and leave merge authorization to
  the user.

## 14. Required Handoff Format

Conclude an implemented slice with:

1. outcome first;
2. changed files with absolute paths;
3. important design and architecture decisions;
4. validation performed and results;
5. privacy and platform-boundary checks;
6. remaining risks or next recommended slice;
7. current Git status;
8. Git actions performed under the approved plan, and confirmation that no merge,
   protected-branch write, force push, or unapproved history rewrite occurred.

## 15. Initial Resume Instruction

When a new task starts from this file, the agent's first response must:

1. verify the current workspace and Git state;
2. read the mandatory sources relevant to the requested slice;
3. summarize current product, design, and architecture state;
4. identify conflicts, missing inputs, and assumptions;
5. recommend one immediate milestone;
6. present a short Git plan;
7. ask for explicit approval;
8. make no edits until approval is received.
