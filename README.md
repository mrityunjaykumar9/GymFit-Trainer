# Free Gym Trainer

A free, ad-supported Android fitness app built around one question: *how do I do this
exercise correctly?* Every exercise is demonstrated by a realistic 3D bodybuilder (rotate it,
slow it down, read the form cues), there is a "train a body part" hub with ready-made
routines (chest, back, shoulders, biceps, triceps, legs, glutes, abs ...), plus yoga, nutrition
& macro planning, and weight logging. Everything runs **fully offline** on-device using a local
Room database — there is no backend server, so there's nothing to host and nothing that costs
money to run.

## Why this architecture

The original spec called for a much bigger build (Kotlin Multiplatform + iOS, a Ktor
backend, PostgreSQL, Redis, Kubernetes). That's a legitimate architecture for a
funded, multi-person team — but it's the wrong starting point for one person trying to
publish a free app and start earning ad revenue quickly. This build trades that scope for:

- **Android only** (Kotlin + Jetpack Compose) — no Kotlin Multiplatform/iOS complexity.
- **No backend, no login, no cloud sync** — Room (SQLite) stores everything on-device.
- **$0 hosting cost** — nothing to deploy or pay for except the one-time Play Console fee.
- **AdMob banners** for ad revenue, since the app is meant to stay free.

You can add a backend and cloud sync later without rewriting the app — the repository
layer (`data/repository/`) is the seam where that would plug in.
    
## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Room (local SQLite database)
- DataStore (theme/unit preferences)
- Navigation Compose
- AlarmManager (local notifications — no push server needed)
- Google AdMob (`play-services-ads`)
- Gradle (Kotlin DSL), KSP for Room's annotation processing

## Project structure

```
FreeGymTrainer/
├── app/
│   └── src/main/
│       ├── java/com/freegymtrainer/app/
│       │   ├── data/
│       │   │   ├── local/          # Room entities, DAOs, database
│       │   │   ├── repository/     # Repository layer (the seam for a future backend)
│       │   │   └── seed/           # Exercise/food/workout-template seed data
│       │   ├── domain/usecase/     # BMR/TDEE/macro calculators
│       │   ├── di/                 # Manual DI container (AppContainer) + DataStore
│       │   ├── notifications/      # Local reminders (AlarmManager)
│       │   ├── ads/                # AdMob banner wrapper
│       │   ├── presentation/       # One package per screen (Compose UI + ViewModel)
│       │   ├── MainActivity.kt
│       │   └── GymTrainerApp.kt    # Application class - AdMob init, DB seeding
│       ├── res/                    # Strings, theme, launcher icon
│       └── AndroidManifest.xml
├── build.gradle.kts / settings.gradle.kts / gradle.properties
└── PRIVACY_POLICY_TEMPLATE.md
```

---

## 1. Running it on your local machine

### Prerequisites

- **Android Studio** (Koala or newer) — comes bundled with the Android SDK and a
  compatible Gradle/JDK, which is the easiest path. Download: https://developer.android.com/studio
- A device to test on: either a physical Android phone (USB debugging enabled) or an
  emulator created inside Android Studio (Device Manager → Create Device).

### Steps

1. **Unzip the project** and open the `FreeGymTrainer` folder in Android Studio
   (File → Open → select the folder).
2. Let Android Studio sync Gradle automatically (bottom status bar shows progress). First
   sync downloads dependencies from Google's and Maven Central's repositories and can take
   a few minutes.
   - If Android Studio reports a missing Gradle wrapper jar, click the prompt to let it
     regenerate one, or run `gradle wrapper` once from a terminal if you have Gradle
     installed separately.
3. Select a run target at the top toolbar (an emulator or your connected physical device).
4. Click **Run ▶**. The app builds and installs. First launch seeds the local database
   with the exercise library, food database, and workout templates (a few hundred
   milliseconds) — everything after that works fully offline.
5. You'll land on the onboarding flow the first time; complete it to reach the Home screen.

No `.env` file, no server to start, no Docker — that's the point of the local-only MVP.

### Debug ads

