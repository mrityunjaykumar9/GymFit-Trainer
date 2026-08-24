# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Free Gym Trainer: a free, ad-supported Android fitness app (exercise library, guided workout
execution with sets/reps/rest timer, nutrition/macro calculators, water tracking, progress
dashboard). It runs **fully offline** on a local Room (SQLite) database — there is no backend,
no login, and no cloud sync. Monetization is AdMob banners only.

This scope is a deliberate simplification from a larger original spec (Kotlin Multiplatform +
iOS, Ktor backend, Postgres, Redis, K8s) down to a single-person, zero-hosting-cost Android app.
Don't reintroduce backend/multiplatform complexity unless the user explicitly asks for it — the
`data/repository/` layer is the intended seam for a future remote data source.

## Build & run

There is no committed Gradle wrapper (`gradlew`/`gradlew.bat` are absent). Building from the
command line requires a system-installed `gradle`, or opening the project in Android Studio
(which will offer to regenerate the wrapper). Prefer guiding the user through Android Studio
(Run ▶, Build → Generate Signed App Bundle) over assuming `./gradlew` works.

If a wrapper exists or `gradle` is installed:
```bash
gradle assembleDebug          # debug build
gradle bundleRelease          # signed release .aab (needs keystore.properties, see below)
gradle test                   # JVM unit tests (app/src/test)
gradle connectedAndroidTest   # instrumented tests (app/src/androidTest), needs device/emulator
```

There are currently no test source sets present in `app/src/` despite the dependencies being
wired up (JUnit, Espresso, Compose UI test) — don't assume tests exist without checking.

Release builds sign using `keystore.properties` in the project root (gitignored, never commit
it) — see `app/build.gradle.kts` `signingConfigs`. Debug builds always use Google's official
test AdMob IDs (`ca-app-pub-3940256099942544/...`), which is intentional and safe to leave as-is.

## Architecture

**Manual DI, no Hilt/Koin.** `di/AppContainer.kt` is a hand-rolled container holding lazily-built
repositories, constructed once in `GymTrainerApp.onCreate()` and threaded through
`MainActivity` → `NavGraph` → every screen composable as a plain constructor parameter (`container:
AppContainer`). ViewModels are created per-screen via `GenericViewModelFactory` (`di/ViewModelFactory.kt`),
a lambda-wrapping `ViewModelProvider.Factory` — there's no ViewModel-scoping framework, each
screen builds its own ViewModel from `container` directly.

**Data flow:** `presentation/<feature>/` (Compose screen + ViewModel) → `data/repository/`
(one repository per domain area: Exercise, Workout, User, Progress, Nutrition, Seed) →
`data/local/dao/` (Room DAOs) → `AppDatabase` (single Room database, `data/local/AppDatabase.kt`,
schema export on). Entities live in `data/local/entity/`. When adding a field to an entity,
remember Room needs a migration (or `fallbackToDestructiveMigration` — check `AppDatabase` for
current policy) since `exportSchema = true`.

**Seeding:** `data/seed/` holds static seed data (exercises, foods, workout templates).
`SeedRepository.seedIfNeeded()` runs once on app startup (`GymTrainerApp.onCreate`, on the
`applicationScope` IO dispatcher) to populate the DB on first launch — offline-first, no network
call involved.

**Navigation:** single `NavGraph.kt` using Navigation Compose with a sealed `Screen` class
(`presentation/navigation/Screen.kt`) defining routes. All routes take `container` and (where
relevant) `settings: SettingsDataStore`. Long IDs (exercise ID, workout day ID) are passed as
nav args.

**Settings/preferences:** `di/SettingsDataStore.kt` wraps Jetpack DataStore for
theme/unit/onboarding-complete preferences — separate from the Room database, injected alongside
`AppContainer`.

**Notifications:** local-only reminders via `AlarmManager`, no push server. `notifications/`
holds `ReminderScheduler` (schedules alarms), `ReminderBroadcastReceiver` (fires notifications),
`BootReceiver` (reschedules alarms after device reboot by calling
`GymTrainerApp.rescheduleRemindersFromSettings`), and `NotificationHelper` (channel setup).

**Ads:** `ads/AdBanner.kt` is a thin Composable wrapper around AdMob banner views, reading the ad
unit ID from `BuildConfig.ADMOB_BANNER_ID` (set per build type in `app/build.gradle.kts`). Banners
are placed only on Home, Nutrition, and Progress — deliberately not during workout execution, to
avoid interrupting an active session.

**Domain calculators:** `domain/usecase/` holds pure calculation logic (BMR/TDEE, macros, calorie
estimation) decoupled from Room/Compose — these are the best candidates for unit tests if/when a
test suite is added.

## Known gaps (intentional, not oversights)

- No exercise demonstration video/animation — placeholder in `ExerciseDetailScreen.kt`, meant to
  be wired to ExoPlayer + external object storage (e.g. Cloudflare R2) later, not bundled in-APK.
- No login/cloud sync — single-device local storage only.
- No barcode scanner, wearables integration, or AI recommendations.

Don't "fix" these by adding scope unless asked — they're documented deferrals, not bugs.
