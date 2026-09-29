---
name: notimate-kmp-pr-delivery
description: Deliver an approved NotiMate task branch through validation, commit, push, pull-request creation or update, independent clean-context review, and review-fix cycles; never approve or merge the pull request.
---

# NotiMate KMP PR Delivery

Coordinate delivery only after the user approves a Git plan under repository
`AGENTS.md`. The approval covers the planned task branch, edits, commits, pushes,
pull-request delivery, independent review, and validated review fixes. It does not
authorize scope expansion, destructive actions, history rewrites, force pushes,
push-protection bypasses, branch deletion, GitHub approval, or merge.

Use `$notimate-kmp-git-writting-conventions` for commit and pull-request copy and
`$notimate-kmp-pr-reviewer` for every review pass. The delivery agent owns fixes;
the reviewer remains read-only.

## Prepare the change

1. Confirm the approved base and task branch. Never work directly on `dev` or
   `main`.
2. Keep unrelated working-tree changes out of the intended diff.
3. Run the focused validation required by the change.
4. Before every commit and pull-request update, inspect the complete intended diff
   and changed filenames for secrets, credentials, sensitive notification content,
   and personal data. Run the available secret scanner with redacted output.
5. Stop before publication if a likely secret is present. If it was already pushed,
   stop further publication and ask the user to revoke or rotate it. Do not rewrite
   published history without new approval.
6. Present the exact commit or pull-request title and body before delivery. Report
   only validation actually observed.

## Start an independent review

After pushing or updating the pull request, record its number or URL, declared base,
base SHA, head SHA, and relevant task or specification paths. Spawn a new subagent
with clean context for each review pass. Use no inherited conversation context when
the collaboration interface supports that option.

Give the reviewer only the review target and the information needed to locate its
authorities. Require it to load repository `AGENTS.md` and
`$notimate-kmp-pr-reviewer` itself. Do not include the implementation agent's
reasoning, suspected defects, or desired verdict.

The reviewer must first inspect the complete pull-request diff independently. Only
after that inspection may it reconcile earlier findings, developer replies, and
false-positive rationales. This preserves independence without losing the review
audit trail.

## Resolve findings

Every blocker, should-fix finding, and nit prevents readiness.

- For a valid finding, implement the smallest safe correction, validate it, repeat
  the pre-commit secret gate, commit, push, and start a new clean review pass.
- For a suspected false positive, post a concise rationale grounded in code,
  specification, or observed validation. The developer cannot clear its own
  finding; a new reviewer must accept or reject the rationale.
- A real accepted risk or deferred issue remains a finding and blocks readiness
  unless the user explicitly changes the approved policy.
- A new commit, changed base SHA, or changed head SHA invalidates the previous
  verdict. Review the complete current diff again rather than only the latest fix.

When GitHub access permits, publish each validated finding as an inline comment and
maintain one summary comment identified as an independent agent review. Include the
reviewed head SHA. If comment publication is unavailable, return the exact comment
text and verified diff coordinates instead of claiming it was posted.

## Completion gate

The pull request is ready for the user's review only when all of these are true:

- the exact current head SHA received `APPROVE` from a fresh reviewer;
- blockers, should-fix findings, and nits are all empty;
- rejected concerns include evidence for every accepted false positive;
- required deterministic GitHub checks are green for that head SHA;
- review conversations are resolved;
- the branch and pull-request metadata still match the approved scope.

Report the pull-request URL, reviewed SHA, validation evidence, rejected concerns,
and residual limitations. Never submit a GitHub approval or merge the pull request.
The user alone reviews and authorizes every merge.
