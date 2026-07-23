# NotiMate Product and Design Foundation

Status: Canonical product-design input
Last updated: 2026-07-23

This folder resolves the product decisions required before visual-reference selection, wireframing, and Compose Multiplatform design-system implementation.

## Document order

1. `00_PRODUCT_CONTEXT.md` — product promise, audience, platform truth, privacy boundaries, and MVP scope.
2. `01_MVP_SCREEN_MAP.md` — information architecture and required screen families.
3. `02_CORE_USER_FLOWS.md` — end-to-end behavior for setup, proposals, profiles, automations, and optional sign-in.
4. `03_UI_STATES_AND_CONTENT.md` — states, empty/error handling, permission states, and product language.
5. `04_DOMAIN_MODEL_AND_DECISIONS.md` — Notification Profile, Automation, Proposal, Account, and Activity concepts plus resolved decisions.
6. `05_DESIGN_BRIEF.md` — visual and interaction requirements for the next design-system phase.

## Source of truth

Once this branch is merged, the files in this repository folder are the canonical source for product UX and design decisions. Google Drive remains the collaboration and visual-assets workspace; changes that affect implementation must be promoted through Git review.

The implementation requirements in `specs/001-notimate-android-mvp/` must stay aligned with these documents. Older Google Drive files remain useful for architecture and research context. If an older UX statement conflicts with this folder, use this folder and reconcile the older document explicitly rather than maintaining two editable authorities.

Key reconciliations:

- NotiMate is local-first and does not require login to use its core features.
- The MVP home experience is proposal-first, not chatbot-first.
- The local LLM is an extraction and automation-authoring capability, not the sole UI.
- Users organize automations into Profiles; exactly one Profile is active in the MVP.
- Calendar proposals are reviewed in NotiMate and completed through the Android Calendar insert UI.
- Raw notification content and proposal text are not cloud-synced.
- Shared Compose UI belongs under the repository's `shared/src/commonMain` structure, not a nonexistent `composeApp` module.

## Next documents

After this foundation is reviewed, create:

1. `06_REFERENCE_SOURCES.md`
2. `07_VISUAL_DIRECTION.md`
3. `08_DESIGN_SYSTEM.md`
4. `09_COMPONENT_INVENTORY.md`
5. A human-viewable visual preview or Figma library

Runtime Compose tokens and components will remain the implementation source of truth in the repository.
