# NotiMate Component Inventory

Status: Proposed runtime inventory; awaiting implementation approval
Updated: 2026-08-21

## Inventory Rules

- Prefer Material 3 directly for buttons, text fields, switches, checkboxes,
  dialogs, snackbars, progress, and basic chips.
- Create a NotiMate component only when it encodes recurring product structure or
  semantics.
- Components render immutable state and emit `onAction`; they do not call
  repositories, navigation, permissions, notifications, or Calendar APIs.
- Component names below are candidates for runtime implementation, not a requirement
  to create every component in the first code slice.

## Shared Product Components

| Candidate | Responsibility | Key states/actions | Used by |
|---|---|---|---|
| `NotiMateAppShell` | Shared destination chrome and adaptive navigation slot | Destination selected; navigation action | Main application |
| `ProcessingStatusBanner` | Current processing state and safe next action | Active, paused, permission required, degraded, unavailable; resume/retry/settings | Today, Settings |
| `ActiveProfileChip` | Show and open the active Profile | Active name; open switcher | Today, Automations |
| `ProfileSwitcher` | Select or manage Profiles | Active/inactive/only Profile; select/manage | Today, Automations |
| `ProfileRow` | Summarize a Profile | Active, inactive, empty, cannot delete; open/context action | Manage Profiles |
| `ProposalCard` | Summarize a reviewable Proposal | Needs review, ready, deferred, failed, expired; review/dismiss/retry | Today |
| `ProposalFieldEditor` | Edit one structured Proposal field | Value, missing, ambiguous, invalid, read-only; edit | Proposal Detail |
| `ExplanationPanel` | Explain why a Proposal exists or is uncertain | Neutral, review, privacy-safe warning | Proposal Detail, Automation Review |
| `AutomationRow` | Summarize an Automation without opening it | Enabled, disabled, draft, unsupported, needs attention; toggle/open | Automations |
| `AutomationBuilderSection` | Stable `When / Then / Confirm` editing section | Complete, incomplete, invalid, disabled; edit | Create/Edit Automation |
| `AutomationSummary` | Plain-language final Automation sentence | Valid, warning, unsupported; edit/activate | Automation Review |
| `ActivityRow` | Show one privacy-safe outcome | Created, dismissed, handed off, ignored, deferred, permission lost | Activity |
| `PermissionEducationPanel` | Explain access before a platform handoff | Informational, denied, revoked, unavailable; continue/not now | Onboarding, Settings |
| `CapabilityPanel` | Describe optional local-AI capability and fallback | Ready, download required, downloading, unsupported, constrained; download/fallback/retry | Onboarding, Settings, Describe Automation |
| `MonitoredSourceRow` | Select and explain a notification source | Selected, unselected, unavailable; toggle | Onboarding, Settings |
| `PrivacyIndicator` | Concise on-device/local-only reassurance | Standard, compact | Onboarding, Today, Sync disclosure |
| `MessagePanel` | Product error, warning, ambiguity, or degraded message | Review, warning, failure, paused; primary/secondary recovery | Any destination |
| `NotiMateEmptyState` | Explain an empty state with next action | New user, caught up, empty Profile, no Activity | Today, Automations, Activity |

Use Material components directly for ordinary app bars, navigation bar/rail,
buttons, fields, sheets, dialogs, snackbars, switches, checkboxes, progress, and
chips. Wrap them only when NotiMate semantics or defaults would otherwise be
duplicated across several features.

## Screen Coverage

