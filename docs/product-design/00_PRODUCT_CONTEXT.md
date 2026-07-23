# NotiMate Product Context

Status: Approved foundation for screen and design work
Platform: Android-first Kotlin Multiplatform application
Product posture: Local-first, privacy-first, user-controlled automation

## Product promise

NotiMate turns useful information found in Android notifications into understandable, reviewable action proposals without uploading raw notification content.

The product should feel like a quiet, reliable assistant:

- It works in the background.
- It interrupts only when an action may be useful.
- It explains what it found.
- It lets the user review and edit.
- It does not perform external writes silently in the MVP.

Primary promise:

> NotiMate finds useful actions in your notifications, explains its proposal, and keeps you in control.

## Core product loop

1. Android posts a notification from another application.
2. NotiMate receives the notification through user-granted Notification Access.
3. NotiMate minimizes and analyzes the available content on-device.
4. NotiMate either ignores the notification or creates a structured proposal.
5. When proposal alerts are permitted, NotiMate posts its own notification.
6. The notification opens the relevant NotiMate proposal screen.
7. The user reviews, edits, dismisses, or continues to an external system UI.
8. NotiMate retains only a privacy-safe structured activity result.

Swiping away a NotiMate notification dismisses the alert, not the underlying proposal. Explicit `Dismiss` rejects the proposal.

## Initial action capability

The first complete action journey is a calendar-event proposal:

- Detect a possible event from notification content.
- Extract a tentative title, date, time, and location.
- Notify the user when allowed.
- Open a specific Proposal Detail screen in NotiMate.
- Let the user review and edit extracted fields.
- Continue to Android's Calendar insert UI.
- Let the Calendar application perform the final save or cancellation.

The MVP uses Android's `ACTION_INSERT` calendar intent. It does not request direct calendar read/write permission.

## Notification and message context limitation

NotiMate cannot read an application's complete conversation history.

It can use:

- The currently posted notification.
- Updates to that notification.
- Active notifications available to the listener.
- Historic messaging context only when the source application includes it in the Android notification.

User-facing language must say:

> NotiMate analyzes notification content and any conversation context included by the source app.

It must never promise access to full WhatsApp, email, SMS, or other application history.

## Local AI product role

Local AI is valuable for:

- Classification.
- Event and entity extraction.
- Explaining why an action was proposed.
- Translating a natural-language instruction into a visible structured automation.

Local AI is not required for:

- Navigation.
- Viewing or editing Modes.
- Enabling or disabling automations.
- Reviewing a proposal.
- Dismissing a proposal.
- Opening Android's Calendar UI.

The MVP must degrade gracefully when on-device generative AI is unavailable, still downloading, rate-limited, too slow, or incompatible with the device.

The first conversational feature should be `Describe an automation`, which converts user language into an inspectable automation draft. An open-ended chatbot is not a primary navigation destination.

## Modes and automations

Every user has one or more NotiMate Modes.

A Mode is a named collection of automations. Examples:

- Personal
- Work
- Travel
- Quiet

Exactly one Mode is active in the MVP. Switching Modes affects newly received notifications only. Existing proposals keep the Mode that created them.

Each Automation contains:

- Sources: applications or notification categories.
- Conditions: what must match or be excluded.
- Action: what NotiMate proposes or changes.
- Confirmation policy: what user review is required.
- Enabled state.

The product should use `Mode`, `Automation`, `Condition`, `Action`, and `Proposal` in user-facing language. `Filter` and `rule set` are implementation terms and should not become separate user management concepts.

## Account and login posture

Core NotiMate features work without an account.

Login is optional and contextual. It may be offered for:

- Backing up non-sensitive configuration.
- Restoring Modes and automations.
- Future cross-device settings continuity.
- Future account or subscription management.

Login must not appear before the privacy explanation or gate Notification Access setup.

Recommended timing:

- After local setup is complete, as an optional `Back up your setup` step; or
- Later from Settings when the user enables backup and sync.

## Cloud data boundary

Allowed for optional sync after explicit opt-in:

- Mode names and appearance.
- Automation structure.
- Enabled states.
- General preferences.
- Selected application identifiers, following disclosure.

Local only:

- Raw notification content.
- Conversation context.
- Extracted message text.
- Proposal content.
- Activity history.
- Model prompts containing notification data.
- Authentication secrets.

Free-text matching values may reveal people, organizations, schedules, or private topics. Keep these local in the MVP unless client-side encryption and recovery behavior are designed and documented.

## MVP product principles

1. Trust before automation.
2. Explain before requesting sensitive access.
3. Ask permissions in context.
4. Never require cloud processing for the core loop.
5. Never imply access Android does not provide.
6. Keep external writes user-confirmed.
7. Prefer visible structured automation over hidden agent behavior.
8. Make paused, unavailable, and degraded states understandable.
9. Make every proposal traceable to a Mode and Automation.
10. Avoid notification fatigue through conservative proposal thresholds and user-controlled channels.

## Out of MVP

- Silent direct calendar writes.
- Full conversation-history access.
- Multiple simultaneously active Modes.
- Automatic Mode switching.
- A required account.
- Cloud notification analysis.
- General-purpose chatbot navigation.
- Location-triggered Modes.
- Direct model-to-side-effect execution.

## Android reference points

- Notification listener settings: https://developer.android.com/reference/android/provider/Settings#ACTION_NOTIFICATION_LISTENER_SETTINGS
- Notification runtime permission: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Notification actions: https://developer.android.com/develop/ui/compose/notifications
- Notification navigation: https://developer.android.com/develop/ui/views/notifications/navigation
- Notification trampoline restrictions: https://developer.android.com/about/versions/12/behavior-changes-12#notification-trampolines
- Notification channels: https://developer.android.com/develop/ui/compose/notifications/channels
- Calendar insert intents: https://developer.android.com/identity/providers/calendar-provider#intents
- On-device Prompt API: https://developers.google.com/ml-kit/genai/prompt/android
