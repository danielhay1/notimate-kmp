# 003 — Design-System Specification

**Status:** Complete
**Depends on:** 002
**Expected branch:** `feature/product-design-foundation`

Before editing, verify the checked-out branch. If blocked by a mismatch, tell the
task invoker. Branch changes, commits, pushes, PRs, and merges require explicit user
permission.

## Outcome

Create an implementation-ready design-system specification and component inventory.

## Sources

- `docs/product-design/01_MVP_SCREEN_MAP.md`
- `docs/product-design/03_UI_STATES_AND_CONTENT.md`
- `docs/product-design/05_DESIGN_BRIEF.md`
- `docs/product-design/07_VISUAL_DIRECTION.md`

## Scope

- Define semantic color, type, spacing, shape, elevation, and motion roles.
- Map screens and states to a small reusable component set.
- Specify accessibility, localization, RTL, adaptive, and preview requirements.

## Done When

- [x] `08_DESIGN_SYSTEM.md` and `09_COMPONENT_INVENTORY.md` are complete.
- [x] Every MVP screen can be composed from the documented roles and components.
- [x] Runtime implementation remains separately gated by task 004.

## Validation

- Cross-check screen, state, component, and token coverage.

## Result

- Added a compact Material 3-based token, semantic state, adaptive, resource, and
  accessibility contract in `08_DESIGN_SYSTEM.md`.
- Added a screen-complete product component inventory and initial runtime slice in
  `09_COMPONENT_INVENTORY.md`.
- Task 004 retains the separate approval gate for runtime implementation.
