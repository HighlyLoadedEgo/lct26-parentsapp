# Architecture

Current implementation: 2026-09-29. ParentsApp is a native Android companion
using Kotlin, Compose Material 3, Hilt, Navigation 3, Retrofit/OkHttp and
Preferences DataStore.

## Modules and presentation

- `:app` owns the Activity, composition, theme, navigation stack, route serializers
  and navigation policy. Features receive state and callbacks, never a navigator.
- `:core:report` owns the parent report API, DTOs, repository and remembered device
  ID (`PetSessionStore`). Report and questions consume that same contract.
- `:feature:pin` owns PIN storage, verification and the access gate.
- `:feature:report` owns the report, manual ID entry and profile reset.
- `:feature:questions` owns skill details and published conversation materials.
- `:feature:quests` owns the three authored local demos and their checklist state.
- `:feature:scanner` owns CameraX/ML Kit QR scanning and runtime camera permission.

ViewModels expose immutable state through StateFlow. Navigation entries obtain
Hilt ViewModels and collect with lifecycle awareness; UI emits callbacks. Feature
modules never import each other or app wiring. The app supplies quest cards from
`:feature:quests` through a composable slot in the report entry.

## Navigation and linking

The protected root is `Statistics`. QR scanning and manual input remain available.
Manual entry accepts Android IDs (16 hexadecimal characters) and legacy UUIDs.
Manual entry preserves its existing save-before-navigation behavior. QR handoff
is persisted only after a successful response with the same device ID.

Linking replaces the entire old report stack through source-guarded `resetFrom`.
After a successful QR handoff the transient `Statistics(petId)` becomes
`Statistics()`; later restoration reads the selected device from DataStore.
Failed QR loads retain the attempted ID for explicit retry. A resumed report
without a handoff rereads the stored selection.

Report and topic entries refresh while RESUMED, cancel network work when hidden,
and retain already loaded content with an error message if refresh fails. The
report and skill screen both expose manual refresh. Reset remains an explicit
confirmed action and clears only the remembered ID, not the child's game or PIN.
It invalidates pending fetches; a late response cannot re-persist a cleared ID.
Reset storage work is separate from cancellable fetch work, and the UI waits for
it to finish before offering another navigation action.

`QuestionTopic(skillId)` carries only the skill ID. `Quest(ParentQuest)` carries
only the authored demo identifier. AppNavigationSavedState registers both keys.
Old `Quests`, `PinSetup`, and `PinLock` serializers remain for compatibility;
new PIN access is always enforced outside the navigation tree.

## PIN access

The Activity owns `ParentsAccessViewModel`, which gates the complete navigation
tree before any restored report can appear. Access exists only in memory. A real
background transition locks it; rotation retains the ViewModel. A new process
requires the PIN again. Late verification after background cannot reopen access.
The secure window flag prevents report/PIN content in screenshots and recents.

The implementation is ported from the main application's parent mode. Setup
requires four digits and confirmation. Five wrong attempts impose the same
persisted 30-second delay. Storage errors keep the gate closed and offer retry;
they never silently offer a fresh PIN. Forgotten-PIN reset remains undefined.

The new salted PBKDF2 verifier and attempt state live in
`noBackupFilesDir/parents_pin.preferences_pb`. A one-time DataStore migration
copies and validates the old `files/datastore/pin_store.preferences_pb` verifier
without changing the PIN. Both its legacy SHA256/Base64 representation and the
main application's SHA1/hex representation are readable. New verifiers use the
API-24-compatible algorithm from the main app. The old file is retained for data
preservation and excluded from backup/transfer; an existing new record prevents
re-import. Legacy PIN repositories delegate to the same new singleton store.

## Backend and materials

The existing base URL remains `https://fin-api.mortypython.ru/`.
`GET /api/parents/{petId}` returns the standalone report. Money uses Long;
statuses include MASTERED, PRACTICING, NO_DATA and HAS_PROBLEM. Assessments are
not calculated from client counters or `isMastered`.

The existing report payload also supplies `materialsAvailable`, `learningGoal`,
`story`, `replaceWithParentStory`, `conversationStarters`, `parentTakeaway`,
`researchBasis` and `researchSources`. The topic screen displays those sections
without authoring new text. Older/unpublished payloads retain their empty state.
The standalone client keeps its report API rather than reconstructing gameplay
history from a snapshot. Its demo badge remains driven by `isDemo`.

Live verification confirmed that this endpoint still returns demonstration pet
and assessment values, even though the separate game skills endpoint has real
assessments. Preserving that hardcoded behavior is part of the user request.
See [verification](verification/parent-parity-2026-09-29.md).

## Quests

Shopping, Weekend and Second life are copied from the main app's authored demos
(PARENT-MODE-D-006/007/008). Each shows its description, four checkboxes, progress,
an accessory placeholder and local completion. All four checks are required;
completion freezes the checklist. A new entry starts fresh. No reward request,
game state write, new quest content or persistent demo progress is introduced.
There is no Create quest action. The old Quests placeholder and QuestStore remain
for compatibility; the latter does not store the new demo progress.

## Approved scope and verification

**PARENT-STANDALONE-D-001 — Принято пользователем, 2026-09-29.** Bring the
standalone parent's changed features up to the main application, preserving
synchronization/refresh, reset and standalone linking. Keep hardcoded features
hardcoded; do not invent product rules. Test the app/backend where possible.

Verification includes JVM tests, debug assembly, release Kotlin compilation and
Android lint. No app/emulator or connected/instrumented test was launched.
DataStore tests use real on-disk stores to verify selected-ID reopen/reset and
legacy PIN migration/reopen. Live writes use a separate synthetic profile;
the user's device is probed read-only. No Room or WorkManager is introduced.