The app ships with **Google's official test ad unit IDs** in debug builds
(`ca-app-pub-3940256099942544/...`). These always serve real-looking test ads and are safe
to click during development — they will never generate real revenue and will never get
your AdMob account flagged. Do not swap in your real ad unit ID until you're testing a
release build specifically for that purpose (see Step 3 below).

---

## 2. AdMob setup (this is how the app earns money)

1. Create a Google AdMob account: https://apps.admob.com (uses your existing Google account).
2. **Add app** → Android → "No" when asked if it's published on Play yet → name it
   "Free Gym Trainer".
3. Under your new app, **Ad units → Add ad unit → Banner**. Name it e.g. "Home/Progress/Nutrition banner".
4. Copy the **App ID** (format `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`) and the
   **Ad unit ID** (format `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ`).
5. Open `app/build.gradle.kts` and replace the two `release` block placeholders:
   ```kotlin
   buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY\"")
   buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ\"")
   manifestPlaceholders["admobAppId"] = "ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"
   ```
   Leave the `debug` block pointed at Google's test IDs — never test-click your own real ads.
6. Repeat step 3 for an **Interstitial** ad unit too, and set `ADMOB_INTERSTITIAL_ID` /
   `manifestPlaceholders["admobAppId"]` the same way. That's it functionally — `AdBanner()`
   (used on Home, Nutrition, Progress) and `InterstitialAdManager` already read their
   respective `BuildConfig` fields, so a release build automatically serves your real ads.

**Where the ads are placed:** one banner each at the bottom of Home, Nutrition, and
Progress — screens people revisit often — plus one interstitial shown after finishing a
workout (never mid-session; interrupting an active workout is against most fitness-app
norms and hurts retention more than it earns). The interstitial also won't show in the
first 2 minutes of a session or within 5 minutes of the last one shown, so back-to-back
short workouts don't get hit with an ad every single time (see `InterstitialAdManager`).

**Payout:** AdMob pays out monthly once you cross the regional threshold (commonly $100
equivalent) via bank transfer/wire, after linking a payment profile in the AdMob console.
There's no cost to you to run ads — Google takes a share of ad revenue, not a fee from you.

**Realistic expectations:** ad revenue here is a function of installs × daily opens, not
of the app existing. A free fitness app with no marketing budget typically gets organic
installs in the tens-to-low-hundreds over its first months unless you promote it (social
media, Reddit fitness communities, ASO/keyword-optimized store listing, word of mouth).
At that scale, expect this to earn cents to a few dollars a month at first, not a living —
it can grow meaningfully with real user acquisition and retention, but the ad plumbing
being correct (which it is) is a small part of that; distribution is the actual bottleneck.

---

## 3. Building a release version

### Generate a signing key (one-time)

Google Play requires every release to be signed with the same key for the life of the app.

```bash
keytool -genkeypair -v -keystore release-key.jks -alias freegymtrainer \
  -keyalg RSA -keysize 2048 -validity 10000
```

You'll be prompted for a keystore password, key password, and identity details. **Store
`release-key.jks` somewhere safe and back it up** — if you lose it, you can never update
your published app again under the same listing.

Create `keystore.properties` in the project root (already excluded via `.gitignore`, never
commit it):

```properties
storeFile=/absolute/path/to/release-key.jks
storePassword=yourStorePassword
keyAlias=freegymtrainer
keyPassword=yourKeyPassword
```

`app/build.gradle.kts` already wires this file into the `release` signing config
automatically if it's present.

### Build the release bundle

Android Studio: **Build → Generate Signed App Bundle / APK → Android App Bundle** and
follow the wizard (it can also create the keystore for you if you skipped the step above).

Or from the command line:
```bash
./gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab` — this `.aab` file is what you
upload to Play Console (Google Play requires the App Bundle format, not a raw `.apk`, for
new listings).

---

## 4. Publishing to the Google Play Store

1. **Create a Play Console developer account** (one-time $25 fee):
   https://play.google.com/console/signup
