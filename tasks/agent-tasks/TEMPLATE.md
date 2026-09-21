# Subtask: <ID> — <Outcome>

**Status:** Proposed | Approved | In progress | Blocked | Complete
**Parent:** `tasks/agent-task.md`
**Depends on:** <subtask IDs or none>
**Expected branch:** `<branch>`
**Relevant sources:** <only what this subtask needs>

## Start Gate

Before changing files:

1. Read the parent task, relevant sources, and current repository state.
2. Run `git branch --show-current` and `git status --short --branch`.
3. Confirm the checked-out branch is the expected branch for this subtask.
4. If the branch is wrong, missing, or otherwise blocks safe work, stop and tell
   the task invoker the current branch, expected branch, and required next action.

The user-approved Git plan authorizes the task-scoped branch and delivery actions
defined by repository `AGENTS.md`. Never merge, change protected branches
directly, or exceed that plan.

## Outcome

<One observable result that can be implemented and reviewed as a coherent change.>

## Scope

**In:**

- <required work>

**Out:**

- <related work intentionally deferred>

## Context And Constraints

- **Decisions:** <already-approved decisions and their sources>
- **Modules/source sets:** <for example `shared/commonMain` or `androidApp`>
- **Architecture:** <ownership or boundary that matters for this task>
- **Privacy/security:** <data minimization, secrets, logging, persistence, or fixture constraints>
- **Open question:** <blocking decision or `None`>

Follow existing project patterns and Google/Kotlin conventions. Keep shared code
platform-neutral, Android APIs at Android edges, and test data synthetic.

## Work

**Expected files:**

- `<path>` — inspect | modify | create

**Steps:**

1. <small implementation step>
2. <small implementation step>

## Done When

- [ ] <observable acceptance criterion>
- [ ] Relevant focused tests pass.
- [ ] Source-set, architecture, and privacy boundaries remain correct.
- [ ] Specifications and tasks are aligned when behavior changed.
- [ ] The intended diff and changed filenames pass the secret and sensitive-data check.

**Validation:**

- `<command>`
- <manual check, if needed>

## Git And Handoff

When a commit is requested, first inspect the final diff and propose a concise,
senior-quality Conventional Commit title and required body, following the Google
KMP Senior Developer guidance and repository `AGENTS.md`:

```text
<type>(<scope>): <imperative summary under 72 characters>

<Problem, resulting behavior, and rationale.>

Validation: <actual commands and results, or not run with reason>
Task/spec: <relevant reference>
Limitations: <if applicable>
```

Use the narrowest accurate type and a project or KMP scope such as `shared`,
`android`, `ios`, `compose`, `gradle`, `network`, or `database`. Do not mix unrelated
changes. Delivery may proceed under the approved Git plan only after the intended
diff passes security and validation gates. Merge authorization remains user-only.

When a PR is requested, provide its exact title and full body using the same
conventions, based on the complete PR diff. Include synthetic screenshots or
recordings for UI changes as required by repository `AGENTS.md`.

Report the outcome, changed files, validation results, remaining risks or work, and
current Git state to the task invoker.
