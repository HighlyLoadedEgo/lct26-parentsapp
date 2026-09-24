# Agent instructions

## Current verification preference — 2026-09-23

The user runs the application personally. Check builds only; do not start the
app, launch emulators, or run connected/instrumented tests unless the user
explicitly changes this preference. Common checks are
`./gradlew :app:testDebugUnitTest :core:report:testDebugUnitTest :feature:pin:testDebugUnitTest :feature:report:testDebugUnitTest :feature:questions:testDebugUnitTest :feature:quests:testDebugUnitTest :feature:scanner:testDebugUnitTest :app:assembleDebug :app:lintDebug`.

## Project

ParentsApp is a native Android app written in Kotlin with Jetpack Compose.
Application code lives in `app/src/main/java/ru/nksk/parentsapp/`. The app is
composed of `:app`, the shared data module `:core:report`, and one Gradle
module per feature: `:feature:pin`, `:feature:questions`, `:feature:quests`,
`:feature:report`, and `:feature:scanner`.

Read [the architecture guide](docs/architecture.md) before adding or moving app
code. Keep app composition in `app`, each feature in `feature/<name>`, and
shared presentation in the `core/ui` packages inside `app`. Features do not
import each other or app wiring; screen UI receives state and callbacks instead
of navigation objects.

## Tech stack

- Keep Kotlin, Gradle Kotlin DSL, and Jetpack Compose with Material 3. Manage
  dependency versions in [the version catalog](gradle/libs.versions.toml).
- Use Navigation 3 for in-app navigation. Each feature owns its `NavKey`
  (a `@Serializable` `data object`/class) and its
  `EntryProviderScope<NavKey>` entry; the app owns the back stack, the saved
  state serializer list (`app/navigation/AppNavigationSavedState.kt`), and the
  navigation policy (`AppNavigator`). Keep that serializer list in sync when a
  key is added. Screens receive state and callbacks, never navigation objects.
- Use reinforced MVVM: unidirectional data flow, immutable `UiState` exposed as
  `StateFlow`, explicit UI actions, and lifecycle-aware collection at screen
  entries. Screens render state and emit callbacks; ViewModels coordinate domain
  operations; domain code owns business rules, and repositories own data access.
  Use coroutines and Flow for asynchronous work.
- Use Hilt for dependency injection. Hilt is integrated; extend the existing
  graph using constructor injection. Feature-owned bindings live in the
  owning feature's `di` package (`feature/pin/di`, `feature/report/di`,
  `feature/scanner/di`) so implementations can stay `internal`; use `app/di`
  only for app-level wiring when it appears. Obtain injected ViewModels at
  feature navigation entries; screens receive state and callbacks, and domain
  models remain free of DI annotations.
- Gate every app start behind the parent PIN (`:feature:pin`): first run asks
  parents to choose a 4-digit PIN, later runs ask for it before the tab shell
  (`Statistics` root). Store
  only a salted PBKDF2 hash (`PinHasher`) in Preferences DataStore
  (`PinRepositoryImpl`); never store or log the plaintext PIN, and keep hashing
  off the main thread. Verify through the repository, never by reading the
  stored record directly.
- Preferences DataStore holds small device state (the parent PIN record, the
  remembered pet id, the parent-created quest); keep each store inside its
  owning module's data package.
- Add Room and WorkManager when a working feature needs them; do not
  scaffold unused layers or dependencies for placeholders.
- Use CameraX with ML Kit barcode scanning for scanning features. The scanner
  client is QR-only, provided by Hilt in `feature/scanner/di`; camera frames go
  through `QrFrameAnalyzer`, and scan state follows the same MVVM rules as
  every screen. Camera permission is requested through the
  `ActivityResultContracts.RequestPermission` API at the feature entry.
- Use Retrofit + OkHttp + Kotlin serialization for backend HTTP/JSON calls.
  The backend base URL is `https://fin-api.mortypython.ru/` (FastAPI, camelCase
  JSON); report networking (API, DTOs, repository, pet-session store, Hilt
  bindings) lives in `:core:report` and is shared by the report, questions,
  and quests features.

## Working conventions

- Preserve existing uncommitted work and limit changes to the requested scope.
- Record unresolved design decisions rather than silently guessing; do not
  invent product rules that were not requested.
- Validate behavior changes with relevant tests. Common checks are listed under
  the verification preference above.
- Documentation-only changes require checking local links and consistency;
  they do not require an Android build.
