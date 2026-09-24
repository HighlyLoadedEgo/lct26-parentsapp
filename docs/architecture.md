# Architecture

ParentsApp is a native Android application written in Kotlin with Jetpack
Compose, Material 3, and Hilt. The project currently is a bare stub; this
document records the structure that new code must follow.

## Modules

- `:app` — the Android application. Owns composition (`app` package), the
  manifest, the launcher, app-owned navigation (back stack, `AppNavigator`,
  saved state serializers in `app/navigation`), wiring (`app/di` once a binding
  graph appears), and shared presentation in the `core/ui` packages (theme
  today, shared components as they appear).
- `:feature/<name>` — one Gradle module per feature (currently
  `:feature:questions`, `:feature:quests`, `:feature:report`,
  `:feature:scanner`, and `:feature:pin`). A feature module contains its
  screens, its ViewModels, its navigation entry and `NavKey`, and its own
  resources. Features never import each other and never import app wiring.
  Feature-owned Hilt modules provide their own dependencies (see
  `feature/scanner/di`, `feature/pin/di`).
- `:core:report` — the shared data module for the parent report: Retrofit API,
  DTOs, `ParentReportRepository`, the `PetSessionStore` (DataStore), and their
  Hilt bindings. Consumers: `:feature:report` and `:feature:questions`. A
  `core/<name>` module appears only when several modules actually share the
  data or domain logic; do not create one preemptively.

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

## Navigation

Navigation 3 (`androidx.navigation3`) mirrors the structure: each feature
declares a `@Serializable` `NavKey` and an `EntryProviderScope<NavKey>`
extension; the app builds the back stack with `rememberNavBackStack`, lists all
keys in `AppNavigationSavedStateConfiguration`, and routes callbacks through
`AppNavigator` (single-top navigation, guarded back, `resetTo`, `replace`).
Screens take plain callbacks; `dropUnlessResumed` wraps callbacks that leave
the current entry.

After the PIN gate the app lands on the parent-mode report (`Statistics`,
the `Statistics` route). There is no tab bar: the report is the only root,
and the PIN gates `resetTo` it. The scanner and the manual pet-id input push
above it and `replace` themselves with `Statistics(petId = …)`, so the fresh
id rides in the navigation key while the session persistence happens on a
successful fetch.

Skill topics open `QuestionTopic(skillId)` — the questions feature's practice
screen, reached from inside the skills list; the key carries only the stable
skill id and the title is resolved through the report repository. Back pops
to the report. Below the summary tiles the report renders the quests section:
a framed card with one big lime call-to-action that pushes the standalone
`Quests` screen (`:feature:quests`) — a placeholder while quest creation
awaits a backend endpoint. The module keeps a device-local `QuestStore`
(Preferences DataStore) for the future parent-created quest; it is not wired
into the UI yet.

## Startup gate

`AppStartupViewModel` (in `:app`) reads `PinRepository.hasPin()` once before
navigation shows anything, mirroring the startup-gate pattern: `Loading` renders
a spinner, then `ParentsNavHost` starts on `PinSetup` (no PIN yet) or `PinLock`.
Both gates call `AppNavigator.resetTo(Statistics())` on success, so Back never
returns to a gate.

## Parent PIN

`:feature:pin` owns the PIN flow end to end. `PinHasher` salts the PIN and
derives a PBKDF2-HMAC-SHA256 hash; `PinRepositoryImpl` stores only
`version|iterations|salt|hash` in Preferences DataStore, compares with
`MessageDigest.isEqual`, and hashes on `Dispatchers.Default`. Open follow-ups:
throttling repeated wrong attempts, auto-lock on background, and biometric
unlock.

## Networking

`:core:report` talks to the backend at `https://fin-api.mortypython.ru/`
(FastAPI, camelCase JSON) with Retrofit + OkHttp + kotlinx.serialization.
`GET /api/parents/{petId}` returns the parent report (pet summary + skill
list). The report opens after a QR scan (`Scanner` navigates with the captured
value) or through the manual pet-id input screen, which validates the UUID
shape before navigating. Load failures render an in-screen error with retry;
no toasts or global error surfaces. `QuestionTopic` reads the same report
through `ParentReportRepository` to resolve the tapped topic.

The backend currently exposes only `/api/pets` and `/api/parents/{petId}`;
question content has no endpoint yet, so `QuestionTopic` renders an honest
placeholder for the topic. Quests are stored locally (`QuestStore`) until the
backend gains quest endpoints; the reward payout is not wired anywhere.
Inventing question content on the client is out of scope.

The last successfully loaded pet id is remembered in DataStore
(`PetSessionStore` in `feature/report/data`): after PIN unlock the app opens
that pet's report directly, and "Сменить питомца" on the report screen clears
it (with a confirmation dialog) and returns to the empty `Statistics` chooser.

## Scanning

`:feature:scanner` uses CameraX (`Preview` + `ImageAnalysis` with
`STRATEGY_KEEP_ONLY_LATEST`) feeding an ML Kit barcode scanner restricted to
QR codes. The Hilt graph provides the scanner client and binds `QrFrameAnalyzer`;
the ViewModel turns detections into state (result pauses scanning until the
user restarts). Camera permission is requested at the entry via
`ActivityResultContracts.RequestPermission`.

## Dependency injection

Hilt is set up in `:app` (application class annotated with `@HiltAndroidApp`,
activities with `@AndroidEntryPoint`) and in feature and core modules.
ViewModels use constructor injection with `@HiltViewModel`. Keep module-owned
bindings in the owning module's `di` package (`feature/pin/di`,
`feature/scanner/di`, `core/report/di`); use `app/di` only for app-level
wiring when it appears. Domain models stay free of DI annotations.

## Planned layers

Add the following only when a working feature needs them, not as scaffolding:

- Room for persistent app data, Preferences DataStore for device preferences.
- Retrofit + OkHttp + Kotlin serialization for backend HTTP/JSON calls.
- WorkManager for durable background work.
