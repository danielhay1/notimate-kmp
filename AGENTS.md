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

Prefer modern Kotlin, Compose Multiplatform, and AndroidX APIs for UI, concurrency, lifecycle, and state. Use MVVM as the default feature shape: ViewModels or shared state holders expose immutable UI state and receive actions from composables. Prefer constructor injection and dependency-injection-friendly structure, with interfaces at module boundaries and implementations in platform-specific or data packages. Do not expose mutable flows outside their owner.

Evaluate persistence per data type before choosing storage. Use modern KMP/CMP-friendly storage where possible; choose Room for relational/queryable durable data, DataStore or platform settings for simple preferences, and encrypted platform storage for secrets or sensitive tokens. Keep raw notification bodies out of durable storage.

## Testing Guidelines

Place platform-neutral tests in `shared/src/commonTest`. Use Android host tests for Android-specific behavior and iOS tests only for iOS-specific shared behavior. Mirror implementation package paths in tests and use the same filename suffix, for example `shared/src/commonMain/domain/AgentPolicy.kt` -> `shared/src/commonTest/domain/AgentPolicyTest.kt`.

Plan code to be testable through pure logic, injected dependencies, and deterministic state transitions. Do not test UI in this project. Test business logic, policy decisions, mappers, validation, and privacy behavior; avoid over-testing library behavior or Compose/Android framework functions. Use synthetic notification payloads only; never commit real notification text, MFA codes, financial balances, or message content.

## Commit & Pull Request Guidelines

Before implementation work, provide a short Git plan for the feature or bug fix and wait for user approval. After approval, provide a short high-level branch/merge diagram plus the exact command list before changing code.

Use isolated branches for implementation work:

```text
main
  dev
    feature/<short-kebab-name>
    bug/<short-kebab-name>
```

Default branch flow:

1. Determine whether the work is a feature or bug and propose `feature/<name>` or `bug/<name>`.
2. Create and check out the new branch before implementation starts.
3. Validate that all work happens on the intended branch and remains isolated from unrelated changes.
4. After validation, open a pull request into the correct base branch and give the user the PR link for review.
5. Merge only after user approval.

Use Conventional Commits:

- `chore(kmp): initialize NotiMate app scaffold`
- `feat(shared): add notification classification models`
- `fix(android): handle notification access revocation`
- `test(common): cover privacy redaction rules`

Pull requests should include a summary, validation commands, linked task/spec, and screenshots or recordings for UI changes.

## Spec-Driven Development

Before implementing features, read `.specify/memory/constitution.md` and the relevant `specs/<number>-<feature>/` files. Keep `spec.md`, `plan.md`, and `tasks.md` aligned as decisions change.

For long-running or high-effort work, use workflows or multiple agents to split research, implementation, review, and validation. If the scope is unclear, ask the user whether to use a workflow before starting.

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
