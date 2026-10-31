# Android OTP clipboard support

The OTP foundation parses local-model output, validates exact source membership, and supplies an
Android clipboard adapter. Production processing remains disabled; this is not a working automatic
OTP feature until the integration checklist in task 008 is complete.

| Capability | Limit |
| --- | --- |
| Notification access | User grants special notification access; only posted notification content is visible. |
| Android 15+ OTPs | Android can redact codes for untrusted listeners, regardless of target SDK. Hidden content cannot be recovered. |
| Restricted permission | RECEIVE_SENSITIVE_NOTIFICATIONS is restricted; manifest declaration is not an ordinary runtime grant. |
| Clipboard write | Normal API write has no runtime permission prompt or success receipt; device policy may deny it. |
| Clipboard preview | EXTRA_IS_SENSITIVE masks the Android 13+ preview; it does not encrypt the code. |
| Older devices | Current resource probe lacks thermal readings below API 29, so local heavy inference fails closed. |

No SMS inbox permission, accessibility service, overlay, default-keyboard role, foreground service,
network permission, or developer security bypass is introduced. A foreground service cannot recover
redacted notifications. The current manifest declares a system-bound notification listener; access
must still be granted by the user. The debug-only posting permission serves the test notification UI.

Model-generated codes must match the only distinct qualifying whole token in received title/body.
The clipboard adapter accepts a code only within 60 seconds of posting while the device is unlocked
and interactive. Callers must recheck opt-in, pause, source, Profile, and access immediately before
invocation and prevent duplicate/update races. OTP values stay in transient memory and the system
clipboard; only content-free identifiers reach Activity. The system clipboard may be accessible to
the receiving application or system features; masking the preview is not confidentiality protection.

Audit completion is protected from caller cancellation after a write begins, with a five-second
audit timeout. Storage failure or process death can leave an unaudited write. Never automatically
retry a clipboard side effect after an audit failure or cancellation.

Runtime OTP visibility, actual model accuracy/latency, OEM behavior, and clipboard writes have not
been qualified on physical target devices in this review. Host tests use synthetic data and fakes;
Robolectric clipboard tests are not proof of Android 15 OTP visibility.

## Official sources

- [Android 15 OTP redaction](https://developer.android.com/about/versions/15/behavior-changes-all#otp-redaction)
- [Restricted sensitive notification permission](https://developer.android.com/reference/android/Manifest.permission#RECEIVE_SENSITIVE_NOTIFICATIONS)
- [Sensitive clipboard content](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#SensitiveContent)
- [Android 10 clipboard read restrictions](https://developer.android.com/about/versions/10/privacy/changes#clipboard-data)
- [Android clipboard service write policy](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/clipboard/ClipboardService.java)
