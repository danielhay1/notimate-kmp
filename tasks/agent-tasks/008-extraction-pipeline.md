# 008 — Extraction Pipeline

**Status:** Proposed
**Depends on:** 006, 007
**Expected branch:** Set in the approved Git plan before work

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. The approved Git plan authorizes task-scoped delivery under repository
`AGENTS.md`; merges and protected-branch changes remain user-only.

## Outcome

Turn minimized notification input into validated calendar proposals or safe fallback
states using an on-device pipeline.

## Scope

- Define the extractor contract and strict validated output schema.
- Implement a deterministic rule-based calendar extractor first.
- Model ready, unavailable, downloading, unsupported, deferred, and failed states.
- Gate heavy work by memory, battery, and thermal conditions.
- Use WorkManager only when deferred data can remain privacy-safe.

## Guardrails

- Model output is advisory until schema and policy validation pass.
- Never send raw content to cloud services or durable unstructured queues.

## Done When

- [ ] Valid, ambiguous, invalid, and constrained-device paths are tested.
- [ ] Each input resolves to an explicit outcome without unsafe side effects.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`
