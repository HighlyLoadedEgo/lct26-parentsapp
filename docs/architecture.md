# Architecture

ParentsApp is a native Android application written in Kotlin with Jetpack
Compose, Material 3, and Hilt. The project currently is a bare stub; this
document records the structure that new code must follow.

## Modules

- `:app` — the Android application. Owns composition (`app` package), the
  manifest, the launcher, app-owned wiring (`app/di` once a binding graph
  appears), and shared presentation in the `core/ui` packages (theme today,
  shared components as they appear).
- `:feature/<name>` — one Gradle module per feature (currently only
  `:feature:home`). A feature module contains its screens, its ViewModels, its
  navigation entry, and its own resources. Features never import each other and
  never import app wiring.

A `core/<name>` Gradle module (pure Kotlin domain, `java-library`) is added
when domain logic that multiple modules share actually appears; do not create
it preemptively.

## Presentation pattern

Reinforced MVVM with unidirectional data flow:

- Each screen has an immutable `UiState` data class and a sealed `Action`
  interface describing every user interaction.
- The ViewModel holds the state as a `MutableStateFlow` and exposes it as
  `StateFlow` via `asStateFlow()`; it exposes a single `onAction(action)` entry
  point (or dedicated callbacks when clearer).
- The navigation entry composable (`feature/<name>/navigation/<Feature>Entry`)
  obtains the ViewModel with `hiltViewModel()` and collects
  `uiState` with `collectAsStateWithLifecycle()`.
- The screen composable (`feature/<name>/ui/<Feature>Screen`) is stateless:
  it receives state and callbacks as parameters and emits actions upward.
- User-visible strings live in the owning module's `res/values/strings.xml`.

## Dependency injection

Hilt is set up in `:app` (application class annotated with `@HiltAndroidApp`,
activities with `@AndroidEntryPoint`) and in feature modules. ViewModels use
constructor injection with `@HiltViewModel`. Keep manual bindings in
`app/di`; domain models stay free of DI annotations.

## Planned layers

Add the following only when a working feature needs them, not as scaffolding:

- Room for persistent app data, Preferences DataStore for device preferences.
- Retrofit + OkHttp + Kotlin serialization for backend HTTP/JSON calls.
- WorkManager for durable background work.
