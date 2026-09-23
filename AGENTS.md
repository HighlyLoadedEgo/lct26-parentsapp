# Agent instructions

## Current verification preference — 2026-09-23

The user runs the application personally. Check builds only; do not start the
app, launch emulators, or run connected/instrumented tests unless the user
explicitly changes this preference. Common checks are
`./gradlew :app:testDebugUnitTest :feature:home:testDebugUnitTest :app:assembleDebug :app:lintDebug`.

## Project

ParentsApp is a native Android app written in Kotlin with Jetpack Compose.
Application code lives in `app/src/main/java/ru/nksk/parentsapp/`. The project
is currently a bare stub: one feature module with a placeholder screen.

Read [the architecture guide](docs/architecture.md) before adding or moving app
code. Keep app composition in `app`, each feature in `feature/<name>`, and
shared presentation in the `core/ui` packages inside `app`. Features do not
import each other or app wiring; screen UI receives state and callbacks instead
of navigation objects.

## Tech stack

- Keep Kotlin, Gradle Kotlin DSL, and Jetpack Compose with Material 3. Manage
  dependency versions in [the version catalog](gradle/libs.versions.toml).
- Use reinforced MVVM: unidirectional data flow, immutable `UiState` exposed as
  `StateFlow`, explicit UI actions, and lifecycle-aware collection at screen
  entries. Screens render state and emit callbacks; ViewModels coordinate domain
  operations; domain code owns business rules, and repositories own data access.
  Use coroutines and Flow for asynchronous work.
- Use Hilt for dependency injection. Hilt is integrated; extend the existing
  graph using constructor injection and keep bindings in `app/di` once the graph
  appears. Obtain injected ViewModels at feature navigation entries; screens
  receive state and callbacks, and domain models remain free of DI annotations.
- Add Room, Preferences DataStore, Retrofit + OkHttp + Kotlin serialization,
  and WorkManager when a working feature needs them; do not scaffold unused
  layers or dependencies for placeholders.

## Working conventions

- Preserve existing uncommitted work and limit changes to the requested scope.
- Record unresolved design decisions rather than silently guessing; do not
  invent product rules that were not requested.
- Validate behavior changes with relevant tests. Common checks are listed under
  the verification preference above.
- Documentation-only changes require checking local links and consistency;
  they do not require an Android build.
