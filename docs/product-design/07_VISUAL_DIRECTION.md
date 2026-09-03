# NotiMate Visual Direction

Status: Approved visual direction and low-fidelity MVP wireframes
Approved: 2026-08-21

## Decision

Use **Calm Material** as the product foundation and borrow **Operational Control**
clarity for Automations.

NotiMate should feel like a quiet Android productivity tool: clear system state,
warm neutral surfaces, restrained emphasis, and explicit review before external
actions. AI appears as an optional capability, not the product's personality.

## Visual Principles

1. Lead with the current state and the next useful action.
2. Use familiar Material structure without resembling Android Settings.
3. Give Proposals and Profiles a recognizable identity; avoid decorating every
   section as a card.
4. Use `When / Then / Confirm` as the stable visual grammar for Automations.
5. Explain uncertainty, permissions, and privacy in plain language.
6. Pair semantic color with text, icons, or shape; never rely on color alone.
7. Keep motion short and functional: continuity, state change, and confirmation.

This document defines direction, not final token values. Color values, typography
scales, shape values, elevation, and motion timings belong in
`08_DESIGN_SYSTEM.md` after these wireframes are accepted.

## Reference Lessons

| Source | Use in NotiMate | Do not copy |
|---|---|---|
| [Android notifications](https://developer.android.com/design/ui/mobile/guides/home-screen/notifications) | Contextual permission education, standard templates, concise content, direct actions | Custom rich notification layouts or sensitive lock-screen text |
| [Android Design for Safety](https://developer.android.com/quality/privacy-and-security) | Data minimization, transparency, and user control | Fear-based or legalistic onboarding |
| [Android layout and navigation](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns) | Familiar mobile hierarchy and adaptive navigation | Phone navigation stretched unchanged to large screens |
| Google Calendar and Tasks | Familiar structured field order and progressive details | Calendar branding or unsupported fields |
| Samsung Routines and Apple Shortcuts | Readable condition, action, and confirmation structure | Device-control vocabulary or automatic MVP side effects |

The mapped Drive reference categories were checked on 2026-08-21. They contain
organizational README files only, so no screenshots are treated as collected or
approved visual evidence.

## Product Character

- **Calm:** quiet surfaces, limited emphasis, no decorative glow.
- **Trustworthy:** privacy and processing state remain visible without dominating.
- **Structured:** proposals and automations scan as understandable objects.
- **Conservative:** ambiguity looks reviewable, not alarming or falsely confident.
- **Android-native:** system permissions, notifications, and Calendar remain
  visibly platform-owned handoffs.

## Layout Direction

### Compact

- Use `Today | Automations | Activity` as bottom navigation.
- Keep Settings in the top app bar.
- Use one dominant column with clear section spacing.
- Use bottom sheets for quick Profile switching.
- Keep primary actions near the content they affect.

### Expanded

- Adapt primary navigation to a rail.
- Prefer list-detail layouts for Proposals, Automations, and Activity.
- Keep readable content widths; do not stretch forms across the window.
- Preserve the same information order as compact layouts.

## Color And Surface Direction

- Warm neutral application and content surfaces.
- Restrained blue or blue-teal for primary actions and active selection.
- Softer distinct treatments for review, warning, failure, paused, privacy, and
  degraded-AI states.
- Moderate rounding with less container chrome than the scaffold default.
- Light and dark themes use the same semantic hierarchy, not inverted decoration.

## Typography And Content Direction

- Use a clear Android-friendly sans-serif hierarchy.
- Prefer short state-led headings and direct action labels.
- Keep supporting explanations readable rather than visually minimized.
- Avoid model probabilities, anthropomorphic AI language, and unverified success
  claims.

## Accessibility And Localization

- Support Hebrew and right-to-left layout from the first component specification.
- Keep dates, times, weekdays, and numerals locale-aware.
- Allow large text without clipping actions or hiding state explanations.
- Use explicit labels and mobile-appropriate touch targets.
- Keep sensitive content out of locked-device announcements and previews.
- Use synthetic proposal and notification content in every design artifact.

## Low-Fidelity Wireframes

These wireframes define hierarchy and behavior only. They do not prescribe final
tokens, illustration, icon style, or exact component dimensions.

### 1. Privacy Onboarding

```text
┌──────────────────────────────┐
│ NotiMate                     │
│                              │
│ Your notifications stay      │
│ on this device               │
│                              │
│ ✓ Analyzed locally           │
│ ✓ External actions reviewed  │
│ ✓ Access can be paused       │
│                              │
│ [ Continue ]                 │
└──────────────────────────────┘
```

### 2. Notification Access Education

```text
┌──────────────────────────────┐
│ Notification Access          │
│                              │
│ Needed to notice useful      │
│ information in selected apps │
│                              │
│ Can: analyze notifications   │
│ Cannot: read full history    │
│                              │
│ [ Open Android settings ]    │
│ Not now                      │
└──────────────────────────────┘
```

### 3. Today

```text
┌──────────────────────────────┐
│ Today              Settings  │
│ NotiMate is active           │
│ [ Personal Profile ▾ ]       │
│                              │
│ NEEDS YOUR REVIEW            │
│ Possible calendar event      │
│ Tuesday · time uncertain     │
│ [ Review ]                   │
│                              │
│ RECENTLY HANDLED             │
│ 8 notifications ignored      │
│ Today  Automations  Activity │
└──────────────────────────────┘
```

### 4. Proposal Detail

```text
┌──────────────────────────────┐
│ ‹ Possible calendar event    │
│ Time needs confirmation      │
│                              │
│ Title     Project review     │
│ Date      Tuesday, 25 Aug    │
│ Time      [ Select time ]    │
│ Location  Studio 4           │
│                              │
│ Why this was proposed        │
│ Personal · Calendar events   │
│                              │
│ [ Continue to Calendar ]     │
│ Dismiss proposal             │
└──────────────────────────────┘
```

### 5. Profile Switcher

```text
┌──────────────────────────────┐
│ Today                        │
│                              │
│ ┌ Select Profile ──────────┐ │
│ │ ● Personal              │ │
│ │ ○ Work                  │ │
│ │ ○ Travel                │ │
│ │                         │ │
│ │ Manage Profiles         │ │
│ └─────────────────────────┘ │
└──────────────────────────────┘
```

### 6. Automations

```text
┌──────────────────────────────┐
│ Automations        Settings  │
│ [ Personal Profile ▾ ]       │
│ 3 enabled                    │
│                              │
│ Calendar event suggestions ● │
│ Delivery updates           ● │
│ Reduce promotions          ○ │
│                              │
│ [ Create automation ]        │
│ Describe an automation       │
│ Today  Automations  Activity │
└──────────────────────────────┘
```

### 7. Automation Builder

```text
┌──────────────────────────────┐
│ ‹ Create automation          │
│                              │
│ WHEN                         │
│ Selected apps + date/time    │
│                              │
│ THEN                         │
│ Propose a calendar event     │
│                              │
│ CONFIRM                      │
│ Always ask me to review      │
│                              │
│ [ Review automation ]        │
└──────────────────────────────┘
```

## Required State Variants

The design-system specification must derive variants for:

- active, paused, permission-required, and degraded processing;
- proposal `NeedsReview`, `Ready`, `Deferred`, `Failed`, and `Expired`;
- proposal alerts off while processing remains active;
- local AI ready, downloading, unavailable, unsupported, and device-constrained;
- empty Profiles and Automations needing attention;
- light, dark, English, Hebrew/RTL, and large-text layouts.

## Implementation Translation

Later Compose work should keep shared UI in `shared/src/commonMain`, pass immutable
state down, emit actions up, and leave Android permission launchers, notification
posting, and Calendar intents at Android platform edges.
