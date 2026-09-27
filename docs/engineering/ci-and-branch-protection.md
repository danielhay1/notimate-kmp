# CI And Branch Protection

GitHub Actions provides deterministic validation and an independent Codex review
for pull requests targeting `dev` or `main`.

## Pull-request events

Both workflows run when a pull request is:

- opened;
- reopened;
- updated with new commits (`synchronize`);
- changed from draft to ready for review (`ready_for_review`).

The deterministic CI workflow also runs after a push reaches `dev` or `main`.
Base-only updates do not automatically rerun PR reviews. Require branches to be
up to date and update the PR branch through a PR (or rerun review as appropriate)
when its base changes. Each review run resets the head status to pending before
requesting access to the review environment.
Manual dispatch becomes available when the workflow exists on GitHub's default
branch. Concurrency controls cancel an obsolete run when a newer commit is pushed
to the same pull request.

## Required CI

The `CI / KMP and Android verification` check runs shared Android-host tests,
Android unit tests, Android lint, and a debug APK build. Failure reports are kept
as short-lived workflow artifacts.

The Android MVP intentionally does not require iOS CI. Validate Android behavior
in Android Studio or on a device in addition to automated checks when applicable.

## Automated PR review

The `Codex PR review` commit status invokes the reusable
`$notimate-kmp-pr-reviewer` skill through the versioned prompt in
`.github/codex/prompts/pr-review.md`. The model job has read-only repository
permission, a read-only sandbox, no saved Git credentials, and no permission to
post or change pull requests. A separate job with narrowly scoped GitHub
permissions converts the structured findings into native inline review comments
and updates one marked summary comment. The reviewer uses a pinned Codex CLI and
proxy version (`0.157.1`); upgrade both through a validated maintenance PR.

The workflow uses `pull_request_target` and checks out only the protected base
commit. GitHub PR patches are serialized as data under `.review-input/`; no PR
code, workflow, prompt, or configuration is checked out or executed. Findings
use patch coordinates and are published against the event's head SHA. Changed
head/base commits invalidate a review. Missing patches or incomplete API results
fail closed for manual review. CI continues testing the PR merge result.

Every inline comment is attached to a real pull-request diff line and labeled as
`🔴 Blocker`, `🟡 Should fix`, or `🟢 Nit`. The workflow submits comments only; it
never sends a GitHub approval or request-changes review on the user's behalf.

`APPROVE` and `APPROVE WITH FOLLOW-UPS` pass the review check. `REQUEST CHANGES`,
a missing response, or an action failure fails it. The verdict remains advisory;
only the user can authorize a merge.

### Required owner setup before activation

1. Create a GitHub environment named `codex-review`. Set deployment access to
   **Selected branches and tags**, with branch rules for exactly `dev` and `main`.
   Do not allow wildcard branches, PR merge refs, tags, or all protected branches.
2. Store `OPENAI_API_KEY` only as a secret in that environment. Use a dedicated
   OpenAI project/service account with limited API permissions and usage alerts.
   Remove any repository-level or repository-accessible organization-level copy
   of this secret; otherwise another branch workflow can still use it. Never
   send the key in chat or put it in tracked files.
3. Confirm environment branch restrictions are supported and enforced by your
   GitHub plan. If unavailable, do not enable this API-key workflow; use an
   externally hosted reviewer instead. An environment name alone provides no
   protection. If required reviewers are enabled, each run also needs approval.
4. Merge this workflow and its trusted skills/prompt/schema into `dev`, then
   `main` through user-reviewed PRs. The trusted workflow must exist on the
   default branch to activate it. If an Actions event policy blocks
   `pull_request_target`, the owner must explicitly allow this reviewed workflow.
5. Open a smoke-test PR and add a commit. Verify both events run Android CI and
   review, that inline comments point to the PR head, and that the `Codex PR
   review` status appears on that exact SHA. Validate denied environment access
   from a feature-branch workflow with a harmless canary, never the API key.
6. Only after that smoke test, require `Codex PR review` alongside Android CI.
   Keep user review mandatory. The target-event job itself reports on the base;
   the explicit commit status is the gate for the PR head.

The action's default author-access check rejects untrusted actors; fork reviews
are not automatically enabled. A failed automated review requires owner handling,
never a silently successful status. Review data and outputs are not uploaded as
artifacts. API-key authentication is used because the pinned action supports it
directly; OIDC would require separately verified runtime authentication support.

## Required GitHub rules

Create rulesets for `dev` and `main` that:

- require pull requests before merge;
- require the CI and Codex review checks to pass;
- require the user's review;
- dismiss stale approvals when new commits are pushed;
- require the branch to be up to date before merge;
- block force pushes and branch deletion;
- do not allow bypass for agents or automation identities.

Enable GitHub secret scanning and push protection when available. These hosted
settings cannot be enforced by committed workflow files and must be verified in
the repository settings.
