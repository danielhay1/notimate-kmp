# NotiMate MVP Screen Map

Status: Canonical information architecture
Navigation model: Three primary destinations plus nested Settings

## Primary navigation

```text
Today | Automations | Activity
```

Settings opens from the top app bar and is not a fourth primary destination.

The active Profile is visible from Today and Automations. Profile management is part of Automations, not the Account screen.

## Screen hierarchy

```text
Launch
├── First-run onboarding
│   ├── Welcome
│   ├── Privacy and on-device processing
│   ├── Notification Access education
│   ├── Android Notification Access settings
│   ├── Notification Access verification
│   ├── Monitored apps
│   ├── Proposal alerts education
│   ├── Android POST_NOTIFICATIONS permission
│   ├── On-device AI readiness
│   ├── Starter Profile
│   └── Setup complete
│
└── Main application
    ├── Today
    │   ├── Processing status
    │   ├── Active Profile switcher
    │   ├── Proposal queue
    │   ├── Recent outcomes
    │   └── Proposal Detail
    │
    ├── Automations
    │   ├── Active Profile summary
    │   ├── Automation list
    │   ├── Automation Detail
    │   ├── Create/Edit Automation
    │   ├── Describe an Automation
    │   ├── Automation Review
    │   ├── Profile quick switcher
    │   ├── Manage Profiles
    │   └── Profile Detail
    │
    ├── Activity
    │   ├── Structured activity timeline
    │   └── Activity Detail
    │
    └── Settings
        ├── Account
        ├── Backup and sync
        ├── Permissions and access
        ├── Monitored apps
        ├── Proposal notifications
        ├── On-device AI
        ├── Data and privacy
        ├── Diagnostics
        └── About
```

## Onboarding screens

### Welcome

Purpose:

- Explain the outcome in one sentence.
- Establish that NotiMate works with notifications already received by Android.
- Start setup without requiring login.

Primary action: `Set up NotiMate`
Secondary action: none

### Privacy and on-device processing

Must communicate:

- Notification analysis happens on-device.
- Raw notification content is not uploaded.
- NotiMate cannot access full message history.
- External actions require confirmation in the MVP.
- The user can pause or revoke access.

Primary action: `Continue`

### Notification Access education

Purpose:

- Explain why the broad Android special access is required.
- Show what NotiMate can and cannot do.
- Prepare the user for an Android-owned settings screen.

Primary action: `Open Android settings`
Secondary action: `Not now`

### Notification Access verification

States:

- Granted.
- Not granted.
- Access was revoked.
- Android settings screen unavailable.

The application must verify the actual state after returning from Android settings.

### Monitored apps

Purpose:

- Let the user constrain which notification sources NotiMate processes.
- Default to a small understandable selection, not every application.

The first release may offer recommended categories plus applications NotiMate has safely observed. Avoid promising access to applications Android does not expose.

### Proposal alerts education

Purpose:

- Explain that NotiMate uses its own notifications to surface proposals.
- Explain that processing can still work without proposal alerts.

Primary action: `Enable proposal alerts`
Secondary action: `Not now`

The primary action invokes Android's `POST_NOTIFICATIONS` runtime request on Android 13+.

### On-device AI readiness

States:

- Ready.
- Download required.
- Downloading.
- Temporarily unavailable.
- Unsupported.
- Device constrained.

Do not present unsupported AI as a setup failure. Explain which NotiMate features remain available.

### Starter Profile

Default:

```text
Personal Profile

- Suggest possible calendar events
- Keep important alerts visible
- Group delivery updates
- Always ask before external actions
```

Primary action: `Use Personal Profile`
Secondary action: `Customize`

### Optional account prompt

Do not place before core setup.

If shown after setup:

Title: `Back up your setup`
Primary action: `Continue with Google`
Secondary action: `Not now`

The screen must state exactly what is and is not synced.

## Today

Required regions:

1. Processing state.
2. Active Profile chip.
3. Proposals requiring attention.
4. Recent safe outcomes.
5. Privacy reassurance or degraded-state explanation.

Do not make metrics the dominant content.

Example:

```text
NotiMate is active
[Personal Profile ▾]

Needs your review
- Possible calendar event
- Time needs confirmation

Recently handled
- 8 notifications ignored by your automations
```

## Proposal Detail

Calendar proposal fields:

- Proposal type.
- Source application.
- Originating Profile and Automation.
- Editable title.
- Editable date.
- Editable start and end time.
- Editable location.
- Explanation of why the proposal was created.
- Ambiguity or missing-field guidance.

Actions:

- `Continue to Calendar`
- `Dismiss proposal`

The final event save occurs in the Calendar application.

## Automations

Required regions:

- Active Profile name and switch action.
- Number of enabled automations.
- Enabled and disabled automation list.
- `Create automation`.
- `Describe an automation` when local AI supports it.
- `Manage profiles`.

## Create/Edit Automation

Builder sections:

1. Sources.
2. Conditions and exclusions.
3. Proposed action.
4. Confirmation behavior.
5. Proposal notification behavior.
6. Name and appearance.
7. Review and activate.

The final review must present the automation as a readable sentence:

> When WhatsApp notifications contain a possible date and time, propose a calendar event and always ask me to review it.

## Manage Profiles

Required behavior:

- Display all Profiles.
- Show which Profile is active.
- Switch active Profile.
- Create a Profile.
- Open Profile Detail.
- Duplicate, rename, and delete through contextual actions.

MVP constraints:

- Exactly one active Profile.
- The only Profile cannot be deleted.
- Deleting the active Profile requires selecting a replacement first.
- Switching affects new notifications only.

## Activity

Displays structured privacy-safe records:

- Proposal created.
- Proposal dismissed.
- Calendar handoff started.
- Notification ignored by an automation.
- Processing deferred.
- Automation enabled or disabled.
- Profile switched.
- Permission lost.
- Local AI unavailable.

Activity must not reconstruct or expose raw notification bodies.

## Settings

### Account

- Signed-out state.
- Optional sign-in.
- Sign out.
- Account deletion when an account exists.

### Backup and sync

- Explicit opt-in.
- Clear list of synced data.
- Clear list of local-only data.
- Last successful sync.
- Error and retry behavior.

### Permissions and access

- Notification Access state.
- Proposal notification permission state.
- Direct links to relevant Android system settings.

### Data and privacy

- Local data summary.
- Retention controls.
- Clear local activity.
- Privacy explanation.
- Pause all processing.

## Deep-link destinations

The Android shell must be able to open at least:

- Proposal Detail by proposal identifier.
- Permissions and access.
- Active Profile Automations.

Notification taps must open the correct destination with a normal application back stack.