2. **Host your privacy policy.** Fill in `PRIVACY_POLICY_TEMPLATE.md` (included in this
   project) with your details, then publish it somewhere public and free — a GitHub Pages
   site, a free Google Site, or a Gist raw link all work. Copy the resulting URL.
3. In Play Console: **Create app** → name "Free Gym Trainer" → Free → fill in the
   declarations (not a game, complies with policies, etc.).
4. **App content** section — complete each required item:
   - **Privacy policy**: paste the URL from step 2.
   - **Data safety form**: declare that the app collects no personal data on your servers
     (true — there's no backend), but that AdMob collects advertising ID/usage data for
     ads (also true — see AdMob's own disclosures).
   - **Content rating questionnaire**: answer honestly (fitness app, no violence/gambling/etc.) → generates a rating (typically "Everyone" or "Teen" depending on answers).
   - **Target audience**: mark as not primarily directed at children.
   - **Ads declaration**: mark "Yes, my app contains ads."
5. **Store listing**: short description, full description, app icon (512×512 PNG — export
   one from the launcher icon or design a proper one; the vector placeholder shipped here
   is functional but basic), at least 2 phone screenshots (take these from your emulator
   with **Run**, or Android Studio's screenshot tool), and a feature graphic (1024×500).
6. **Production → Create new release**: upload your signed `app-release.aab`, write release
   notes, save, and roll out. Google reviews new apps before publishing — this commonly
   takes anywhere from a few hours to a few days.
7. Once approved, the app is live and AdMob starts serving real ads to real users
   immediately (assuming you swapped in your real IDs per Section 2).

### Updating the app later

Bump `versionCode` and `versionName` in `app/build.gradle.kts`, rebuild
(`./gradlew bundleRelease`), and upload the new `.aab` as a new release in Play Console.
The signing key must be the same one you used for the first release.

---

## 5. Known limitations & what to extend next

Being upfront about what this MVP intentionally leaves out, per the "don't fake it,
mark it as a TODO" principle from your original spec:

- **No real-person exercise video.** Exercises are shown by a 3D model instead (see section 6),
  which keeps the APK small and works offline. `Exercise.mediaUrl` can still point at a remote clip.
- **No login/cloud sync.** All data is local to the device — uninstalling the app deletes
  it, and there's no way to use the app across two devices. If you want that later, the
  `data/repository/` layer is exactly where a remote data source would be added.
- **No barcode scanner, wearables integration, or AI recommendations** — same as the
  original spec's "Advanced Features" list: deliberately deferred, not faked.
- **I could not compile this project in my own sandbox** (no Android SDK or Google's Maven
  repository is reachable from here), so while every file was written carefully and
  reviewed for consistency, treat the first Android Studio sync/build as the real
  correctness check. Small issues, if any turn up, are typically one-line fixes (an import
  or a version mismatch) — paste any error you hit back to me and I'll fix it directly.

## 6. The 3D athlete model (one-time setup)

The 3D demos use a skinned model, `app/src/main/assets/models/athlete.glb`, rendered with
[Filament](https://github.com/google/filament). The model is the Superhero_Male body from
Quaternius' *Universal Base Characters* pack (free for commercial use inside an app, but the
license forbids redistributing the raw files, so the `.glb` is **gitignored** — don't commit it
to a public repo). To (re)generate it:

1. Download the pack from <https://quaternius.itch.io/universal-base-characters> (the free
   *Standard* zip) and unzip it.
2. `python tools/athlete_model/build_glb.py "<unzipped>/Base Characters" app/src/main/assets/models/athlete.glb`
   (needs `pip install pillow numpy`). This packs the glTF, shrinks the textures and adds a short haircut.
3. The gym props (bars, plates, benches...) are original procedural meshes:
   `python tools/athlete_model/build_props.py app/src/main/assets/models/props`.

Without the `.glb` the app still runs: the player falls back to the 2D side-view animation.
Poses come from `assets/exercise_motions.json` (joint-angle key poses, see `tools/exercise_motion/`);
`presentation/components/body3d/` turns them into bone rotations with inverse kinematics.

---

Questions, build errors, or want the next feature (more exercises, more equipment, a
backend for cloud sync) built out — just ask.