| Screen family | Required product components | Compact layout | Wider layout |
|---|---|---|---|
| Welcome/privacy onboarding | `PrivacyIndicator`, `PermissionEducationPanel` | Centered single column | Centered readable column |
| Notification Access | `PermissionEducationPanel`, `MessagePanel` | Single column and Android handoff | Same content width; no extra pane |
| Monitored apps | `MonitoredSourceRow`, `NotiMateEmptyState` | Selectable list | Wider list or two-column flow where useful |
| Proposal alerts | `PermissionEducationPanel`, `MessagePanel` | Single column | Same content width |
| Local-AI readiness | `CapabilityPanel` | Single panel | Same content width |
| Starter Profile | `ProfileRow`, `AutomationRow` | Summary list | Summary and optional supporting preview |
| Today | `ProcessingStatusBanner`, `ActiveProfileChip`, `ProposalCard`, `ActivityRow`, `NotiMateEmptyState` | Feed | Feed plus selected Proposal detail when useful |
| Proposal Detail | `ProposalFieldEditor`, `ExplanationPanel`, `MessagePanel` | One detail pane | Detail pane with originating list when available |
| Automations | `ActiveProfileChip`, `AutomationRow`, `NotiMateEmptyState`, `CapabilityPanel` | List | List-detail |
| Create/Edit Automation | `AutomationBuilderSection`, `AutomationSummary`, `MessagePanel` | Step or scrolling form | Readable builder with supporting summary |
| Manage Profiles | `ProfileRow`, `ProfileSwitcher`, `MessagePanel` | List | List-detail |
| Activity | `ActivityRow`, `NotiMateEmptyState`, `MessagePanel` | Timeline list | List-detail |
| Settings | `ProcessingStatusBanner`, `MonitoredSourceRow`, `CapabilityPanel`, `PrivacyIndicator` | Settings list and nested pages | List-detail |
| Account and sync | `PrivacyIndicator`, `MessagePanel` | Disclosure and actions | Same content width or settings detail pane |

Every screen family in `01_MVP_SCREEN_MAP.md` is covered by this table. Platform-owned
Android settings, permission dialogs, notification templates, and Calendar editors
are handoffs rather than shared NotiMate screens.

## State Models

Keep state models small and product-named. Examples:

```text
ProcessingStatusUiState
ProposalCardUiState
ProposalFieldUiState
ProfileRowUiState
AutomationRowUiState
CapabilityUiState
MessageUiState
```

Prefer a sealed state when variants have different required data. Avoid scattered
boolean combinations such as `isLoading + isError + isPaused` when the states are
mutually exclusive.

Events should describe user intent:

```text
ReviewProposal
DismissProposal
SelectProfile
OpenPermissionSettings
RetryProcessing
EditAutomationSection
```

Leaf components may use `onAction` or a small specific callback. Destination
composables should normally expose one screen-level `onAction` entry point.

## Adaptive Ownership

- `NotiMateAppShell` owns navigation chrome selection from one shared layout mode.
- Destination state holders own selection; pane layout only decides where it renders.
- List-detail layouts share the same list, selection, and detail state across window
  changes.
- Feature components do not query window size independently unless their internal
  presentation genuinely changes.
- Back behavior remains route/state behavior, not a side effect of current pane
  visibility.

## Preview Set

Create previews incrementally with each implemented component. The minimum useful
set is:

| Preview group | Variants |
|---|---|
| Theme | Light, dark |
| Direction | English LTR, Hebrew RTL |
| Text | Default, large text |
| Processing | Active, paused, permission required, degraded |
| Proposal | Needs review, ready, deferred, failed, expired |
| Profiles/Automations | Active, empty, disabled, needs attention |
| Capability | Ready, downloading, unsupported, constrained |
| Adaptive shell | Compact navigation, expanded rail and list-detail |

Use centralized synthetic preview fixtures outside production workflow logic. Do not
include real notification text, personal names, financial data, authentication
codes, or application screenshots.

## Platform Boundary

Shared components may emit intents such as `OpenNotificationAccessSettings` or
`ContinueToCalendar`, but they must not import Android APIs or execute those effects.

Android owns:

- permission and settings launchers;
- `NotificationListenerService` state integration;
- proposal notification construction and deep-link `PendingIntent` creation;
- Calendar `ACTION_INSERT` construction and launch.

The shared UI owns the visible state, explanation, editable structured fields, and
user intent leading to those platform actions.

## Initial Runtime Slice

Task 004 should begin with only:

1. `NotiMateTheme` and semantic colors;
2. spacing and shape tokens;
3. `ProcessingStatusBanner`;
4. `ProposalCard`;
5. synthetic light/dark previews.

Add other inventory items only when the corresponding screen task needs them.
