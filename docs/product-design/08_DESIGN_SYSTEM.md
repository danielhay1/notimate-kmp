# NotiMate Design System

Status: Proposed runtime contract; awaiting implementation approval
Updated: 2026-08-21

## Purpose

Translate the approved Calm Material direction into a small, accessible Compose
Multiplatform system. Prefer Material 3 components and semantics; add NotiMate
tokens or components only when they encode recurring product meaning.

## Principles

- State first: show what NotiMate is doing and what the user can do next.
- One clear primary action per decision area.
- Use containers selectively; spacing and hierarchy should do most grouping.
- Components render immutable state and emit actions.
- Shared UI stays platform-neutral and previewable with synthetic data.
- Adaptive decisions use the current window, not device labels or orientation.

## Color

### Material Color Scheme

Use these stable defaults for the first runtime implementation. Android dynamic
color is deferred so the shared Android/iOS experience has one predictable identity.

| Role | Light | On light | Dark | On dark |
|---|---|---|---|---|
| Primary | `#315F55` | `#FFFFFF` | `#9FCFC0` | `#00382E` |
| Primary container | `#B5DACE` | `#0B211C` | `#164B41` | `#BBECE0` |
| Secondary | `#4E635D` | `#FFFFFF` | `#B5CCC4` | `#21332E` |
| Secondary container | `#D1E8E0` | `#0B201B` | `#374B45` | `#D1E8E0` |
| Tertiary | `#5B5F7A` | `#FFFFFF` | `#C2C5E5` | `#2C3049` |
| Tertiary container | `#E1E2FF` | `#171A32` | `#424760` | `#E1E2FF` |
| Error | `#B3261E` | `#FFFFFF` | `#F2B8B5` | `#601410` |
| Error container | `#F9DEDC` | `#410E0B` | `#8C1D18` | `#F9DEDC` |
| Background/surface | `#F8FAF7` | `#191C1A` | `#101412` | `#E0E3DF` |
| Surface variant | `#DDE5E0` | `#414945` | `#414945` | `#C1C9C4` |
| Outline | `#717974` | — | `#8B938E` | — |
| Outline variant | `#C1C9C4` | — | `#414945` | — |

All text/container pairs above exceed WCAG AA contrast for normal text. Revalidate
generated theme code rather than assuming later palette changes remain compliant.

### Product Semantic Colors

Use a small `NotiMateSemanticColors` extension alongside `MaterialTheme`.

| Meaning | Light container/content | Dark container/content | Required companion cue |
|---|---|---|---|
| Success/completed | `#D5EBDD` / `#123722` | `#244A34` / `#B8F0CA` | Check icon and completed label |
| Needs review/ambiguity | `#F6E9BE` / `#332B00` | `#4C4300` / `#F6E9BE` | Review icon and explanation |
| Privacy/on-device | Secondary container pair | Secondary container pair | Privacy icon and explicit text |
| Paused | `#E8E1D5` / `#322E28` | `#49443C` / `#E8E1D5` | Pause icon and Resume action |
| AI available | Tertiary container pair | Tertiary container pair | Capability label |
| AI degraded | Surface variant pair | Surface variant pair | Named fallback action |
| Failed/destructive | Error container pair | Error container pair | Error icon and recovery text |
| Disabled | Material disabled treatment | Material disabled treatment | Disabled control semantics |

Do not color-code Proposal lifecycle values directly. Map lifecycle to user meaning:
`NeedsReview` uses review, `Ready` remains primary/neutral, `HandedOff` uses success
language only for the verified handoff, and `Deferred`, `Failed`, or `Expired` use
explicit labels and next actions.

## Action Hierarchy

| Role | Material treatment | Rule |
|---|---|---|
| Primary action | Filled button using primary roles | At most one per decision area |
| Secondary action | Tonal or outlined button | Visible alternative without competing with primary |
| Low-emphasis action | Text button | Navigation, dismissal, or optional action |
| Destructive action | Error-colored text or outlined button | Use a filled error button only in the final destructive confirmation |
| Inline state change | Switch, checkbox, or chip as appropriate | Label the result; do not rely on control position alone |

Disable an action only when the requirement to enable it is clear nearby. Prefer
inline validation and recovery guidance over unexplained disabled controls.

## Typography

Use the platform sans-serif through Material 3. Do not bundle a custom font for the
first implementation.

| Product role | Material role | Size / line height | Weight |
|---|---|---:|---:|
| Onboarding hero | `headlineSmall` | 24 / 32 sp | 500 |
| Screen title | `titleLarge` | 22 / 28 sp | 500 |
| Section title | `titleMedium` | 16 / 24 sp | 500 |
| Item title | `titleSmall` | 14 / 20 sp | 500 |
| Primary body | `bodyLarge` | 16 / 24 sp | 400 |
| Supporting body | `bodyMedium` | 14 / 20 sp | 400 |
| Action label | `labelLarge` | 14 / 20 sp | 500 |
| Metadata | `bodySmall` | 12 / 16 sp | 400 |

