// Top-level build file
plugins {
    id("com.android.application") version "8.5.2" apply false
    // org.jetbrains.kotlin.plugin.compose (Compose compiler Gradle plugin) hanya ada sejak Kotlin
    // 2.0 — sebelumnya compose compiler bukan plugin Kotlin resmi. Starter sebelumnya memakai
    // 1.9.24 untuk ketiga plugin ini, yang membuat plugin.compose gagal di-resolve.
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
