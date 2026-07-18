# Repository Guidelines

## Project Structure & Module Organization

NotiMate is a Kotlin Multiplatform project initialized from the Compose Multiplatform scaffold.

- `:shared`: shared Compose UI, common domain logic, platform abstractions, and KMP tests.
- `:androidApp`: Android application shell, manifest, launcher resources, and Android entry point.
- `iosApp/`: Xcode iOS application that hosts the shared Compose UI through the generated framework.
- `gradle/`: version catalog, Gradle wrapper, and daemon JVM configuration.
- `specs/`: spec-driven development documents for planned product work.

Keep notification/domain/extraction logic in shared code when it is platform-neutral. Keep notification listener, WorkManager, Room, Calendar Provider, Android permissions, SwiftUI/UIKit hosting, and iOS-specific integrations at platform edges.

## Build, Test, and Development Commands

- `./gradlew :androidApp:assembleDebug` builds the Android debug APK.
- `./gradlew :shared:allTests` runs shared multiplatform tests when all targets are configured.
- `./gradlew :shared:check` runs checks for the shared module.
- `./gradlew clean` removes generated build output.
- Open `iosApp/iosApp.xcodeproj` in Xcode for iOS app work.

Use Android Studio's bundled JDK or the Gradle toolchain defined in `gradle/gradle-daemon-jvm.properties`.

## Coding Style & Naming Conventions

Follow official Kotlin style, Google Android conventions, and existing Compose Multiplatform scaffold patterns.

- Packages: lowercase dot-separated names.
- Classes and composables: `UpperCamelCase`.
- Functions, properties, and local variables: `lowerCamelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- ViewModels: `SomethingViewModel`.
- UI state/actions/effects: `SomethingUiState`, `SomethingAction`, `SomethingEffect`.

Prefer immutable state, constructor injection, explicit nullability, and unidirectional data flow. Do not expose mutable flows outside their owner.

## Testing Guidelines

Place platform-neutral tests in `shared/src/commonTest`. Use Android host tests for Android-specific shared behavior and iOS tests for iOS-specific shared behavior. Use synthetic notification payloads only; never commit real notification text, MFA codes, financial balances, or message content.

## Commit & Pull Request Guidelines

Use Conventional Commits:

- `chore(kmp): initialize NotiMate app scaffold`
- `feat(shared): add notification classification models`
- `fix(android): handle notification access revocation`
- `test(common): cover privacy redaction rules`

Pull requests should include a summary, validation commands, linked task/spec, and screenshots or recordings for UI changes.

## Spec-Driven Development

Before implementing features, read `.specify/memory/constitution.md` and the relevant `specs/<number>-<feature>/` files. Keep `spec.md`, `plan.md`, and `tasks.md` aligned as decisions change.

## Security & Privacy

Raw notification payloads must stay on device. Do not log, persist, upload, or include sensitive notification bodies in tests, screenshots, analytics, or crash reports.

## Notification Agent Architecture

Treat the NotiMate agent as a local policy engine for received notifications. Prefer this flow:

1. Minimize OS notification data at the Android boundary.
2. Classify and extract on device.
3. Validate model/rule output against strict schemas.
4. Produce ignored, draft, review-needed, deferred, or failed states.
5. Require user confirmation before external provider writes in the MVP.

Do not implement cloud-first processing, raw payload upload, or direct model-to-side-effect execution.
