plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ikun.waktusholat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ikun.waktusholat"
        minSdk = 24        // Android 7.0 — cakupan >98% device aktif, cukup modern utk WorkManager/notif channel
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    // Core & Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // Perhitungan waktu shalat — versi Java, tanpa perlu minSdk 26 / desugaring
    implementation("com.batoulapps.adhan:adhan:1.2.1")

    // Scheduling & background work (dipakai utk fallback reschedule harian, bukan pemicu alarm utama)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Lokasi (GPS)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Penyimpanan preferensi (pengganti SharedPreferences lama)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines — dipakai DataStore, LocationProvider (Task.await()), dan CoroutineWorker
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
