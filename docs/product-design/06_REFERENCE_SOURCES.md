# NotiMate Reference Sources

Status: Curated reference board for visual-direction selection
Reviewed: 2026-07-23
Scope: Android-first MVP product design

## Purpose

This document collects references by interaction problem rather than selecting one application to copy.

The reference set should help NotiMate:

- Feel native and current on Android.
- Explain broad notification access honestly.
- Make proposals easy to review.
- Make Notification Profiles and Automations understandable.
- Preserve a distinct proposal-first identity.
- Avoid looking like Samsung Modes and Routines with different branding.

## Reference policy

- Official platform guidance takes precedence for Android behavior.
- Product references may inform layout, hierarchy, interaction, and language.
- Do not copy brand assets, illustrations, exact screens, or proprietary visual identity.
- Do not commit third-party screenshots to Git.
- Store internal reference captures in Google Drive only when their use is permitted.
- Every saved capture must retain its source URL, product name, capture date, and purpose.
- Dribbble, Pinterest, Mobbin, and similar libraries are mood and pattern sources, not behavioral authority.

## Reference map

| NotiMate problem | Primary references | Supporting references |
|---|---|---|
| Permission onboarding | Android notification design; Android Design for Safety | Digital Wellbeing |
| Primary navigation | Android layout and navigation patterns | Material 3 components |
| Today and proposal queue | Gmail categories and snooze | Digital Wellbeing |
| Proposal notification | Android notification design and navigation | Gmail notification actions |
| Calendar Proposal Detail | Google Calendar event editor | Google Tasks editor |
| Notification Profiles | Samsung Modes and Routines | Digital Wellbeing app selection |
| Automation builder | Samsung `If / Then`; Apple Shortcuts | Material lists, cards, and sheets |
| Confirmation policy | Apple Shortcuts automation confirmation | Android permission principles |
| Activity history | Gmail archive/snooze semantics | Digital Wellbeing dashboard |
| Privacy communication | Android Design for Safety | DuckDuckGo App Tracking Protection |
| Optional local AI | ML Kit structured output guidance | Structured builder fallback |

## 1. Android notification design

