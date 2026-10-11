# Feature Keys And Manual Notifications

`shared/src/commonMain/composeResources/files/feature-keys.json` owns the complete
ordered list of feature keys and their defaults. Each entry has exactly `key`,
`type`, and `default`. Supported types are `boolean`, `int`, `float`, `double`, and
`string`; numeric defaults must be finite and within the corresponding Kotlin range.
Names use camelCase letters and digits. Duplicate names or invalid defaults block
startup with Retry instead of starting with an incomplete configuration.

## Startup And Overrides

`FeatureKeyManager` is owned by the platform app shell and passed to consumers.
Its read-only `keys` flow contains the effective values. Boolean lookups fail closed
before initialization or for an unknown key; numeric/string lookups require a known
key of the matching type.

Release startup loads only bundled defaults and opens the normal app. Android
debug builds launch `FeatureKeysActivity`; iOS passes its Swift `#if DEBUG` build
configuration to the shared startup host. Both show the shared scrollable editor,
with a switch for booleans and text/numeric fields for other types. The list shows
each key, default, and editable effective value. Changes stay in a draft until
Continue validates and saves them, updates the manager, and opens the normal app.
Failed saves keep the editor open for retry.

Overrides use a separate Android private preference snapshot and iOS app-owned
UserDefaults entry. They survive process restarts and app updates, until changed,
app data is cleared, or the app is uninstalled. Android backup remains disabled.
The bundled JSON is never edited. Values equal to the default remove that override,
so future default changes apply unless there is an explicit different override.
Removed keys, changed types, invalid values, and corrupt snapshots fall back to
defaults, with a notice in the editor. Unreadable storage blocks startup and does
not reset existing data. Release never reads or writes debug overrides.

Add a key to the JSON, then gate its feature using the injected manager:

```kotlin
if (featureKeys.isEnabled(FeatureKeys.NOTIFICATION_TESTING_ENABLED)) {
    // Offer the manual notification testing destination.
}
```

Add named constants to `FeatureKeys` when a feature needs a stable lookup name;
the JSON remains the sole source of its type and default. Use `intValue`,
`floatValue`, `doubleValue`, or `stringValue` for other types. Keys are local feature
configuration, never credentials or an authorization boundary.

## Manual Android Notifications

`notificationTestingEnabled` defaults to `false`. Enable it in the debug editor
and Continue; the normal app then offers **Test a local notification**. The screen
accepts a nonblank title (up to 256 characters) and body (up to 4096 characters).
Use synthetic content only. Input stays in memory; it is not saved to preferences,
logs, or app history. Sending hands the content to the Android notification system.

Send requests `POST_NOTIFICATIONS` on Android 13+ when necessary. After allowing
permission, tap Send again. Denial or a blocked app/channel produces a recoverable
message and a notification-settings action. Notifications use a dedicated channel,
private visibility, a content-free lock-screen preview, and a tap action that opens
the normal app. Each send replaces the previous test notification.

The sender rechecks the feature key at posting time. Its Activity, native sender,
resources, launcher editor Activity, and notification permission are in Android's
debug source set; release builds have the normal launcher and no test sender.
The shared feature editor works on iOS, but native notification testing is Android
only in this slice.

NotiMate deliberately ignores its own package in the notification listener. These
notifications test OS delivery, not cross-app capture or extraction. Production
capture remains fail closed; this tooling does not activate the extraction pipeline.

## Validation

Shared tests cover typed parsing, overrides, release isolation, schema evolution,
invalid edits, storage failures, and startup state. Android host tests cover
preference reload and notification gating, permission, blocked channels, and private
previews. No UI tests are added. Manual simulator checks verify the startup/editor,
Continue, process restart persistence, and notification delivery.
