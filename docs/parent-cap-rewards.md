# Parent quest cap rewards

User approval: PARENT-STANDALONE-D-002, 2026-09-29. This companion implements
the same six-cap flow as LCTApp, except immediate local inventory application.

## Transport

Base URL: `https://fin-api.mortypython.ru/` (existing app backend).

- `POST /v1/profiles/snapshot/download`: `{deviceId, schemaVersion:1}`.
  Read `gameRunId` and `snapshotJson` state.ownedItems[].itemId; supported
  CURRENT_WORLD v1 or legacy snapshot format 1–5. Validate the envelope/run.
- `POST /v1/profiles/rewards/pull`: `{deviceId, gameRunId, afterSequence, limit:50,
  schemaVersion:1}`. Start at zero, require contiguous sequences and follow
  nextAfterSequence/hasMore. Include ACCESSORY grants in availability, even
  if acknowledged. Reject mismatched identities/cursors/malformed pages.
- `POST /v1/parent-profiles/rewards`: header `Idempotency-Key`, body
  `{deviceId, gameRunId, reward:{type:"ACCESSORY",itemId}, schemaVersion:1}`.
  Persist both before sending. Matching 200/201 grant confirms issuance.
  Retries keep the same request and key. A definite 422 UNKNOWN_ACCESSORY or
  INVALID_REWARD releases the selection; ambiguous failures preserve it.

No game snapshot is restored or uploaded, and no reward is acknowledged from
this app. Delivery and duplicate inventory protection belong to the child game.
Server-journal availability can lag unsynchronized child inventory; it still
prevents repeat issuance of a gift waiting for delivery from this parent client.
Run registration is performed by the child's normal game synchronization.

## Shared catalog

| itemId | Artwork |
| --- | --- |
| cosmetic-cap-moscow-blue-v1 | gear_cap_moscow_blue |
| cosmetic-cap-moscow-emerald-v1 | gear_cap_moscow_emerald |
| cosmetic-cap-moscow-burgundy-v1 | gear_cap_moscow_burgundy |
| cosmetic-cap-lct2026-blue-v1 | gear_cap_lct2026_blue |
| cosmetic-cap-lct2026-emerald-v1 | gear_cap_lct2026_emerald |
| cosmetic-cap-lct2026-burgundy-v1 | gear_cap_lct2026_burgundy |

[Full game contract](https://github.com/salyamii/lct2026/blob/main/docs/backend/parent-rewards.md).
[Asset provenance](design/assets/README.md).

## Verification

Compile repository/ViewModel regression tests and assemble debug, compile release,
run Android lint. Per the current user preference, do not run tests or launch
apps/emulators. Coverage source includes frozen retries across DataStore reopen,
owned/pending gifts, pagination, target changes, mismatched replies, definitive
rejections, malformed snapshots/journals and throttling. Compilation alone does
not prove runtime behavior; the user verifies the app manually.

Build verification on 2026-09-29: `:app:assembleDebug`,
`:app:compileReleaseKotlin`, `:app:compileDebugUnitTestKotlin`,
`:app:compileDebugAndroidTestKotlin`, `:core:report:compileDebugUnitTestKotlin`,
`:feature:quests:compileDebugUnitTestKotlin`, `:app:lintDebug` all completed
successfully (offline Gradle build, 26 seconds). Lint: 0 errors, 25 warnings.
No tests, applications or emulators were launched. All six icon files matched
LCTApp byte-for-byte; carousel code matched after namespace substitution.
