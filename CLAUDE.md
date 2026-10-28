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

**Ads:** `ads/AdBanner.kt` is a thin Composable wrapper around an AdMob adaptive banner view,
reading the ad unit ID from `BuildConfig.ADMOB_BANNER_ID` (set per build type in
`app/build.gradle.kts`). It's placed on every content screen except Onboarding (Google's AdMob
policy bans ads on loading/setup screens) and the active workout-execution timer (accidental-tap/
invalid-traffic risk during a timed session — an interstitial is shown there instead, once, when
the workout finishes). `ads/InterstitialAdManager.kt` shows a full-screen interstitial at natural
break points only (workout finished, custom workout saved, meal logged), gated by its own
session-engagement and cooldown timers so it can't show on cold launch or back-to-back.

**Room migrations (production data!):** the app is live, so every schema change needs a real
`Migration` registered in `AppDatabase` (see `MIGRATION_7_8`) plus a case in
`app/src/androidTest/.../DatabaseAndPlannerTest.kt` (`connectedDebugAndroidTest`, uses the exported
`app/schemas/*.json`). There is deliberately NO `fallbackToDestructiveMigration()` for v7+ - a missing
migration must fail in testing instead of silently wiping users' workouts, weights and profile.
Only installs older than v7 may still be wiped (`fallbackToDestructiveMigrationFrom(1..6)`).

**Nutrition:** `domain/nutrition/` is pure Kotlin (JVM-testable): `DietModels` (goal / diet style /
macro style / `DietPrefs`, vegan-veg-egg classification of foods), `PlanGenerator` (builds a 7-day plan
on the device from the person's targets: meal templates scaled to hit calories and protein, with a
protein-repair pass), `Insights` (weekly summary, coach tips, "what to eat next"), `Fasting`,
`ShoppingList`. `NutritionCalculator.planTargets` applies goal + pace + macro style with safety limits
(calorie floor 1200 F / 1500 M, deficit capped at 25%, no deficit under 18). The person's diet choices
live in `SettingsDataStore` (`savedDietPrefs`), not Room, so changing them needs no migration.
`data/repository/NutritionPlanner` resolves profile + prefs + latest logged weight into targets and
the plan - Home, the Nutrition tab (Today / Plan / Insights) and the meal reminders all use it so they
always agree. New foods go in `FoodSeedData` / `MoreFoodSeedData`; `SeedRepository` inserts any missing
foods (by name) on existing installs. The old fixed veg / non-veg `meal_plan_*` tables are legacy and
unused (kept in the schema only so databases open without a migration). JVM tests:
`app/src/test/.../NutritionPlanTest.kt` (`testDebugUnitTest`).

**Home:** `presentation/home/` - no stat cards. A weekly-split suggestion (`HomeViewModel.WEEKLY_SPLIT`), a
muscle picker row, a live 3D "exercise of the day" (rotates daily), and shortcuts to All exercises / Yoga /
Nutrition / weight logging.

**Domain calculators:** `domain/usecase/` holds pure calculation logic (BMR/TDEE, macros, calorie
estimation) decoupled from Room/Compose — these are the best candidates for unit tests if/when a
test suite is added.

**Food art:** `assets/food_icons/` holds Kenney "Food Kit" PNGs (CC0, credited in Settings). The
food-name -> icon mapping lives in `presentation/components/FoodIcons.kt`; add a mapping there when
adding a new seeded food. Nutrition UI building blocks are in `presentation/nutrition/NutritionComponents.kt`.

## Known gaps (intentional, not oversights)

Hands (`HandModes.kt` + `AthletePoser`): each exercise has a grip (overhand, underhand, neutral, palms down, or flat on the mat);
the solver orients the hand outright and splits the forearm roll between forearm and wrist, so palms never flip mid-rep.
Dumbbell/handle direction comes from the closed hand (`AthletePoser.handleAxis`), the barbell is centred between both hands
with its plates outside the grip (`PropKit.barbell`).

Front-view data (`tools/exercise_motion/m_lateral.py`): grip width, stance width, elbow flare / arm swing, and `frontal`
poses (Warrior II, Triangle, Tree, Lateral Raise) for the movements that live in the plane facing the viewer. The authored
key poses are side-view only - always check a changed exercise from the Front and Side chips. Coaching text (what it builds +
3 form cues) is in `domain/training/ExerciseCoach.kt`; `ExerciseContentTest` fails if an exercise lacks motion, coaching or
pictures, or if photos come back into assets.

Floor handling in 3D (`AthleteStage.poseBody`): hands/feet/knees/elbows must rest on the floor or mat (the whole pose is
raised just enough), a sinking head is tipped up, and the mat is sized from the body's reach over the whole movement so
no limb leaves it.

- No real-person exercise *video*. Instead every exercise is demonstrated in 3D by a realistic, skinned
  bodybuilder model (`assets/models/athlete.glb`, Quaternius "Universal Base Characters" Superhero_Male, licensed
  for use inside apps but NOT for redistribution as files - so the glb is gitignored; rebuild it with
  `tools/athlete_model/build_glb.py`) rendered by Filament (`presentation/components/body3d/`). The poses come from
  the same joint-angle key poses as before (`assets/exercise_motions.json`, authored with `tools/exercise_motion/`):
  `Pose3D.kt` lifts a side-view skeleton into 3D, `AthletePoser.kt` (pure Kotlin) turns it into bone rotations
  (spine/neck/head follow the pose, arms and legs use two-bone IK so hands/feet land exactly on bars, benches and the
  floor), `PropKit.kt` builds 3D gym equipment from the 2D prop data using pooled meshes from `assets/models/props/`
  (generated by `tools/athlete_model/build_props.py`), `AthleteStage.kt` owns the Filament engine/scene/camera/lights
  and `Athlete3DCanvas.kt` is the Compose wrapper (TextureView; drag to orbit). Debug builds accept
  `adb shell am start ... --es debug_exercise "Barbell Curl" --ef debug_yaw -35 --ef debug_t 0.5` to open an exercise
  frozen at a camera angle/time (see `DebugView`) for screenshot review. The old 2D side-view canvas remains
  (`motion/`) as the player's "3D off" mode. `Exercise.mediaUrl` can still point at a remote clip later.
- No photos or video anywhere in the app (removed on purpose to keep it small and fully original): exercise pictures
  are tiny WebPs rendered from the 3D athlete itself (`assets/exercise_pics/<slug>_a|b.webp`, start / peak of the
  rep; regenerate with `tools/athlete_model/make_pics.py` from debug-build screenshots). Muscle diagrams are drawn in
  code (`presentation/components/BodyMap.kt`). Headers are gradients.
- No "Workouts" tab, plans, history or progress charts: removed on purpose. The product is "how do I do this
  exercise correctly" - Home suggests a muscle for the day, the Exercises tab has a Train-a-body-part hub with ready
  routines (`domain/training/BodyPartPlans.kt`), and each exercise has the 3D demo. The guided session screen
  (`WorkoutExecutionScreen`) is only reachable from a body-part routine's "Start workout" button.
- No login/cloud sync — single-device local storage only.
- No barcode scanner, wearables integration, or AI recommendations.

Don't "fix" these by adding scope unless asked — they're documented deferrals, not bugs.
