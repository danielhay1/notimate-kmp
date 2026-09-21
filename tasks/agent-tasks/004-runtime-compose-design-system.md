# 004 — Runtime Compose Design System

**Status:** Proposed
**Depends on:** 003
**Expected branch:** Set in the approved Git plan before work

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. Branch, commit, push, PR, and merge actions require explicit user
permission.

## Outcome

Implement the approved shared Compose theme, tokens, and core reusable components.

## Scope

- Place platform-neutral UI in `shared/src/commonMain`.
- Add light and dark `NotiMateTheme` schemes and resource-backed strings.
- Implement only components required by the approved inventory.
- Keep components stateless, previewable, adaptive, and RTL-safe.

## Guardrails

- Do not add unrelated navigation, persistence, or dependency injection.
- Use synthetic preview data and existing Google/Kotlin conventions.

## Done When

- [ ] Approved tokens and core components are implemented.
- [ ] Source-set and accessibility requirements are satisfied.
- [ ] Focused tests cover non-UI logic only where useful.

## Validation

- `./gradlew :shared:allTests :shared:check :androidApp:assembleDebug`
