---
name: notimate-kmp-comments-and-kdoc
description: Write, preserve, and review Kotlin comments and KDoc for NotiMate implementation work. Use when code changes add or affect comments, public API documentation, TODOs, or non-obvious contracts; do not add chat-session or agent-process commentary to source files.
---

# NotiMate KMP Comments and KDoc

Write comments that explain durable implementation intent, constraints, contracts,
or non-obvious tradeoffs. Prefer clear code and names when they can remove the need
for a comment. Keep comments as short as correctness permits: normally one clear
sentence, with additional sentences only when they preserve an essential contract.

## Comment threshold

Add a comment only when all of these are true:

- the information is not already clear from names, types, control flow, tests, or
  the surrounding API;
- it will remain useful after the current implementation session;
- omitting it could cause a future maintainer to misunderstand a constraint,
  contract, risk, or intentional tradeoff.

If any condition is false, do not add the comment. Prefer clearer naming, smaller
functions, stronger types, or simpler control flow when those changes can make the
code self-explanatory. Avoid decorative headings, commented-out code, line-by-line
narration, and comments that merely translate Kotlin into prose.

## Apply comments with the implementation

When changing code:

1. Inspect comments and KDoc adjacent to the affected behavior.
2. Preserve accurate comments even when rewriting nearby code.
3. Update comments that the implementation makes inaccurate or incomplete.
4. Remove a comment when the affected implementation makes it obsolete,
   misleading, redundant, or unnecessary, while keeping cleanup within scope.
5. Add a comment only when future readers need information the code cannot express
   clearly by itself.

Never add comments about the chat, current agent session, prompts, review
conversation, temporary plan, or how the agent produced the code. Preserve accurate
implementation comments regardless of who created them. If existing session/process
commentary is discovered, remove it only when that cleanup is within the approved
scope; otherwise report it.

## What comments should explain

Use comments for information such as:

- why a privacy, security, lifecycle, or platform constraint exists;
- a non-obvious invariant or ordering requirement;
- cancellation, retry, idempotency, migration, or process-death behavior;
- a platform/API workaround and the condition that permits its removal;
- an intentional tradeoff that might otherwise look like a bug;
- externally imposed schema or compatibility behavior.

Explain why the implementation needs a non-obvious decision, not what each line
does. Do not restate names or signatures, preserve dead code, record authorship, or
leave change-history prose that belongs in Git.

## KDoc

Provide KDoc for public types and their public or protected members, except
genuinely self-explanatory declarations and overrides that add no contract.
Prioritize shared module boundaries, platform abstractions, repositories, domain
policies, reusable Compose components, and public models with important invariants.
Do not add empty or repetitive KDoc merely to satisfy coverage.

KDoc is required when an API exposes information callers cannot safely infer from
its signature, including non-obvious invariants, side effects, ownership, privacy,
threading or cancellation behavior, failure semantics, lifecycle constraints, or
caller responsibilities. It is also required for intended public KMP boundaries
and `expect` contracts. Document an `actual` declaration separately only when its
platform behavior materially differs from the common contract.

KDoc is not required for private implementation details, local declarations,
obvious properties, or overrides that preserve the documented parent contract. If
a declaration is unintentionally public, reduce its visibility instead of writing
documentation merely to justify the exposure.

- Start with a brief, capitalized summary fragment. Avoid boilerplate openings such
  as "This class" or "This method."
- Prefer a one-line KDoc when the complete contract fits clearly on one line. For
  multiline KDoc, use standard `/** ... */` formatting and one blank `*` line
  between paragraphs.
- Wrap comments and KDoc at 100 columns except when an unbreakable URL or a
  copyable shell command requires a longer line.
- Add detail only for non-obvious behavior; do not restate the declaration.
- Describe behavior, invariants, ownership, side effects, threading, cancellation,
  failure semantics, privacy constraints, and caller responsibilities as relevant.
- Use `[symbol]` links for Kotlin declarations.
- Prefer prose that naturally mentions parameters and return values. Use `@param`,
  `@return`, or `@throws` only when longer clarification adds value.
- When block tags are needed, use only applicable tags, never leave descriptions
  empty, and order them as `@constructor`, `@receiver`, `@param`, `@property`,
  `@return`, `@throws`, then `@see`.
- Use `@Deprecated` rather than an unsupported KDoc `@deprecated` tag.
- Keep documentation synchronized with the implementation.

When a copyright or license header is required, use a regular multiline comment,
not KDoc or consecutive single-line comments.

For reusable composables, document non-obvious state ownership and event contracts;
do not document ordinary layout mechanics. For `expect` and `actual` APIs, document
the common contract at `expect` and add platform documentation only for materially
different behavior or constraints.

## Task markers

Neither `TODO` nor `FIXME` is required. Do not add either by default.

- Use `TODO` only for approved deferred work with a concrete outcome and a stable
  task or issue reference.
- Treat `FIXME` as a marker for known incorrect behavior. Prefer fixing the defect
  now or reporting it as a blocker. Add `FIXME` only when the user explicitly
  accepts the defect and there is a tracked correction.

Never use task markers as agent reminders, session notes, vague cleanup wishes, or
substitutes for required implementation.

## Privacy

Never place real notification text, personal data, MFA codes, financial data,
credentials, tokens, internal prompts, or sensitive payload examples in comments
or KDoc. Use synthetic placeholders when an example is necessary.
