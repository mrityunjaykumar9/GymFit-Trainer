import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.freegymtrainer.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.freegymtrainer.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // Lets the migration test read the exported Room schemas (app/schemas) from the test APK.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    signingConfigs {
        create("release") {
            // Values are supplied via keystore.properties (not committed to git).
            // See README.md -> "Signing & Play Store Release" for setup.
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val props = Properties()
                props.load(FileInputStream(keystorePropertiesFile))
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // Google's official TEST ad unit IDs - safe to use while developing.
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Real AdMob App ID / Ad Unit IDs (apps.admob.com - "Free Gym Trainer" app).
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-1772325777930064~2981801767\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-1772325777930064/3504545949\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-1772325777930064/9353348002\"")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-1772325777930064~2981801767"
            if (rootProject.file("keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            // FlowRow/FlowColumn - used so chip rows (goal tags, muscle groups, nutrients) wrap
            // onto a new line on narrow screens instead of a plain Row squeezing them into
            // unreadable, letter-per-line text.
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    lint {
        // False positive for this app: the check exists for FragmentActivity subclasses that
        // combine Fragments with the ActivityResult APIs (a real bug in old Fragment versions
        // that skipped calling super.onRequestPermissionsResult()). This app is pure Compose
        // with a plain ComponentActivity and uses zero Fragments, so the underlying issue can't
        // apply - without this it hard-fails lintVitalRelease (and therefore assembleRelease/
        // bundleRelease) on MainActivity's registerForActivityResult call.
        disable += "InvalidFragmentVersionForActivityResult"
    }
}

ksp {
    // AppDatabase has exportSchema = true; without this KSP just warns on every build that it
    // has nowhere to write the schema. Exported JSONs are useful history for Room migrations.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room (local database - no backend needed)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (settings / onboarding flag / theme preference)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // AdMob (ads-based free monetization)
    implementation("com.google.android.gms:play-services-ads:23.2.0")
    // play-services-ads/play-services-basement transitively pull in androidx.fragment:1.1.0
    // (2019) - this app has no Fragments of its own, but Gradle's default "highest version
    // wins" resolution means declaring a current version directly here is enough to override
    // the stale transitive one and clear Play Console's "outdated SDK" technical-quality flag.
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    // User Messaging Platform - EEA/UK ad-consent + DMA message, required by AdMob's EU User
    // Consent Policy before any ad request goes out to a user in that region. See ConsentManager.
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    // WorkManager (background reminder scheduling)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Filament: physically based 3D renderer for the skinned athlete model (presentation/components/body3d).
    implementation("com.google.android.filament:filament-android:1.69.5")
    implementation("com.google.android.filament:gltfio-android:1.69.5")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
