# Standalone parent parity verification — 2026-09-29

Source: main application's `feature/parents` as inspected on 2026-09-29.
Target: `HighlyLoadedEgo/lct26-parentsapp`, branch `codex/parent-app-parity`.

## Implemented parity

- Three authored quest demos with four checks, progress and placeholder reward;
  no Create quest button, reward delivery or persistent demo progress.
- Existing published skill materials, all four server statuses and 64-bit money.
- Refresh on foreground/report entry, explicit refresh and retained current
  content on refresh failure. QR/manual linking and confirmed reset remain.
- Android ID support alongside legacy UUIDs; failed first scan can be retried.
- Reset invalidates late loads and remains independent of network cancellation;
  linking retires old report navigation entries and the transient scanned ID.
- Main-app PIN gate before restored navigation, foreground relock, persisted
  failed-attempt delay and one-time preservation of existing PIN records.

## Local checks

Command:

```sh
./gradlew testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin :app:lintDebug
```

Result: BUILD SUCCESSFUL, **79 JVM tests, 0 failures, 0 errors**. Android lint:
0 errors, 24 warnings (dependency/version updates, target API and redundant
manifest label). No dependencies or target API upgraded for this task.

Regression tests reproduced Android-ID rejection, failed-scan retry, late-response
reset and stale-session behavior before fixes. On-disk DataStore tests verify
remembered ID across store reopening, persistent clear and legacy PIN migration
followed by reopening. PIN tests cover wrong PIN, throttle, corrupt storage,
late verification, setup races and process-style ViewModel recreation.

No UI/device/instrumented testing was run. These checks do not prove CameraX
hardware behavior, visual layout on every device, or restore into the child's
local Room database.

## Live backend

[Machine-readable results](live-backend-2026-09-29.json) record 12 requests.
The supplied device was used only for read requests:

- Parent report: 200, 12 skills, all 12 topic materials available, `isDemo=true`.
- Published material catalogue: 200, version `2026-09-29-v1`.
- Cloud world download: 200, CURRENT_WORLD.
- Real skill assessments query for that world's run: 200.

On a separate synthetic profile, registration and its identical retry succeeded.
Snapshot upload and its identical retry returned the same result. Download
returned the exact original snapshot string and revision. Changing a body under
the same key returned IDEMPOTENCY_CONFLICT; a stale revision returned
SNAPSHOT_REVISION_CONFLICT. A subsequent download confirmed the saved world and
revision remained intact. The synthetic profile is retained; the API exposes no
profile deletion method. No user snapshot, analytics or rewards were written.

The parent's existing legacy endpoint still supplies demo balances/statuses.
The standalone app preserves that explicitly requested hardcoded behavior; it
does not silently replace it with a new aggregation mechanism. The separate
skills query confirms real game assessments are available on the server.
