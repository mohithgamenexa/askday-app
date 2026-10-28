plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Firebase switches on once you add google-services.json (from the Firebase console) to this folder.
// Without it the app still builds; phone login then shows a "set up Firebase" message.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

// Your AdMob app ID. Until you set one in gradle.properties, Google's official test ID is used.
val admobAppId = (project.findProperty("askday.admobAppId") as String?) ?: "ca-app-pub-3940256099942544~3347511713"

android {
    namespace = "app.askday"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.askday.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["admobAppId"] = admobAppId
    }

    signingConfigs {
        // A fixed key, so the SHA fingerprints you register in Firebase stay the same on every build.
        // Fine for testing and sharing. For the Play Store, create your own key and keep it private.
        create("askday") {
            storeFile = file("askday-release.jks")
            storePassword = "askday-test-key"
            keyAlias = "askday"
            keyPassword = "askday-test-key"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("askday")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("askday")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.3")
    // AdMob brings an old Fragment library; the permission request needs 1.3.0 or newer.
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Firebase: phone login, analytics, crash reports, remote switches
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-config")

    // Google Mobile Ads (AdMob) and Google's consent form for regions that require it
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
}
