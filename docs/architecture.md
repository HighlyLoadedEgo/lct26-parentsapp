# Architecture

Current implementation: 2026-09-29. ParentsApp is a native Android companion
using Kotlin, Compose Material 3, Hilt, Navigation 3, Retrofit/OkHttp and
Preferences DataStore.

## Modules and presentation

- `:app` owns the Activity, composition, theme, navigation stack, route serializers
  and navigation policy. Features receive state and callbacks, never a navigator.
- `:core:report` owns the parent report API, remembered device ID (`PetSessionStore`),
  and parent cap rewards API/repository with durable pending requests.
- `:core:ui` owns the item carousel reused from the main game onboarding.
- `:feature:pin` owns PIN storage, verification and the access gate.
- `:feature:report` owns the report, manual ID entry and profile reset.
- `:feature:questions` owns skill details and published conversation materials.
- `:feature:quests` owns the three authored quests, checklist and cap selection state.
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
only the authored quest identifier. AppNavigationSavedState registers both keys.
Old `Quests`, `PinSetup`, and `PinLock` serializers remain for compatibility;
new PIN access is always enforced outside the navigation tree.

## PIN access

The Activity owns `ParentsAccessViewModel`, which gates the complete navigation
tree before any restored report can appear. Access exists only in memory. A real
background transition locks it; rotation retains the ViewModel. A new process
requires the PIN again. Late verification after background cannot reopen access.
**PARENT-SCREENSHOTS-D-001 — Принято пользователем, 2026-09-29.** Screenshots
are allowed in the standalone parent app; its Activity does not set `FLAG_SECURE`.

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
The standalone report reads the selected device's synchronized world through
`POST /v1/profiles/snapshot/download`, then queries real assessments for that
world's run through `POST /v1/profiles/skills/query`. It no longer calls the
legacy demo endpoint. The read-only snapshot projection supplies the saved pet
name, age, fur, accessory, visual state, available money and savings. Bundled
artwork and its selection rules are copied from the main game.

Assessments must have the matching run, supported schema, all 12 skill IDs and
a history cursor no later than the snapshot. Older assessments carry a visible
staleness notice; unavailable/invalid assessments are labelled unavailable,
without inventing mastery. Missing cloud saves show an explicit synchronization
message rather than substituting a demo pet. No game data is uploaded or changed.

Published conversation materials come independently from
`GET /v1/parent-materials`. Materials or assessment failures do not prevent the
real pet from loading. The report explains that its data reflects the child's
last synchronization, and refresh retries all read endpoints.

**PARENT-SOURCES-D-001 — Принято пользователем, 2026-09-29.** Research/source
links are clickable in both the main game's parent mode and this companion.
Each `researchSources` URL uses Compose `LinkAnnotation.Url` with an underlined
primary-color style and system URL handling. Source order and text selection
for copying are preserved.

**PARENT-REAL-DATA-D-001 — Requested by the user, 2026-09-29.** Replace the
previously preserved demo data with real synchronized data. This supersedes the
hardcoded-report exception in PARENT-STANDALONE-D-001. Verification and launch
on the active emulator are explicitly authorized for this task.

## Quests

Shopping, Weekend and Second life retain the main app's authored content
(PARENT-MODE-D-006/007/008) and four required checkboxes. Completing a quest now
issues one of the same six caps through the parent reward contract. The carousel
and six bundled icons come directly from the game app; the app supplies artwork
through a composable slot. Already owned/issued caps cannot be newly granted.

**PARENT-STANDALONE-D-002 — Принято пользователем, 2026-09-29.** Both applications
have symmetric cap selection and explicit issuance. The standalone parent does
not apply inventory changes immediately; only the game client applies/ACKs the
grant. Issuance succeeds after a matching server grant. No snapshot upload, game
state replacement, automatic issuance or delivery ACK happens in this app.

Availability uses the currently linked device's downloadable snapshot plus its
complete reward journal (including acknowledged grants). No snapshot means the
child must first synchronize the game. New issuance rechecks availability and
target; changing device/run requires reopening the quest. Remote state cannot
reflect unsynchronized child changes: the game client's atomic duplicate guard
ensures another copy of an already owned cap is not added during offline races.

Before HTTP, the exact request and idempotency key are durably saved in
`noBackupFilesDir/parent_quest_rewards.preferences_pb`, scoped by device/run/quest.
Timeouts, cancellation, uncertain replies and process death retain that request.
Retry uses the same key/body even if the journal already contains the grant.
Only a validated matching grant or definite UNKNOWN_ACCESSORY/INVALID_REWARD
rejection clears it. HTTP 429 respects a persisted Retry-After deadline.

Checklist/completion UI remains local to the quest entry. Reopening a pending
issuance restores its cap and completed checklist; reopening a finished quest
starts fresh, with the already granted cap unavailable. There is no Create quest
action. The old Quests placeholder and QuestStore remain for compatibility.
See [reward contract](parent-cap-rewards.md) and
[cap artwork provenance](design/assets/README.md).

## Application updates

By PARENT-RUSTORE-D-001 (2026-09-29), `app/updates` ports the child's RuStore
In-app Updates 10.5.1 FLEXIBLE flow. The app-owned host is mounted only after
PIN unlock and observes/checks while RESUMED. Its Activity-scoped ViewModel
survives rotation and report navigation. Background removes the listener and
requires PIN again before the host resumes. Installation needs explicit user
confirmation; store failures do not block the report. The Hilt manager uses
Application Context; neither update state nor SDK objects enter navigation
keys, PIN storage or the remembered device. See [RuStore updates](rustore-updates.md).

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