Source: [Android notification design](https://developer.android.com/design/ui/mobile/guides/home-screen/notifications)

Use for:

- Proposal-alert permission education.
- Standard notification structure.
- Action placement.
- Lock-screen-safe variants.
- Notification settings entry points.

Borrow:

- Explain the benefit before requesting notification permission.
- Make the educational surface dismissible.
- Use standard Android notification templates.
- Keep notification actions short and specific.
- Give users notification controls inside Settings.

Do not copy:

- Messaging or media templates that do not match an action proposal.
- Rich custom notification layouts.
- Permission prompts shown before the value is understood.

NotiMate application:

- Body tap and `Review` open Proposal Detail.
- `Dismiss` explicitly rejects the proposal.
- Swiping removes the alert but retains the proposal in Today.
- Lock-screen content stays generic until the device is unlocked.

## 2. Android notification navigation

Source: [Start an activity from a notification](https://developer.android.com/develop/ui/views/notifications/navigation)

Use for:

- Proposal deep links.
- Back-stack behavior.
- Extending a compact notification into a full review screen.

Borrow:

- Open the exact content represented by the notification.
- Preserve expected Back and Recents behavior.
- Treat the destination screen as an extension of the notification.

Do not copy:

- Special-purpose task behavior when Proposal Detail belongs to the normal application hierarchy.
- Indirect service or receiver trampolines.

## 3. Android privacy and permission communication

Sources:

- [Design for Safety](https://developer.android.com/quality/privacy-and-security)
- [Request runtime permissions](https://developer.android.com/training/permissions/requesting)
- [Minimize permission requests](https://developer.android.com/privacy-and-security/minimize-permission-requests)

Use for:

- Privacy onboarding.
- Notification Access education.
- Proposal-alert permission education.
- Denied and revoked states.

Borrow:

- Explain what is accessed, why it is needed, and what changes if access is denied.
- Ask in context.
- Minimize permissions.
- Preserve useful degraded behavior.
- Keep system access transparent and user-controlled.

Do not copy:

- Generic legal or compliance language as primary onboarding copy.
- A single screen that asks for several unrelated permissions.
- Fear-based messaging.

## 4. Material 3 structure and components

Sources:

- [Material components for Compose](https://developer.android.com/develop/ui/compose/components)
- [Layouts and navigation patterns](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns)
- [Content composition and structure](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure)
- [Common layouts](https://developer.android.com/design/ui/mobile/guides/layout-and-content/common-layouts)
- [Adaptive navigation](https://developer.android.com/develop/adaptive-apps/guides/build-adaptive-navigation)

Use for:

- Today, Automations, Activity, and Settings navigation.
- Proposal cards.
- Profile switcher bottom sheet.
- Form containment.
- Compact and expanded layouts.

Borrow:

- Three primary navigation destinations on compact screens.
- Navigation rail adaptation for larger widths.
- A consistent 16 dp compact-screen outer margin as the starting point.
- Lists for repeated items and cards where one subject needs grouped content and actions.
- Bottom sheets for quick Profile selection.
- List-detail adaptation for larger screens.

Do not copy:

- A generic sample-app visual identity.
- Cards around every piece of content.
- A phone bottom-navigation bar stretched unchanged across tablets.

## 5. Samsung Modes and Routines

Sources:

- [Samsung Modes and Routines](https://www.samsung.com/us/support/answer/ANS10002538/)
- [Samsung Routines walkthrough](https://www.samsung.com/uk/support/mobile-devices/how-to-use-routines-on-your-samsung-galaxy-device/)

Use for:

- Automation templates.
- `If / Then` comprehension.
- Enable, disable, edit, and duplicate behaviors.
- Named configurations with icons and colors.

Borrow:

- Plain-language condition and action sections.
- Progressive condition/action selection.
- Recommended starting templates.
- A readable summary before save.
- Fast enable and disable controls.

Do not copy:

- Samsung's `Mode` terminology.
- Separate Samsung-style Modes and Routines tabs.
- Device-control categories such as brightness, Wi-Fi, sound, or wallpaper.
- Samsung visual styling or icon treatment.
- The premise that automation is primarily device-state control.

NotiMate distinction:

> Samsung changes device behavior when a known condition occurs. NotiMate interprets notification meaning and creates an explainable, reviewable proposal.

## 6. Google Calendar event editor

Source: [Create or edit an event on Android](https://support.google.com/calendar/answer/72143?co=GENIE.Platform%3DAndroid&hl=en)

Use for:

- Calendar Proposal Detail.
- Editable title, date, time, location, guests, and calendar concepts.
- Final handoff expectations.

Borrow:

- Clear field hierarchy.
- Progressive disclosure for secondary event details.
- A visible final save action in the Calendar-owned UI.
- Familiar date/time and location patterns.

Do not copy:

- Calendar branding.
- Fields NotiMate cannot reliably extract or safely infer.
- Language claiming the event is saved before Calendar confirms it.

NotiMate application:

- Keep the initial Proposal Detail smaller than a full Calendar editor.
- Show the extracted fields required to understand the proposal.
- Use `Continue to Calendar` for the final external review.

## 7. Google Tasks structured editor

Source: [Add or edit a task on Android](https://support.google.com/tasks/answer/7675838?co=GENIE.Platform%3DAndroid&hl=en-GB)

Use for:

- Lightweight structured proposal editing.
- Progressive optional details.
- Future task proposals.

Borrow:

- Title-first entry.
- Date and time as optional structured additions.
- Simple completion and editing behavior.
- Low visual density for small action objects.

Do not copy:

- Task-list navigation as NotiMate's main structure.
- Immediate completion semantics for unreviewed proposals.

## 8. Gmail triage and reversible actions

Sources:

- [Gmail inbox categories](https://support.google.com/mail/answer/3094499/add-or-remove-inbox-categories-amp-tabs-in-gmail-android)
- [Snooze on Android](https://support.google.com/mail/answer/7622010?co=GENIE.Platform%3DAndroid&hl=en)
- [Archive on Android](https://support.google.com/mail/answer/6576?co=GENIE.Platform%3DAndroid&hl=en)

Use for:

- Today proposal queue.
- Deferred versus dismissed semantics.
- Reversible actions and short confirmations.
- Separating attention-worthy items from background outcomes.

Borrow:

- Strong distinction between active queue and archived history.
- Snooze/defer as different from rejection.
- Snackbar confirmation with undo where safe.
- Scannable rows that expose the most important information first.

Do not copy:

- Email-specific sender/thread density.
- Gmail categories as a replacement for Notification Profiles.
- Swipe behavior without clear discoverability or accessibility alternatives.

## 9. Apple Shortcuts

Sources:

- [Run and inspect a Shortcut](https://support.apple.com/guide/shortcuts/run-a-shortcut-from-the-app-apd5ba077760/ios)
- [Enable, disable, and confirm personal automation](https://support.apple.com/en-ie/guide/shortcuts/apd602971e63/ios)
- [Personal automation overview](https://support.apple.com/en-mide/guide/shortcuts/apd690170742/ios)

Use for:

- Sequential action readability.
- Automation enable/disable.
- Confirmation-policy language.
- Previewing what an automation will do.

Borrow:

- Visible ordered actions.
- Explicit enabled state.
- Clear distinction between asking before running and automatic execution.
- Permission prompts tied to the action that needs private data.

Do not copy:

- iOS navigation or component styling.
- A block-programming editor for the MVP.
- Automatic external actions in NotiMate's initial confirmation policy.

## 10. Android Digital Wellbeing

Source: [Digital Wellbeing on Android](https://support.google.com/android/answer/9346420?hl=en)

Use for:

- Processing-status overview.
- Monitored-app selection.
- Calm system-adjacent tone.
- Compact activity summaries.

Borrow:

- Status first, detail second.
- Per-app rows with direct controls.
- Calm presentation of behavioral information.
- Clear active, paused, and scheduled states.

Do not copy:

- Chart-heavy dashboard as NotiMate's home.
- Screen-time metrics unrelated to actionable notification proposals.
- System Settings visual identity.

## 11. DuckDuckGo App Tracking Protection

Source: [How App Tracking Protection works](https://duckduckgo.com/duckduckgo-help-pages/p-app-tracking-protection/how-does-app-tracking-protection-work)

Use for:

- Privacy explanations.
- Active protection/processing language.
- Explaining local background behavior without alarm.

Borrow:

- Plain-language explanation of what the background feature observes and blocks.
- Clear active/inactive state.
- Specific boundaries rather than vague privacy claims.

Do not copy:

- VPN metaphors.
- Protection or security guarantees NotiMate does not provide.
- DuckDuckGo brand colors, shield motifs, or illustrations.

## 12. ML Kit structured output

Source: [Generate structured output with the on-device Prompt API](https://developers.google.com/ml-kit/genai/prompt/android/structured-output)

Use for:

- Technical feasibility of event name, date, and location extraction.
- Designing AI-assisted drafts as structured objects.
- AI-availability and fallback states.

Borrow:

- Structured schema framing.
- Feature-availability checking.
- Validation before product use.

Do not copy:

- Developer terminology into user-facing UI.
- Alpha/beta capability assumptions as guaranteed product behavior.
- Model output as direct authorization for a side effect.

## Secondary research libraries

### Mobbin

Source: [Mobbin](https://mobbin.com/)

Use for:

- Comparing real shipped onboarding, permission, bottom-sheet, list-detail, and activity-feed patterns.
- Finding alternative implementations after the official reference set establishes behavior.

Constraints:

- Search access may require an account or paid plan.
- Do not commit Mobbin screenshots or credentials.
- Record the original product, screen, and Mobbin link for each saved internal reference.

Suggested searches:

- `notification permission onboarding Android`
- `automation builder mobile`
- `rule builder if then`
- `review proposal detail`
- `privacy dashboard Android`
- `activity history mobile`

### Dribbble and Pinterest

Use only for:

- Color atmosphere.
- Illustration style.
- Surface and shape mood.

Do not use for:

- Android permission behavior.
- Navigation architecture.
- Notification interactions.
- Accessibility decisions.
- Product flows.

## Candidate visual directions

### Direction A: Calm Material

Recommended starting point.

Reference mix:

- Material 3 structure.
- Android permission guidance.
- Google Calendar and Tasks editors.
- Digital Wellbeing restraint.
- DuckDuckGo-style privacy clarity.

Characteristics:

- Spacious layouts.
- Warm neutral surfaces.
- Restrained blue or blue-teal primary.
- Moderate rounding.
- Status and explanation before metrics.
- Familiar Android controls.

Strengths:

- Highest trust.
- Strong Android fit.
- Easiest to implement accessibly in Compose Multiplatform.
- Scales from beginner to advanced use.

Risk:

- Can feel generic unless NotiMate's proposal cards, Profile identity, and privacy indicators are distinctive.

### Direction B: Operational Control

Reference mix:

- Samsung Routines composition.
- Apple Shortcuts action visibility.
- Gmail triage density.
- Material lists and sheets.

Characteristics:

- Denser information.
- Strong `When / Then / Confirm` sections.
- More visible toggles, status chips, and summaries.
- Compact Profile and Automation management.

Strengths:

- Excellent for power users.
- Makes automation structure explicit.
- Supports many Automations without excessive scrolling.

Risk:

- Can feel like device settings.
- Increases cognitive load during onboarding and proposal review.

### Direction C: Human Assistant

Reference mix:

- Material foundations.
- Google Tasks simplicity.
- Conversational explanation panels.
- Warm privacy language.

Characteristics:

- Softer surfaces and warmer color.
- More explanatory copy.
- Guided steps.
- AI assistance presented as an optional helper.

Strengths:

- Friendly for automation beginners.
- Differentiates from settings-heavy products.
- Supports natural-language Automation drafting.

Risk:

- Can over-personify AI.
- Can become verbose and reduce scan speed.
- Must not imply certainty or human understanding.

## Recommendation

Use Direction A as the foundation and borrow Direction B's builder clarity for Automations.

Do not make Direction C the overall system. Use its warmer explanation treatment only in onboarding, privacy, ambiguity, and optional AI-assistance surfaces.

Proposed synthesis:

```text
Calm Material shell
├── Proposal-first Today
├── Distinctive privacy and processing status
├── Structured Profile identity
├── Operational When / Then / Confirm builder
└── Warm explanations only when guidance is needed
```

## Direction-selection criteria

Score each direction against:

| Criterion | Weight |
|---|---:|
| User trust and privacy clarity | 30% |
| Proposal review clarity | 25% |
| Android platform fit | 20% |
| Automation scalability | 15% |
| Visual differentiation | 10% |

## Next decision

Choose one:

1. Direction A: Calm Material.
2. Direction B: Operational Control.
3. Direction C: Human Assistant.
4. Recommended hybrid: Direction A foundation with Direction B automation-builder density.

After selection, create `07_VISUAL_DIRECTION.md` and low-fidelity wireframes for:

1. Privacy onboarding.
2. Notification Access education.
3. Today with proposals.
4. Calendar Proposal Detail.
5. Profile switcher.
6. Automations list.
7. `When / Then / Confirm` builder.
