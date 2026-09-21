# 011 — Profiles And Automations

**Status:** Proposed
**Depends on:** 004, 007
**Expected branch:** Set in the approved Git plan before work

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Let users manage notification profiles and create structured automations with clear
`When / Then / Confirm` behavior.

## Scope

- Build active-profile selection, profile management, and automation lists.
- Enforce exactly one active profile and prevent deleting the only profile.
- Build a structured automation editor with validation.
- Offer natural-language drafting only when local AI is available.

## Guardrails

- AI-generated automations remain validated, editable drafts.
- Do not allow direct model-to-side-effect execution.

## Done When

- [ ] Profile invariants and automation validation are tested.
- [ ] Empty, invalid, unavailable-AI, and successful edit paths work.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`
