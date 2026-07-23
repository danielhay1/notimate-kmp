# NotiMate Core User Flows

Status: Canonical MVP behavior

## Flow 1: First-run setup

1. User opens NotiMate.
2. Welcome explains the outcome.
3. Privacy screen explains on-device processing and limitations.
4. NotiMate explains Notification Access.
5. User chooses `Open Android settings`.
6. Android displays Notification Listener settings.
7. User grants or declines access.
8. User returns to NotiMate.
9. NotiMate verifies the actual access state.
10. If granted, user selects monitored applications.
11. NotiMate explains proposal alerts.
12. On Android 13+, NotiMate requests `POST_NOTIFICATIONS`.
13. NotiMate checks local AI readiness.
14. User starts with or customizes Personal Mode.
15. Setup completes and opens Today.

Alternate outcomes:

- Notification Access declined: open inactive Today with a clear setup reminder.
- Proposal alerts declined: continue setup; proposals appear only inside Today.
- Local AI unsupported: continue with supported deterministic capabilities.
- User leaves Android settings without a decision: show `Access not enabled` without blame.

## Flow 2: Calendar proposal with proposal notifications enabled

1. A monitored source posts a notification.
2. Notification Listener receives the posted notification.
3. NotiMate minimizes and analyzes available content locally.
4. A possible calendar event is detected.
5. Output is validated into a strict proposal schema.
6. NotiMate creates a local structured proposal.
7. NotiMate posts a notification in the `Action proposals` channel.
8. Notification title is useful but lock-screen-safe.
9. User taps the body or `Review`.
10. A direct activity `PendingIntent` opens Proposal Detail in NotiMate.
11. User reviews and edits title, date, time, and location.
12. User chooses `Continue to Calendar`.
13. NotiMate launches Android Calendar using `ACTION_INSERT`.
14. Calendar displays a pre-filled editor.
15. User saves or cancels in Calendar.
16. NotiMate records only the safe handoff outcome it can verify.

Product constraint:

NotiMate must not claim the event was saved unless the result can be verified. `Opened in Calendar` is safer than `Event added` when Android does not return a definitive result.

## Flow 3: Notification actions

Notification behavior:

- Body tap: open Proposal Detail.
- `Review`: open Proposal Detail.
- `Dismiss`: mark proposal dismissed and remove the notification.
- Swipe away: remove the alert while keeping the proposal available in Today.

Sensitive proposal actions should require device authentication where appropriate.

Do not use a service or broadcast receiver as a notification trampoline to open Proposal Detail. The notification action must target the visible activity directly through `PendingIntent`.

## Flow 4: Proposal alerts denied or disabled

1. NotiMate continues processing monitored notifications.
2. A proposal is created locally.
3. No system proposal notification is posted.
4. Today shows the pending proposal.
5. Today displays a non-blocking banner: `Proposal alerts are off`.
6. Banner action opens NotiMate's notification settings guidance.
7. User may enable alerts through Android settings.

Processing and proposal alerts are separate capabilities and must be represented separately.

## Flow 5: Ambiguous calendar proposal

1. NotiMate detects an event but one or more required fields are uncertain.
2. Proposal state becomes `Needs review`.
3. The proposal notification uses cautious language.
4. Proposal Detail highlights missing or uncertain fields.
5. User corrects the fields or dismisses the proposal.
6. `Continue to Calendar` stays disabled until required fields are valid.

Example language:

- `A date was found, but the time needs confirmation.`
- `NotiMate may have misunderstood this notification.`

Do not expose raw model probability values.

## Flow 6: Switch active Mode

1. User taps the active Mode chip from Today or Automations.
2. A Mode switcher sheet lists available Modes.
3. User selects another Mode.
4. NotiMate updates the active Mode immediately.
5. A short confirmation states that the new Mode applies to future notifications.
6. Existing proposals retain their original Mode and Automation labels.

No destructive confirmation dialog is needed for a normal Mode switch.

## Flow 7: Manage Modes

1. User opens Automations.
2. User chooses `Manage modes`.
3. Manage Modes shows the active Mode and all alternatives.
4. User can create, duplicate, rename, reorder, or open a Mode.
5. Deleting a non-active Mode requires confirmation.
6. Deleting the active Mode requires selecting a replacement first.
7. The only remaining Mode cannot be deleted.

## Flow 8: Create an automation with the structured builder

1. User opens Automations in the intended Mode.
2. User selects `Create automation`.
3. User chooses one or more notification sources.
4. User defines conditions and exclusions.
5. User selects an action.
6. User selects a confirmation policy.
7. User names the automation.
8. Review screen shows a readable summary.
9. User activates the automation.
10. Automation appears in the Mode.

For the MVP, external actions use `Always review`.

## Flow 9: Describe an automation with local AI

1. User selects `Describe an automation`.
2. NotiMate confirms local AI availability.
3. User enters a natural-language request.
4. Local AI converts the request into a structured automation draft.
5. Schema validation rejects unsupported or unsafe output.
6. NotiMate presents the normal structured review screen.
7. User edits and activates or discards the draft.

The natural-language instruction never bypasses structured review.

Fallback:

- If local AI is unavailable, offer `Build step by step`.
- Preserve the user's text only as long as needed for the local operation unless the user explicitly saves it as a note.

## Flow 10: Optional sign-in and backup

1. User opens Settings > Backup and sync.
2. NotiMate explains why an account is needed.
3. NotiMate lists what will sync and what remains local.
4. User selects `Continue with Google` or `Not now`.
5. After successful sign-in, backup remains off until the user explicitly enables it.
6. NotiMate performs the first configuration backup.
7. Settings shows last successful sync and errors.

Signing out:

- Stops future sync.
- Does not delete local Modes or automations.
- Offers a separate account-data deletion action.

## Flow 11: Permission revoked

1. Android Notification Access is revoked outside NotiMate.
2. NotiMate detects the state when opened or when the listener disconnects.
3. Processing state becomes `Permission required`.
4. Today explains that no new notifications can be analyzed.
5. Existing privacy-safe proposals and configuration remain available.
6. User can reopen Android settings.

If proposal notification permission is revoked instead:

- Processing remains active.
- Today shows `Proposal alerts are off`.
- Permission status must not be mislabeled as processing disabled.

## Flow 12: Pause and resume

1. User pauses NotiMate from Today or Settings.
2. New notifications are not analyzed.
3. Active Mode remains selected.
4. Existing proposals remain accessible.
5. Today shows `Processing paused`.
6. User resumes without reselecting the Mode.

Pause is global and is not a Mode.
