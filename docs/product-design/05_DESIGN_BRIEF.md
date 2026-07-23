# NotiMate Design Brief

Status: Input to visual-reference selection and design-system definition

## Design objective

Create a trustworthy Android-first product that makes powerful notification automation feel understandable, calm, and reversible.

The experience should communicate:

- Quiet competence.
- On-device privacy.
- User control.
- Clear system state.
- Conservative automation.
- Modern Android quality without looking like a system-settings clone.

## Primary audiences

### Busy organizer

Wants useful dates, tasks, deliveries, and financial alerts extracted without manually reviewing every notification.

### Privacy-conscious power user

Wants automation but needs to understand permissions, data movement, and external actions.

### Automation beginner

Benefits from templates and natural-language setup but should not need to understand rule-engine terminology.

## Hero journeys for design references

Reference selection must cover these journeys rather than copying one entire application:

1. Permission education followed by Android-owned settings.
2. Today dashboard with one or more proposals.
3. Proposal notification and Proposal Detail.
4. Calendar event review and handoff.
5. Mode switching and Mode management.
6. Structured `when/then` automation builder.
7. Natural-language automation draft converted to a structured rule.
8. Activity timeline with privacy-safe outcomes.
9. Optional account and sync disclosure.
10. Empty, denied, unsupported, paused, and error states.

## Recommended reference categories

Use official platform patterns first:

- Material 3 mobile components and navigation.
- Android permission and settings patterns.
- Android notification templates and actions.
- Android Calendar insert flow.

Use product references for specific interaction problems:

- Samsung Modes and Routines for Mode selection, templates, and `If/Then` composition.
- Task and calendar products for editable structured proposals.
- Privacy-focused products for plain-language disclosures.
- Finance or security products for confidence, review, and confirmation states.
- Email triage products for review queues and activity history.

Dribbble, Pinterest, and similar sources may inform visual mood only. They must not define platform behavior or product flows.

## Visual direction

Recommended starting direction:

- Calm productivity rather than futuristic AI spectacle.
- Restrained blue or blue-teal primary color.
- Warm neutral surfaces.
- Strong but not alarming semantic states.
- Moderate corner radii.
- Clear information hierarchy.
- Sparse illustration use.
- Minimal gradients and glow effects.
- Motion used for state continuity, not decoration.

AI should appear as a capability label or assistive affordance, not as a glowing omnipresent personality.

## Required semantic roles

The design system must define roles for:

- Primary action.
- Secondary action.
- Destructive action.
- Success or completed.
- Needs review.
- Warning or ambiguity.
- Error or failed.
- Paused.
- Disabled.
- Privacy/on-device reassurance.
- AI available.
- AI degraded or unsupported.

Semantic meaning must not rely on color alone.

## Required component families

### Foundations

- Color tokens.
- Typography.
- Spacing.
- Shape.
- Elevation.
- Motion.
- Iconography.
- Responsive width and inset rules.

### Navigation

- Top app bar.
- Bottom navigation.
- Back navigation.
- Mode switcher sheet.
- Settings list navigation.

### Product components

- Processing status banner.
- Active Mode chip.
- Mode card and Mode row.
- Automation row and summary card.
- `When / Then / Confirm` builder section.
- Proposal card.
- Proposal field editor.
- Explanation panel.
- Privacy indicator.
- AI availability panel.
- Activity row.
- Permission education panel.
- Empty state.
- Error and degraded-state panel.

### Actions and feedback

- Primary, secondary, tonal, text, and destructive buttons.
- Snackbar.
- Confirmation dialog.
- Progress indicator.
- Toggle and checkbox.
- Chips for sources and conditions.
- Inline validation.

## Android interaction constraints

- Use standard Android notification templates.
- Do not attempt multi-field proposal editing inside notifications.
- Notification body tap and Review action open Proposal Detail directly.
- Preserve a normal application back stack.
- Ask `POST_NOTIFICATIONS` in context on Android 13+.
- Treat Notification Access as a separate Android special-access journey.
- Do not design an MVP calendar permission request.
- Provide graceful in-app states when system permissions are denied.

## KMP implementation constraints

- Shared visual tokens and platform-neutral components belong in `shared/src/commonMain`.
- Android permission launchers, Notification Listener integration, notification posting, and Calendar intents remain Android platform edges.
- Shared composables receive immutable state and emit events.
- Screens must be previewable with synthetic, privacy-safe sample data.
- RTL and dynamic text behavior must be considered during component design.
- Raw notification examples must never be used in committed previews, tests, or screenshots.

## Design completion criteria

Visual design is ready for implementation when:

- Every primary screen family has a wireframe.
- The calendar hero flow is represented end to end.
- Permission denial and revocation states are designed.
- Proposal notification content and lock-screen-safe content are specified.
- Mode switching and Mode management are unambiguous.
- Builder states cover structured and AI-assisted entry.
- English and Hebrew/RTL samples have been checked.
- Light and dark theme behavior is defined.
- Component inventory maps every screen to reusable components.
- Tokens can be translated directly into Compose theme values.

## Next step

Create `06_REFERENCE_SOURCES.md` with a small, curated reference set. For each source, record:

- Product and screen.
- URL.
- Interaction problem it solves.
- What to borrow.
- What not to copy.
- Platform or licensing notes.
- Date reviewed.

Then select one coherent visual direction and build the first low-fidelity wireframes before finalizing color and typography.
