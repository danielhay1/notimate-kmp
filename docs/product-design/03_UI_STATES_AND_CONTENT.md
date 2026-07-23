# NotiMate UI States and Content

Status: Required state coverage before wireframing

## Product voice

NotiMate should sound:

- Calm.
- Direct.
- Privacy-aware.
- Helpful without pretending certainty.
- Nonjudgmental when access is denied or a model is unavailable.

Prefer:

- `Possible calendar event`
- `Time needs confirmation`
- `Processed on this device`
- `Open Android settings`
- `Continue to Calendar`
- `NotiMate may have misunderstood this notification`

Avoid:

- `AI knows`
- `Event created` before confirmed
- `Failed because you denied permission`
- `Grant all permissions`
- `Reading your WhatsApp history`
- Raw probability values
- Overly human claims such as `I read your conversation`

## Global processing states

### Active

Meaning: Notification Access is granted and processing is enabled.

Primary message: `NotiMate is active`
Supporting message: `Using Personal Mode`

### Paused

Meaning: User intentionally paused all processing.

Primary message: `Processing is paused`
Action: `Resume`

### Permission required

Meaning: Notification Access is missing or revoked.

Primary message: `Notification Access is required`
Supporting message: `NotiMate cannot analyze new notifications until access is enabled.`
Action: `Open Android settings`

### Degraded

Meaning: Core processing works but a capability such as local generative AI is unavailable.

Primary message: `Some smart features are unavailable`
Supporting message must name the affected capability and available fallback.

### Temporarily unavailable

Meaning: Processing cannot currently complete because of device pressure, a local model state, or another recoverable condition.

Actions: `Try again` or `Review later`, depending on the operation.

## Permission states

### Notification Access

- Not requested.
- Education viewed.
- Granted.
- Declined or left unchanged.
- Revoked.
- Android settings unavailable.

### Proposal notifications

- Not requested.
- Granted.
- Denied.
- Channel disabled.
- Application notifications disabled.

The UI should distinguish runtime permission denial from a disabled notification channel when it can.

### Calendar

The MVP has no NotiMate calendar permission state.

Calendar handoff states:

- Compatible calendar application found.
- No compatible calendar application found.
- Calendar editor opened.
- Save result unknown.

Use `Opened in Calendar`, not `Added to Calendar`, unless completion is verified.

## Local AI states

### Ready

`On-device AI is ready`

### Download required

Explain approximate download behavior if known at runtime. Do not invent a size.

Actions:

- `Download`
- `Use basic processing`

### Downloading

Show progress only if the platform reports meaningful progress.

### Unsupported

`This device does not support the optional on-device model. You can still use Modes and supported automations.`

### Temporarily unavailable

`On-device AI is temporarily unavailable. Try again or build the automation step by step.`

### Device constrained

`Smart processing is paused while the device is under pressure. The proposal will be reviewed later without uploading its content.`

Do not durably queue raw unstructured notification text merely to retry model processing.

## Proposal lifecycle

Canonical states:

- `Draft`
- `NeedsReview`
- `Ready`
- `Dismissed`
- `HandedOff`
- `Deferred`
- `Failed`
- `Expired`

### Draft

Structured extraction exists but validation is incomplete.

### NeedsReview

Required information is uncertain or missing.

### Ready

Required fields are valid, but user confirmation is still required.

### Dismissed

User explicitly rejected the proposal.

### HandedOff

NotiMate opened the external system UI. This does not necessarily mean the external action completed.

### Deferred

Processing will be retried without persisting prohibited raw content.

### Failed

The proposal could not be completed. The UI must provide a safe next action where possible.

### Expired

Proposal is no longer useful because its time window passed.

## Proposal notification content

Default:

Title: `Possible calendar event`
Body: `Tuesday at 18:00. Review before adding.`
Actions: `Review`, `Dismiss`

Lock-screen-safe version:

Title: `NotiMate found a possible action`
Body: `Unlock to review.`
Actions should require authentication when they expose or change sensitive data.

Notification body tap and `Review` open the same Proposal Detail destination.

## Today empty states

### Active with no proposals

Title: `You're all caught up`
Body: `NotiMate is watching selected apps using Personal Mode.`

### New user before any observed notification

Title: `Waiting for a useful notification`
Body: `When NotiMate finds something actionable, it will appear here.`

### Proposal alerts disabled

Banner: `Proposal alerts are off`
Body: `New proposals will still appear here.`
Action: `Review settings`

### Notification Access missing

Title: `Setup is incomplete`
Body: `Enable Notification Access so NotiMate can analyze new notifications on this device.`
Action: `Open Android settings`

## Mode states

- Active.
- Inactive.
- Empty.
- Contains disabled automations only.
- Cannot delete because it is the only Mode.

Mode switch confirmation:

`Work Mode is now active for new notifications.`

Empty Mode:

Title: `No automations yet`
Body: `Add an automation or start from a recommended template.`
Actions: `Create automation`, `Browse templates`

## Automation states

- Enabled.
- Disabled.
- Draft.
- Unsupported on this device.
- Needs attention because a permission or capability changed.

Automation list rows must show:

- Name.
- Plain-language purpose.
- Enabled state.
- Attention indicator when required.

Do not require opening an automation merely to understand what it does.

## Account and sync states

- Signed out.
- Sign-in in progress.
- Signed in, sync off.
- Signed in, sync on.
- Syncing.
- Sync error.
- Conflict requiring resolution.

Signed-out message:

`NotiMate works without an account. Sign in only if you want to back up supported settings.`

Sync disclosure:

`Notification content, proposals, and activity history remain on this device.`

## Error-writing pattern

Every error should answer:

1. What happened?
2. What remains safe or available?
3. What can the user do next?

Example:

> On-device AI is unavailable right now. Your notification content was not uploaded. Try again later or build this automation step by step.

## Accessibility and localization requirements

- Do not rely on color alone for state.
- Support system font scaling without clipped actions.
- Keep touch targets appropriate for mobile use.
- Use explicit action labels instead of ambiguous icons.
- Support right-to-left layout and Hebrew content.
- Keep dates, times, numerals, and weekday names locale-aware.
- Ensure sensitive information is not announced from a locked device.
- Use standard Android notification templates for compatibility.
