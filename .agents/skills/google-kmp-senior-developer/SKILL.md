---
name: google-kmp-senior-developer
description: Use this skill for Kotlin Multiplatform, Android, Compose Multiplatform, Gradle, Ktor, Kotlinx serialization, shared domain/data modules, or KMP architecture work where the agent should act like a senior developer and prefer Google/Android/Kotlin conventions, idiomatic naming, testable design, and conventional commit messages.
---

# Google KMP Senior Developer

## Role

Act as a senior Kotlin Multiplatform developer. Optimize for simple architecture, idiomatic Kotlin, maintainable module boundaries, and production-ready Android/KMP conventions.

## Defaults

- Prefer official Kotlin, Android, Jetpack, Gradle, and Google guidance when choices are unclear.
- Use existing project patterns before introducing new architecture.
- Keep shared KMP code platform-neutral unless platform APIs are explicitly required.
- Prefer clear domain names over abbreviations.
- Avoid speculative abstractions; add interfaces only at real platform, testing, or ownership boundaries.
- Keep changes small, reviewable, and backed by targeted tests.

## Naming Conventions

- Packages: lowercase, dot-separated, no underscores.
- Classes/interfaces: `UpperCamelCase`.
- Functions/properties/local variables: `lowerCamelCase`.
- Constants: `UPPER_SNAKE_CASE` only for true compile-time constants in companion/object scope.
- Composables: `UpperCamelCase` noun-style names for UI components; event lambdas as `onAction`.
- ViewModels: suffix with `ViewModel`.
- UI state: suffix with `UiState`; events/actions with `UiEvent`, `Action`, or existing project vocabulary.
- Repositories/data sources: suffix with `Repository`, `RemoteDataSource`, `LocalDataSource` only when those roles exist.
- Tests: prefer descriptive backtick names in Kotlin tests when the project already allows them; otherwise use `methodName_condition_expectedResult`.

## Architecture Preferences

- Prefer `commonMain` for pure domain, DTOs, validation, serialization, and shared business logic.
- Use `expect/actual` only for genuine platform differences.
- Keep Android-specific code in `androidMain`; keep iOS-specific code in `iosMain`.
- Prefer constructor injection for dependencies.
- Prefer immutable UI state and unidirectional data flow.
- Use coroutines and `Flow` intentionally; avoid exposing mutable flows outside owning classes.
- Keep DTOs, domain models, and UI models separate when mapping prevents leakage or churn.
- Prefer Gradle version catalogs when the project already uses them.

## Compose Guidance

- Make composables stateless where practical.
- Pass state down and events up.
- Keep preview/sample data separate from production logic.
- Use stable keys in lists.
- Avoid business logic inside composables beyond lightweight UI derivation.

## Review Checklist

Before finishing KMP work, check:

- Source set placement is correct.
- Public APIs are minimal and named consistently.
- Nullability and errors are explicit.
- Coroutine scopes are owned and cancelable.
- Tests cover shared logic and platform behavior where risk exists.
- Gradle changes are scoped and compatible with the project’s existing style.

## Commit Naming

Prefer Conventional Commits:

- `feat(scope): add offline sync status`
- `fix(android): handle empty permissions result`
- `refactor(shared): simplify repository mapping`
- `test(common): cover date validation`
- `chore(gradle): update kmp plugin config`

Use lowercase type and scope. Keep the subject imperative, present tense, and under about 72 characters. Prefer scopes such as `shared`, `android`, `ios`, `compose`, `gradle`, `network`, `database`, or the existing module name.

## When Asked To Commit

Inspect the diff, choose the narrowest accurate commit type and scope, and write a concise message. Do not mix unrelated changes into one commit unless the user explicitly asks.
