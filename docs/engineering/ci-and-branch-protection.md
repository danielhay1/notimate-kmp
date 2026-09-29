# CI And Branch Protection

NotiMate separates reproducible GitHub checks from agent-assisted code review.
GitHub Actions never invokes an OpenAI model and requires no stored repository,
environment, or organization secret.

## Deterministic pull-request gates

The `CI` workflow runs for pull requests targeting `dev` or `main` when they are
opened, reopened, updated with a commit, marked ready for review, or edited. A new
run cancels an obsolete run for the same pull request.

The required checks are:

- `CI / Android MVP`: shared Android-host tests, Android unit tests, Android lint,
  and debug APK assembly;
- `CI / PR policy`: Conventional Commit title, required body evidence, validator
  tests, and whitespace errors in the exact base-to-head diff;
- `CI / Dependency review`: newly introduced moderate-or-higher vulnerabilities;
- `CI / Secret scan`: secrets introduced anywhere in the pull-request commits.

The Android MVP intentionally does not require iOS CI. Validate behavior in
Android Studio or on an Android device when the change requires runtime evidence.

The workflow has read-only repository permission and uses GitHub-hosted runners.
Pull-request code receives no stored secrets. Action references are pinned to
immutable commits, checkout credentials are not persisted, and obsolete runs are
cancelled. The temporary `GITHUB_TOKEN` is read-only; it is not a user-provided
credential or OpenAI API key.

Gitleaks comments, summaries, and report artifacts are disabled so suspected
values are not republished. A detected secret blocks the check. Treat any published
credential as compromised and rotate or revoke it; deleting it from the latest
commit is not sufficient.

## Agent review cycle

GitHub Actions does not perform semantic code review. After an approved task branch
is pushed, the delivery agent uses `$notimate-kmp-pr-delivery` to start a fresh,
read-only subagent that loads `$notimate-kmp-pr-reviewer`.

Each review is bound to an exact base and head SHA. The reviewer first inspects the
complete diff independently, then reconciles earlier findings and author replies.
Every blocker, should-fix finding, and nit prevents readiness. A developer may fix
a finding or provide evidence that it is a false positive, but only a new reviewer
may clear it. Every pushed correction starts a new complete review.

The review is advisory rather than a GitHub status check. Repository instructions
and the user enforce it. If GitHub comment access is available, the delivery agent
publishes inline findings and maintains one SHA-bound summary. Otherwise it returns
the exact comments for manual publication. Agents never submit GitHub approvals or
merge pull requests.

## Required repository rules

After all four checks have completed in a hosted smoke-test pull request, configure
rulesets for `dev` and `main` to:

- require a pull request before merge;
- require all four deterministic checks;
- require the branch to be up to date;
- require review conversations to be resolved;
- block force pushes and branch deletion;
- disallow automation bypass; and
- remove the obsolete `Codex PR review` status if it is configured.

Enable the GitHub dependency graph, Dependabot alerts, secret scanning, and push
protection. These hosted settings cannot be enforced by repository YAML and must
be verified in GitHub.

If the pull request is authored through the user's own GitHub identity, GitHub does
not allow that same identity to submit a formal approval. In that setup, the user's
review and merge authorization are procedural. A separate author identity is
required before configuring a mechanically required user approval.

## Activation

1. Merge the workflow to `dev` through a user-reviewed pull request.
2. Open a harmless smoke-test pull request and add a second commit.
3. Verify that creation and synchronization run all four checks on the latest SHA.
4. Edit the title or body and verify that `CI / PR policy` reruns.
5. Confirm no workflow requests a stored secret or has write permission.
6. Add the observed check names to the `dev` ruleset, then repeat when promoting
   the workflow to `main`.
