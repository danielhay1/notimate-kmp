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
Manual dispatch becomes available when the workflow exists on GitHub's default
branch. Concurrency controls cancel an obsolete run when a newer commit is pushed
to the same pull request.

## Required CI

The `CI / KMP and Android verification` check runs shared Android-host tests,
Android unit tests, Android lint, and a debug APK build. Failure reports are kept
as short-lived workflow artifacts.

## Automated PR review

The `Codex PR Review / Publish review verdict` check invokes the reusable
`$notimate-kmp-pr-reviewer` skill through the versioned prompt in
`.github/codex/prompts/pr-review.md`. The model job has read-only repository
permission, a read-only sandbox, no saved Git credentials, and no permission to
post or change pull requests. A separate job with narrowly scoped GitHub
permissions converts the structured findings into native inline review comments
and updates one marked summary comment.

Every inline comment is attached to a real pull-request diff line and labeled as
`🔴 Blocker`, `🟡 Should fix`, or `🟢 Nit`. The workflow submits comments only; it
never sends a GitHub approval or request-changes review on the user's behalf.

`APPROVE` and `APPROVE WITH FOLLOW-UPS` pass the review check. `REQUEST CHANGES`,
a missing response, or an action failure fails it. The verdict remains advisory;
only the user can authorize a merge.

Add an OpenAI API key in **GitHub repository settings → Secrets and variables →
Actions** with the name `OPENAI_API_KEY`. Never put the value in repository files,
workflow text, logs, comments, screenshots, or documentation.

The workflow uses the `pull_request` event rather than `pull_request_target` so
untrusted pull-request code does not receive base-repository secrets. GitHub does
not expose Actions secrets to pull requests from forks, so those reviews fail
closed and require a trusted maintainer or manual review.

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
