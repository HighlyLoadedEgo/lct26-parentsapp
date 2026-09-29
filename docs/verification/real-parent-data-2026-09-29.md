# Real parent data verification — 2026-09-29

The user requested real synchronized data instead of the legacy demo report and
explicitly authorized read-only backend verification and launch on the active
emulator. This supersedes the old demo-data exception in the parity task.

## Automated checks

`./gradlew testDebugUnitTest :app:assembleDebug :app:lintDebug`

BUILD SUCCESSFUL. 118 JVM tests, zero failures or errors. Android lint: zero
errors (25 warnings). Regression tests first failed for demo identity fallback,
missing saves and legacy format-5 snapshots, then passed after the fixes.
Coverage includes real money and skill mapping, matching runs/history cursors,
stale/unavailable assessments, independent materials failures and saved pet art.

## Read-only backend and emulator

The authorized device was queried through the production game's snapshot,
skills and materials endpoints. The legacy endpoint returned a demo pet and
balance; the new read-only endpoints returned the saved identity, equipped cap,
money breakdown and assessments matching the current run and history cursor.

The APK was installed and launched on the active emulator. A user-approved test
PIN was created through the UI; the device was linked through manual entry.
UI inspection confirmed that the real name, money breakdown and skill totals
matched the server, with the synchronization notice and no demo badge. The first
topic opened its published learning goal, story and conversation questions.
AndroidRuntime reported no crash. No game state, rewards or analytics were written.

After clickable research sources were added, the full JVM/build/lint command
passed again. Source URL order, text and selection were reviewed. Browser taps
were not device-tested in this change. Reports reflect the child's latest sync.
