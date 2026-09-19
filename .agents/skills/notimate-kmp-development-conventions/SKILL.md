---
name: notimate-kmp-development-conventions
description: Apply NotiMate repository-specific KMP structure, naming, architecture, persistence, testing, and validation conventions during implementation or refactoring. Use with the senior KMP skill; do not use as a substitute for feature specifications or specialized domain skills.
---

# NotiMate KMP Development Conventions

Use this skill with `$google-kmp-senior-developer`. Follow existing project
patterns before introducing new architecture, and add a more specialized KMP skill
when the change materially concerns its domain.

## Kotlin style baseline

Follow the Google Android Kotlin style guide and official Kotlin coding conventions
unless an existing repository formatter or target-specific KMP constraint is more
specific. Do not reformat unrelated code.

- Use UTF-8, four-space indentation, and no tab indentation or semicolons.
- Keep code at or below 100 columns except for documented guide exceptions such as
  package/import lines, long URLs, and shell commands in comments.
- Do not use wildcard imports. Keep imports in one ASCII-sorted group.
- Give a file containing one public type that type's exact name. Otherwise use a
  descriptive PascalCase filename focused on one theme.
- Use source-set suffixes such as `.android.kt` and `.ios.kt` for platform files
  with top-level declarations when needed to prevent JVM facade collisions.
- Order declarations logically for top-to-bottom reading; do not order them by
  chronology, alphabet, or visibility alone.

## Project structure

- `:shared`: shared Compose UI, platform-neutral domain logic, platform
  abstractions, and KMP tests.
- `:androidApp`: Android application shell, manifest, launcher resources, and
  Android entry point.
- `iosApp/`: Xcode iOS application hosting the generated shared framework.
- `gradle/`: version catalog, wrapper, and daemon JVM configuration.
- `specs/`: approved feature requirements and implementation plans.

Keep notification, domain, validation, and extraction logic in shared code when it
is platform-neutral. Keep notification listeners, WorkManager, Room, Calendar
Provider, Android permissions, SwiftUI/UIKit hosting, and other native APIs at
platform edges. Use `expect` and `actual` only for genuine platform differences.

## Architecture and naming

- Packages: lowercase dot-separated names.
- Classes and interfaces: `UpperCamelCase`.
- Functions, properties, and local variables: `lowerCamelCase`.
- Top-level or `object` constants that are deeply immutable and side-effect-free:
  `UPPER_SNAKE_CASE`; other read-only values: `lowerCamelCase`.
- ViewModels: `SomethingViewModel`.
- UI state/actions/effects: `SomethingUiState`, `SomethingAction`, and
  `SomethingEffect`.
- Unit-returning composables: PascalCase noun phrases.

Use immutable state and unidirectional data flow. State holders expose immutable
state and receive actions from composables. Prefer constructor injection and do
not expose mutable flows outside their owner. Keep DTO, domain, persistence, and
UI models separate when mapping prevents boundary leakage or churn.

Evaluate persistence per data type. Prefer Room for relational/queryable durable
data, DataStore or platform settings for simple preferences, and encrypted
platform storage for secrets or sensitive tokens. Never persist raw notification
bodies.

## Testing

Place platform-neutral tests in `shared/src/commonTest`. Use Android host tests for
Android-specific behavior and iOS tests only for iOS-specific shared behavior.
Mirror implementation package paths and use the implementation filename plus the
`Test` suffix.

Name test functions with portable camelCase or underscore-separated names. Do not
use spaces in backtick-quoted test names because they are not supported consistently
across Android targets.

Focus tests on business logic, policy decisions, mappers, validation, state
transitions, and privacy behavior. Do not test Compose, Android, or third-party
framework internals; test project-owned behavior at those boundaries when risk
justifies it. Use deterministic dependencies and synthetic notification data only.
Do not over-test.

## Validation

Use the smallest commands that materially verify the change:

- `./gradlew :shared:allTests` for shared multiplatform tests.
- `./gradlew :shared:check` for shared checks.
- `./gradlew :androidApp:assembleDebug` for the Android debug APK.
- Open `iosApp/iosApp.xcodeproj` in Xcode for iOS-specific work.

Use Android Studio's bundled JDK or the configured Gradle toolchain. Report the
exact checks executed and their observed outcomes. Do not claim runtime, build, or
test evidence that was not observed.