Allow system font scaling. Do not cap text scale or replace content with ellipsis
when wrapping can preserve meaning.

## Spacing And Sizing

Use one 4 dp-based spacing scale: `space4`, `space8`, `space12`, `space16`,
`space24`, `space32`, and `space48`.

- Compact outer inset: `space16`.
- Wider-window content inset: `space24` or `space32`.
- Related item gap: `space8` or `space12`.
- Section gap: `space24`.
- Minimum touch target: 48 dp.
- Readable form/detail width: approximately 600–720 dp, constrained by layout.

Do not create tokens for one-off measurements until repetition proves they belong in
the system.

## Shape And Elevation

| Role | Shape |
|---|---:|
| Small controls and fields | 8 dp |
| Rows and compact panels | 12 dp |
| Cards and status panels | 16 dp |
| Sheets and prominent panels | 24 dp |
| Chips and pill actions | Full |

Prefer tonal separation to shadows. Use no elevation for ordinary rows, low tonal
elevation for cards and app chrome, and a restrained shadow only for transient
overlays such as sheets or dialogs.

## Motion

| Role | Duration | Use |
|---|---:|---|
| Short | 150 ms | Selection, visibility, small state changes |
| Medium | 250 ms | Sheet, pane, and destination continuity |
| Long | 350 ms maximum | Larger layout transitions only |

Motion must explain continuity, never loop decoratively, and respect reduced-motion
preferences by removing nonessential movement.

## Iconography

- Prefer standard Material icons for platform-familiar actions.
- Use one consistent outlined/rounded family.
- Give interactive icons explicit accessible labels.
- Mark decorative icons as such.
- Never use a sparkle icon as the general NotiMate or AI identity.

## Adaptive Layout

Read `WindowAdaptiveInfo` once at the shared app shell and derive a small layout
mode passed to destination content. Do not scatter window queries or raw dp
breakpoints across screens.

| Window width | Navigation | Content strategy |
|---|---|---|
| Compact | Bottom navigation | One pane; detail replaces list with normal Back behavior |
| Medium | Navigation rail | Wider single pane or list-detail when both panes remain useful |
| Expanded | Navigation rail | List-detail for Proposals, Automations, Activity, and Settings |

- Today uses a feed-like single column on compact windows and may show the selected
  Proposal as a detail pane when space allows.
- Proposal Detail, onboarding, and builders keep a readable maximum content width.
- Profile switching uses a modal bottom sheet on compact windows and an anchored or
  side presentation only when the same task flow is preserved.
- Resizing must preserve selected destination, selected item, edits, and scroll state
  owned above the layout branch.
- Height constraints, split screen, foldables, ChromeOS, and freeform windows use
  current window bounds rather than device detection.

## Components And State

- Destination composables accept one immutable screen state and `onAction`.
- Reusable components accept the smallest state required to render them.
- Business validation, persistence, platform launches, and navigation effects remain
  outside leaf composables.
- Persistent edits belong to state holders or repositories; truly local visual state
  may remain inside a component.
- One-time navigation, snackbar, permission, notification, and Calendar operations
  are effects handled by their owner, not rendering branches.

Exact component responsibilities are listed in `09_COMPONENT_INVENTORY.md`.

## Resources And Localization

- Put product strings, shared icons, and shared images in Compose Multiplatform
  resources under `shared/src/commonMain/composeResources`.
- Keep Android system labels and permission-specific platform plumbing at Android
  edges while passing display-ready shared state where practical.
- Use locale-aware date/time formatting; never assemble translated sentences from
  fragments.
- Mirror directional icons and layout order in RTL; do not mirror nondirectional
  product or source-application marks.
- Preview English and Hebrew with long text and large font scaling.
- Use synthetic names, applications, dates, and proposal content only.

## Accessibility

- Maintain at least 4.5:1 contrast for normal text and 3:1 for large text and
  meaningful non-text boundaries.
- Never rely on color alone for processing, Proposal, Profile, or AI state.
- Keep visible and semantic reading order aligned when panes rearrange.
- Group a logical row or card deliberately without silencing useful child content.
- Provide clear focus order, action labels, validation text, and recovery actions.
- Protect sensitive notification and Proposal content from lock-screen announcements.

## Preview And Verification Contract

Every reusable product component needs synthetic previews for its important semantic
states. Destination previews should cover only representative combinations:

- light and dark;
- compact and expanded where layout changes;
- English and Hebrew/RTL;
- large text;
- active, empty, needs-review, permission-required, and degraded states.

Test state mapping and adaptive layout decisions as pure logic when possible. Do not
add tests that merely reproduce Material or Compose framework behavior.

## Runtime Boundary

The first implementation belongs under
`shared/src/commonMain/kotlin/com/hayduck/notemate/designsystem/`. Android dynamic
color, permission launchers, notification UI, and Calendar intents are not part of
the shared design-system package.
