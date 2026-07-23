# NotiMate Domain Model and Product Decisions

Status: Product-level model; implementation mapping comes later

## Conceptual model

```text
LocalInstallation
├── ProcessingPreference
├── MonitoredSources
├── Modes
│   └── Automations
│       ├── Sources
│       ├── Conditions
│       ├── Action
│       └── ConfirmationPolicy
├── Proposals
└── ActivityRecords

OptionalAccount
└── SyncableConfiguration
```

## Mode

A Mode groups automations that should apply together.

Product fields:

- Identifier.
- Name.
- Description.
- Icon.
- Color.
- Created and modified time.
- Active state.
- Ordered automations.

MVP invariant:

- At least one Mode exists.
- Exactly one Mode is active while processing is enabled.
- Pausing processing does not deactivate or delete the selected Mode.

Starter Mode:

- Name: `Personal`
- Calendar-event suggestions enabled.
- External actions always require review.

## Automation

An Automation is one understandable `when this, then propose that` behavior.

Product fields:

- Identifier.
- Owning Mode.
- Name.
- Enabled state.
- Sources.
- Conditions.
- Exclusions.
- Action.
- Confirmation policy.
- Proposal notification preference.
- Created and modified time.

MVP constraint:

An Automation belongs to one Mode. Reusable shared Automations may be introduced later through duplication or templates.

## Source

A Source identifies notification origin constraints, such as:

- Any monitored app.
- One selected application.
- A supported application category.
- A notification or conversation category when reliably available.

Do not promise conversation-level selection unless Android and the source notification expose a stable identifier.

## Condition

A Condition determines whether an Automation applies.

Examples:

- A possible date and time is present.
- Notification is classified as promotional.
- Notification is classified as financial.
- Specific locally stored text matches.
- Importance or Android notification category matches.

Free-text values are sensitive local configuration by default.

## Action

Initial action types:

- Propose a calendar event.
- Ignore or reduce attention for a notification.
- Group a structured result for review.

The MVP action engine must not allow model output to call a platform side effect directly.

## Confirmation policy

MVP value:

- `AlwaysReview`

Future candidates:

- `ReviewWhenUncertain`
- `ConfirmFromNotification`
- `AutomaticallyPerform`

Future policies require separate product, permission, risk, and audit design.

## Proposal

A Proposal is a structured, local, user-reviewable suggestion created by a validated Automation result.

Product fields:

- Identifier.
- Proposal type.
- Originating Mode identifier and display name.
- Originating Automation identifier and display name.
- Source application identifier and display name.
- Structured editable fields.
- Explanation.
- Validation and ambiguity markers.
- Lifecycle state.
- Created and expiry time.

Raw notification content is not a Proposal field.

## Activity record

An Activity record explains what NotiMate did without reconstructing sensitive notification content.

Examples:

- `Calendar proposal created`
- `Proposal dismissed`
- `Opened in Calendar`
- `Ignored by Reduce promotions`
- `Work Mode activated`
- `Notification Access lost`

Activity is local-only in the MVP.

## Optional account

An Account is not the owner of core local data. It provides optional backup or future account services.

Signing out:

- Stops future sync.
- Keeps local Modes and automations.
- Does not imply cloud-account deletion.

Deleting cloud account data and clearing local data are separate explicit operations.

## Resolved product decisions

| Decision | Resolution |
|---|---|
| Is login required? | No. Core product is local-first and usable without an account. |
| Where is login shown? | Contextually for backup/sync, preferably after setup or in Settings. |
| What groups automations? | A user-facing Mode. |
| How many Modes are active? | Exactly one in the MVP. |
| Is paused another Mode? | No. Pause is a global processing state. |
| Where are Modes managed? | Under Automations, not Account/Profile. |
| Is chat the home screen? | No. Today is proposal-first. |
| What is the first LLM UI? | Optional natural-language automation drafting. |
| Can NotiMate read full WhatsApp history? | No. Only notification content/context supplied through Android. |
| Can a native notification edit calendar fields? | Not adequately. It opens Proposal Detail in NotiMate. |
| What are notification actions? | Review and Dismiss; body tap also reviews. |
| Does swiping reject a proposal? | No. It dismisses the alert only. |
| How is a calendar event saved? | Through Android Calendar's pre-filled insert UI. |
| Is calendar permission required? | Not for the MVP insert-intent flow. |
| Is `POST_NOTIFICATIONS` required? | On Android 13+ for NotiMate proposal alerts, not for reading source notifications. |
| What if proposal alerts are denied? | Processing continues; proposals appear in Today. |
| Are external writes automatic? | No. User confirmation is required in the MVP. |
| Is activity synced? | No. Activity remains local. |

## Design-stage assumptions

These assumptions allow visual design to continue:

- Android is the complete MVP experience.
- iOS may reuse brand and shared components but cannot provide Android-equivalent cross-app notification listening.
- Personal Mode is the default starter Mode.
- Calendar proposal is the hero end-to-end journey.
- Today, Automations, and Activity are primary navigation destinations.
- Settings is nested from the app bar.
- English and Hebrew/RTL layouts must be supported.

## Questions that do not block design-system work

- Final list of post-calendar action types.
- Whether optional configuration sync ships in the first public MVP.
- Commercial plan and subscription packaging.
- Automatic Mode switching after the MVP.
- Client-side encryption and recovery design for sensitive synced conditions.
- Exact local AI device support at release time.

These questions may affect later feature screens, but they do not change the foundational navigation, trust model, Mode model, or component system.
