# LCT Parents App

Native Android app for parents (LCT 2026), written in Kotlin with Jetpack
Compose and Material 3. Currently a bare stub: single placeholder screen.

## Structure

- `:app` — application module: composition root, theme, shared presentation
  (`core/ui` packages), manifest and launcher resources.
- `:feature/home` — placeholder feature module (screen + ViewModel + entry).

See [AGENTS.md](AGENTS.md) for working conventions and
[docs/architecture.md](docs/architecture.md) for the architecture guide.

## Build

```bash
./gradlew :app:assembleDebug
```

Common verification checks:

```bash
./gradlew :app:testDebugUnitTest :feature:home:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Android SDK location is read from `local.properties` (`sdk.dir=...`), which is
not committed.
